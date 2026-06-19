package com.example.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.data.BlogPost
import com.example.data.BriarDao
import com.example.data.Forum
import com.example.data.ForumPost
import com.example.data.GroupChat
import com.example.data.GroupMessage
import com.example.data.Contact
import com.example.data.Message
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class P2PEngine(
    private val context: Context,
    private val briarDao: BriarDao
) {
    private val SERVICE_TYPE = "_briarp2p._tcp."
    private val TAG = "P2PEngine"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val syncAdapter = moshi.adapter(SyncPacket::class.java)

    private var serverSocket: ServerSocket? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // State flows for UI tracking
    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _activeConnections = MutableStateFlow<List<String>>(emptyList())
    val activeConnections: StateFlow<List<String>> = _activeConnections.asStateFlow()

    private val activeSyncingJobs = ConcurrentHashMap<String, Job>()

    fun start() {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        startServerSocket()
    }

    private fun startServerSocket() {
        scope.launch {
            try {
                // Open dynamic server socket
                val socket = ServerSocket(0)
                serverSocket = socket
                val localPort = socket.localPort
                Log.d(TAG, "Server socket started on port $localPort")

                // Register NSD
                registerService(localPort)

                // Accept client connections loop
                while (isActive) {
                    val client = socket.accept()
                    Log.d(TAG, "Accepted inbound socket connection from ${client.inetAddress}")
                    scope.launch {
                        handleInboundPeer(client)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server Socket Exception: ${e.localizedMessage}")
            }
        }
    }

    private fun registerService(port: Int) {
        scope.launch {
            delay(1000) // Ensure identity is ready
            val identity = briarDao.getMyIdentityOnce() ?: return@launch
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "Briar_${identity.nickname}_${identity.publicKey.take(8)}"
                serviceType = SERVICE_TYPE
                setPort(port)
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service Registered: ${info.serviceName}")
                }
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "NSD Registration Failed: $errorCode")
                }
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            }

            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            startDiscovery()
        }
    }

    private fun startDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery Start Failed: $errorCode")
                nsdManager?.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery Stop Failed: $errorCode")
                nsdManager?.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "NSD Discovery Started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD Discovery Stopped")
            }

            override fun onServiceFound(info: NsdServiceInfo) {
                Log.d(TAG, "Service Found: ${info.serviceName}")
                if (info.serviceType.contains(SERVICE_TYPE)) {
                    // Check if self
                    scope.launch {
                        val identity = briarDao.getMyIdentityOnce()
                        val selfPrefix = "Briar_${identity?.nickname}_"
                        if (info.serviceName.startsWith(selfPrefix)) {
                            Log.d(TAG, "Discovered self instance. Skipping.")
                            return@launch
                        }

                        nsdManager?.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.e(TAG, "Resolve Failed: $errorCode")
                            }

                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                Log.d(TAG, "Service Resolved: ${serviceInfo.host}:${serviceInfo.port}")
                                connectToPeer(serviceInfo)
                            }
                        })
                    }
                }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                Log.d(TAG, "Service Lost: ${info.serviceName}")
                _discoveredPeers.value = _discoveredPeers.value.filter { it.serviceName != info.serviceName }
            }
        }

        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun connectToPeer(serviceInfo: NsdServiceInfo) {
        val hostIP = serviceInfo.host.hostAddress ?: return
        val port = serviceInfo.port
        val serviceName = serviceInfo.serviceName

        val peerKey = "$hostIP:$port"
        if (activeSyncingJobs.containsKey(peerKey)) {
            Log.d(TAG, "Already syncing with $peerKey")
            return
        }

        val job = scope.launch {
            try {
                Log.d(TAG, "Connecting outbound socket to $hostIP:$port")
                val socket = Socket(hostIP, port)
                handleConnectionStream(socket, isOutbound = true)
            } catch (e: Exception) {
                Log.e(TAG, "Connection to $peerKey failed: ${e.localizedMessage}")
            } finally {
                activeSyncingJobs.remove(peerKey)
            }
        }
        activeSyncingJobs[peerKey] = job
    }

    private suspend fun handleInboundPeer(socket: Socket) {
        val peerKey = "${socket.inetAddress.hostAddress}:${socket.port}"
        try {
            handleConnectionStream(socket, isOutbound = false)
        } catch (e: Exception) {
            Log.e(TAG, "Inbound connection with $peerKey error: ${e.localizedMessage}")
        }
    }

    private suspend fun handleConnectionStream(socket: Socket, isOutbound: Boolean) = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer = PrintWriter(socket.getOutputStream(), true)

        val myId = briarDao.getMyIdentityOnce() ?: return@withContext socket.close()

        // 1. Prepare and send Local Sync Packet
        val contactsList = briarDao.getAllContactsOnce()
        val allLocalMessages = briarDao.getAllMessagesOnce()
        val allLocalGroups = briarDao.getAllGroupsOnce()
        val allLocalGroupMsgs = briarDao.getAllGroupMessagesOnce()
        val allLocalForums = briarDao.getAllForumsOnce()
        val allLocalForumPosts = briarDao.getAllForumPostsOnce()
        val allLocalBlogs = briarDao.getAllBlogsOnce()

        // Format private messages *destined for them* or *sent by them* (RSA cryptographically secure)
        val p2pMessages = allLocalMessages.map { 
            P2PMessage(
                senderPublicKey = it.senderPublicKey,
                recipientPublicKey = it.recipientPublicKey,
                encryptedAesKey = it.encryptedAesKey,
                encryptedContent = it.encryptedContent,
                iv = it.iv,
                timestamp = it.timestamp
            )
        }

        val syncPacketOut = SyncPacket(
            nickname = myId.nickname,
            publicKey = myId.publicKey,
            privateMessages = p2pMessages,
            groups = allLocalGroups,
            groupMessages = allLocalGroupMsgs,
            forums = allLocalForums,
            forumPosts = allLocalForumPosts,
            blogs = allLocalBlogs
        )

        val syncJsonStr = syncAdapter.toJson(syncPacketOut).replace("\n", "")
        writer.println(syncJsonStr)
        Log.d(TAG, "Sent sync packet to peer (${syncJsonStr.length} chars)")

        // 2. Read Remote Sync Packet
        val responseLine = reader.readLine()
        if (responseLine != null) {
            Log.d(TAG, "Received sync packet from peer: (${responseLine.length} chars)")
            val syncPacketIn = syncAdapter.fromJson(responseLine)
            if (syncPacketIn != null) {
                // Update UI Connection State
                val remoteNickname = syncPacketIn.nickname
                val remotePublicKey = syncPacketIn.publicKey
                val remoteIP = socket.inetAddress.hostAddress ?: ""
                val remotePort = socket.port

                val updatedConnections = _activeConnections.value.toMutableList()
                val peerDisplay = "$remoteNickname (${remoteIP})"
                if (!updatedConnections.contains(peerDisplay)) {
                    updatedConnections.add(peerDisplay)
                    _activeConnections.value = updatedConnections
                }

                // Check if they are in contacts
                val contact = briarDao.getContact(remotePublicKey)
                if (contact == null) {
                    // Update list of discovered peers that user can click to ADD
                    val currentDiscovered = _discoveredPeers.value.toMutableList()
                    if (currentDiscovered.none { it.publicKey == remotePublicKey }) {
                        currentDiscovered.add(
                            DiscoveredPeer(
                                nickname = remoteNickname,
                                publicKey = remotePublicKey,
                                ip = remoteIP,
                                port = remotePort,
                                serviceName = "Discovered_${remoteNickname}"
                            )
                        )
                        _discoveredPeers.value = currentDiscovered
                    }
                } else {
                    // Update contact connection status
                    briarDao.updateContactDiscovery(
                        pubKey = remotePublicKey,
                        ip = remoteIP,
                        port = remotePort,
                        isOnline = true,
                        lastSeen = System.currentTimeMillis()
                    )
                }

                // Integrate received decentralized sync items
                integrateSyncPacket(syncPacketIn)

                // Flash active connection item on sync completion
                delay(3000)
                _activeConnections.value = _activeConnections.value.filter { it != peerDisplay }
                if (contact != null) {
                    // Update connection state back
                    briarDao.updateContactConnection(
                        pubKey = remotePublicKey,
                        isOnline = false,
                        lastSeen = System.currentTimeMillis()
                    )
                }
            }
        }

        socket.close()
    }

    private suspend fun integrateSyncPacket(packet: SyncPacket) = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: return@withContext
        Log.d(TAG, "Integrating data sync elements into local database...")

        // A. Private Messages (only store if recipient is ME or sender is ME, and we don't have it)
        val allLocalMsg = briarDao.getAllMessagesOnce()
        packet.privateMessages.forEach { p2pMsg ->
            if ((p2pMsg.recipientPublicKey == myId.publicKey && p2pMsg.senderPublicKey == packet.publicKey) ||
                (p2pMsg.senderPublicKey == myId.publicKey && p2pMsg.recipientPublicKey == packet.publicKey)
            ) {
                // Check if already exist
                val isDuplicate = allLocalMsg.any {
                    it.senderPublicKey == p2pMsg.senderPublicKey &&
                            it.recipientPublicKey == p2pMsg.recipientPublicKey &&
                            it.timestamp == p2pMsg.timestamp &&
                            it.iv == p2pMsg.iv
                }
                if (!isDuplicate) {
                    val dbMsg = Message(
                        senderPublicKey = p2pMsg.senderPublicKey,
                        recipientPublicKey = p2pMsg.recipientPublicKey,
                        encryptedAesKey = p2pMsg.encryptedAesKey,
                        encryptedContent = p2pMsg.encryptedContent,
                        iv = p2pMsg.iv,
                        timestamp = p2pMsg.timestamp,
                        isSentByMe = p2pMsg.senderPublicKey == myId.publicKey,
                        isRead = p2pMsg.senderPublicKey == myId.publicKey
                    )
                    briarDao.insertMessage(dbMsg)
                    Log.d(TAG, "Synced new private message from ${packet.nickname}")
                }
            }
        }

        // B. Groups (Decentralized group metadata)
        val allLocalGroups = briarDao.getAllGroupsOnce()
        packet.groups.forEach { peerGroup ->
            if (allLocalGroups.none { it.groupId == peerGroup.groupId }) {
                briarDao.insertGroup(peerGroup)
                Log.d(TAG, "Synced new group: ${peerGroup.name}")
            }
        }

        // C. Group Messages
        val allLocalGroupMsgs = briarDao.getAllGroupMessagesOnce()
        packet.groupMessages.forEach { peerGrpMsg ->
            if (allLocalGroupMsgs.none { it.id == peerGrpMsg.id }) {
                briarDao.insertGroupMessage(peerGrpMsg)
                Log.d(TAG, "Synced new message in group ID ${peerGrpMsg.groupId}")
            }
        }

        // D. Forums
        val allLocalForums = briarDao.getAllForumsOnce()
        packet.forums.forEach { peerForum ->
            if (allLocalForums.none { it.forumId == peerForum.forumId }) {
                briarDao.insertForum(peerForum)
                Log.d(TAG, "Synced new Forum: ${peerForum.name}")
            }
        }

        // E. Forum Posts (Verify Decrypted Signature before saving!)
        val allLocalForumPosts = briarDao.getAllForumPostsOnce()
        packet.forumPosts.forEach { peerPost ->
            if (allLocalForumPosts.none { it.id == peerPost.id }) {
                // Verify signed authenticity
                try {
                    val senderPub = com.example.crypto.CryptoHelper.publicKeyFromBase64(peerPost.senderPublicKey)
                    val isValid = com.example.crypto.CryptoHelper.verify(
                        data = peerPost.content + peerPost.forumId + peerPost.senderPublicKey,
                        signatureBase64 = peerPost.signature,
                        publicKey = senderPub
                    )
                    if (isValid) {
                        briarDao.insertForumPost(peerPost)
                        Log.d(TAG, "Signed Forum Post accepted and synced!")
                    } else {
                        Log.w(TAG, "Forum Post signatures validation failed. Discarded.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verifying forum signature: ${e.localizedMessage}")
                }
            }
        }

        // F. Blogs
        val allLocalBlogs = briarDao.getAllBlogsOnce()
        packet.blogs.forEach { peerBlog ->
            if (allLocalBlogs.none { it.id == peerBlog.id }) {
                try {
                    val authorPub = com.example.crypto.CryptoHelper.publicKeyFromBase64(peerBlog.authorPublicKey)
                    val isValid = com.example.crypto.CryptoHelper.verify(
                        data = peerBlog.title + peerBlog.content + peerBlog.authorPublicKey,
                        signatureBase64 = peerBlog.signature,
                        publicKey = authorPub
                    )
                    if (isValid) {
                        briarDao.insertBlog(peerBlog)
                        Log.d(TAG, "Signed decentralised blog accepted and synced!")
                    } else {
                        Log.w(TAG, "Blog post signatures verification failed. Discarded.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verifying blog signature: ${e.localizedMessage}")
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {}

        registrationListener?.let {
            try {
                nsdManager?.unregisterService(it)
            } catch (e: Exception) {}
        }

        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {}
        }

        scope.launch {
            briarDao.markAllContactsOffline()
        }
    }
}

data class DiscoveredPeer(
    val nickname: String,
    val publicKey: String,
    val ip: String,
    val port: Int,
    val serviceName: String
)

data class SyncPacket(
    val nickname: String,
    val publicKey: String,
    val privateMessages: List<P2PMessage> = emptyList(),
    val groups: List<GroupChat> = emptyList(),
    val groupMessages: List<GroupMessage> = emptyList(),
    val forums: List<Forum> = emptyList(),
    val forumPosts: List<ForumPost> = emptyList(),
    val blogs: List<BlogPost> = emptyList()
)

data class P2PMessage(
    val senderPublicKey: String,
    val recipientPublicKey: String,
    val encryptedAesKey: String,
    val encryptedContent: String,
    val iv: String,
    val timestamp: Long
)
