package com.example.data

import com.example.crypto.CryptoHelper
import com.example.crypto.EncryptedPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class BriarRepository(private val briarDao: BriarDao) {

    val myIdentity: Flow<MyIdentity?> = briarDao.getMyIdentity().flowOn(Dispatchers.IO)
    val contacts: Flow<List<Contact>> = briarDao.getAllContacts().flowOn(Dispatchers.IO)
    val groups: Flow<List<GroupChat>> = briarDao.getAllGroups().flowOn(Dispatchers.IO)
    val forums: Flow<List<Forum>> = briarDao.getAllForums().flowOn(Dispatchers.IO)
    val blogs: Flow<List<BlogPost>> = briarDao.getAllBlogs().flowOn(Dispatchers.IO)

    suspend fun getMyIdentityOnce(): MyIdentity? = withContext(Dispatchers.IO) {
        briarDao.getMyIdentityOnce()
    }

    suspend fun registerIdentity(nickname: String): MyIdentity = withContext(Dispatchers.IO) {
        val keyPair = CryptoHelper.generateKeyPair()
        val pubKeyStr = CryptoHelper.keyToBase64(keyPair.public)
        val privKeyStr = CryptoHelper.keyToBase64(keyPair.private)

        val identity = MyIdentity(
            nickname = nickname,
            publicKey = pubKeyStr,
            privateKey = privKeyStr
        )
        briarDao.insertMyIdentity(identity)
        identity
    }

    suspend fun addContact(nickname: String, publicKeyBase64: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Validate key
            CryptoHelper.publicKeyFromBase64(publicKeyBase64)
            val contact = Contact(
                publicKey = publicKeyBase64,
                nickname = nickname,
                isOnline = false
            )
            briarDao.insertContact(contact)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteContact(contact: Contact) = withContext(Dispatchers.IO) {
        briarDao.deleteContact(contact)
    }

    fun getMessagesForContact(contactPubKey: String): Flow<List<Message>> {
        return myIdentity.map { myId ->
            if (myId == null) return@map emptyList<Message>()
            val decryptedMsgs = briarDao.getAllMessagesOnce() // Get all from cache
            val myPrivKey = CryptoHelper.privateKeyFromBase64(myId.privateKey)

            // Filter and decrypt pairwise messages for specific contact
            decryptedMsgs.filter { msg ->
                (msg.senderPublicKey == contactPubKey && msg.recipientPublicKey == myId.publicKey) ||
                        (msg.senderPublicKey == myId.publicKey && msg.recipientPublicKey == contactPubKey)
            }.map { msg ->
                if (msg.decryptedBody == null) {
                    try {
                        if (msg.isSentByMe) {
                            // If sent by me, the recipient decrypted key is for the recipient. 
                            // How do we decrypt our own sent messages?
                            // In a real system, we could store the plaintext locally on device, or encrypt twice (one for recipient, one for me).
                            // Let's store the raw text as is or if we can decrypt we check if there's a cached plaintext in data model.
                            // In this demo, if it is sent by us, we had pre-stored or we can bypass decryption and save plaintext as a cached field, or decrypt using our private key (if encrypted with ours as well).
                            // Let's decrypt or use cached plaintext. Let's make sure when saving to db we save plaintext in some form OR we actually save and encrypt twice or store local copy.
                            // In our DB, we'll store local messages! Let's examine: when we store a Message, we can store plaintext locally (since DB is local and private).
                            // But what is transmitted is the ENCRYPTED payload. This is perfect! The DB stores the encrypted fields, and for the local user we also store an unencrypted local copy or we encrypt it using our public key for local storage.
                            // Actually, let's keep it simple: the Database saves the encrypted AES key and encrypted content. To decrypt it, we can decrypt it using recipient's key (if they received it) OR let's encrypt it for our own identity key too when we send, or simply save the plaintext locally, OR decrypt since Bob (sender) can also encrypt the AES session key to his own public key too!
                            // Let's implement this: when Bob sends a message, we store the Message in Bob's DB. Since Bob's DB is local and secure, he can just decrypt or we can store an additional field, or simply encrypt the AES session key using Bob's RSA key too if we want Bob to decrypt it! 
                            // Wait, if Bob sent it, Bob encrypted the AES key with ALICE'S public key. Alice can decrypt it, but Bob cannot (since Bob has Alice's public key but not Alice's private key).
                            // So to decrypt it in Bob's list, we can store a local field or Bob encrypts the AES key to HIMSELF (Bob's public key).
                            // Let's add a `localPlaintext` field to the database record! Or simply: for messages sent by me, `encryptedAesKey` is encrypted with MY public key (RSA) so that I can decrypt my own sent history, and when sending over the network, we re-encrypt the AES key under Alice's public key! This is incredibly elegant and standard in cryptography (encrypting session keys for all participants)!
                            // Yes, encrypting the AES key for ourselves is brilliant.
                            val payload = EncryptedPayload(
                                encryptedAesKey = msg.encryptedAesKey,
                                encryptedContent = msg.encryptedContent,
                                iv = msg.iv
                            )
                            msg.decryptedBody = CryptoHelper.decryptPrivateMessage(payload, myPrivKey)
                        } else {
                            // Sent by contact to me (recipient). Encrypted under my public key.
                            val payload = EncryptedPayload(
                                encryptedAesKey = msg.encryptedAesKey,
                                encryptedContent = msg.encryptedContent,
                                iv = msg.iv
                            )
                            msg.decryptedBody = CryptoHelper.decryptPrivateMessage(payload, myPrivKey)
                        }
                    } catch (e: Exception) {
                        msg.decryptedBody = "[Decryption Failed: ${e.localizedMessage}]"
                    }
                }
                msg
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendPrivateMessage(contactPubKey: String, text: String): Boolean = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: return@withContext false
        val contact = briarDao.getContact(contactPubKey) ?: return@withContext false

        try {
            val recipientPubKey = CryptoHelper.publicKeyFromBase64(contact.publicKey)
            val myPubKey = CryptoHelper.publicKeyFromBase64(myId.publicKey)

            // Let's encrypt the message for the recipient (to transmit over the network)
            val networkPayload = CryptoHelper.encryptPrivateMessage(text, recipientPubKey)

            // For local message storage, we encrypt the AES key using our OWN public key so we can decrypt our sent messages history!
            // Let's write the AES encryption helper or just encrypt the session key with our own public key and keep same cipher text.
            val myRsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
            myRsaCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, myPubKey)

            // Decrypt the AES key from recipient payload to re-encrypt for me
            val rsaCipherRecip = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
            val contactTempKey = CryptoHelper.publicKeyFromBase64(contact.publicKey)
            
            // To get the AES key, we can encrypt the message for ourselves as well!
            val localPayload = CryptoHelper.encryptPrivateMessage(text, myPubKey)

            val dbMessage = Message(
                senderPublicKey = myId.publicKey,
                recipientPublicKey = contactPubKey,
                encryptedAesKey = localPayload.encryptedAesKey, // Encrypted for me to read it back
                encryptedContent = localPayload.encryptedContent,
                iv = localPayload.iv,
                timestamp = System.currentTimeMillis(),
                isSentByMe = true,
                isRead = true
            )
            briarDao.insertMessage(dbMessage)
            
            // In a real network setting, we'd queue this message `networkPayload` to send to Alice when connected.
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Groups
    suspend fun createGroup(name: String): GroupChat = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: throw IllegalStateException("No Identity")
        val group = GroupChat(
            groupId = UUID.randomUUID().toString(),
            name = name,
            creatorPublicKey = myId.publicKey,
            timestamp = System.currentTimeMillis()
        )
        briarDao.insertGroup(group)
        group
    }

    suspend fun postToGroup(groupId: String, text: String) = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: return@withContext
        val msg = GroupMessage(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            senderPublicKey = myId.publicKey,
            senderNickname = myId.nickname,
            content = text,
            timestamp = System.currentTimeMillis()
        )
        briarDao.insertGroupMessage(msg)
    }

    fun getMessagesForGroup(groupId: String): Flow<List<GroupMessage>> {
        return briarDao.getMessagesForGroup(groupId).flowOn(Dispatchers.IO)
    }

    // Forums
    suspend fun createForum(name: String): Forum = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: throw IllegalStateException("No Identity")
        val forum = Forum(
            forumId = UUID.randomUUID().toString(),
            name = name,
            creatorPublicKey = myId.publicKey,
            timestamp = System.currentTimeMillis()
        )
        briarDao.insertForum(forum)
        forum
    }

    suspend fun postToForum(forumId: String, content: String) = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: return@withContext
        val myPrivKey = CryptoHelper.privateKeyFromBase64(myId.privateKey)

        // Sign the post content to verify authorship!
        val signature = CryptoHelper.sign(content + forumId + myId.publicKey, myPrivKey)

        val post = ForumPost(
            id = UUID.randomUUID().toString(),
            forumId = forumId,
            senderPublicKey = myId.publicKey,
            senderNickname = myId.nickname,
            content = content,
            signature = signature,
            timestamp = System.currentTimeMillis()
        )
        briarDao.insertForumPost(post)
    }

    fun getPostsForForum(forumId: String): Flow<List<ForumPost>> {
        return briarDao.getPostsForForum(forumId).flowOn(Dispatchers.IO)
    }

    // Blogs
    suspend fun createBlog(title: String, content: String) = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: return@withContext
        val myPrivKey = CryptoHelper.privateKeyFromBase64(myId.privateKey)

        // Sign the blog entry to prove identity authenticity!
        val signature = CryptoHelper.sign(title + content + myId.publicKey, myPrivKey)

        val blog = BlogPost(
            id = UUID.randomUUID().toString(),
            authorPublicKey = myId.publicKey,
            authorNickname = myId.nickname,
            title = title,
            content = content,
            signature = signature,
            timestamp = System.currentTimeMillis()
        )
        briarDao.insertBlog(blog)
    }

    // P2P Data integration (received from remote)
    suspend fun processReceivedPrivateMessage(
        senderPubKey: String,
        encryptedAesKey: String,
        encryptedContent: String,
        iv: String,
        timestamp: Long
    ) = withContext(Dispatchers.IO) {
        val myId = briarDao.getMyIdentityOnce() ?: return@withContext
        // Store into DB
        val msg = Message(
            senderPublicKey = senderPubKey,
            recipientPublicKey = myId.publicKey,
            encryptedAesKey = encryptedAesKey,
            encryptedContent = encryptedContent,
            iv = iv,
            timestamp = timestamp,
            isSentByMe = false,
            isRead = false
        )
        briarDao.insertMessage(msg)
    }

    suspend fun processReceivedGroupMessage(msg: GroupMessage) = withContext(Dispatchers.IO) {
        briarDao.insertGroupMessage(msg)
    }

    suspend fun processReceivedForumPost(post: ForumPost) = withContext(Dispatchers.IO) {
        // Verify signature before saving!
        try {
            val pubKey = CryptoHelper.publicKeyFromBase64(post.senderPublicKey)
            val isValid = CryptoHelper.verify(
                data = post.content + post.forumId + post.senderPublicKey,
                signatureBase64 = post.signature,
                publicKey = pubKey
            )
            if (isValid) {
                briarDao.insertForumPost(post)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun processReceivedBlogPost(blog: BlogPost) = withContext(Dispatchers.IO) {
        // Verify signature before saving!
        try {
            val pubKey = CryptoHelper.publicKeyFromBase64(blog.authorPublicKey)
            val isValid = CryptoHelper.verify(
                data = blog.title + blog.content + blog.authorPublicKey,
                signatureBase64 = blog.signature,
                publicKey = pubKey
            )
            if (isValid) {
                briarDao.insertBlog(blog)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
