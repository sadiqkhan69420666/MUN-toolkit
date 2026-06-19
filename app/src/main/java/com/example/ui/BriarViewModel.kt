package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.p2p.DiscoveredPeer
import com.example.p2p.P2PEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BriarViewModel(
    application: Application,
    private val repository: BriarRepository
) : AndroidViewModel(application) {

    private var p2pEngine: P2PEngine? = null

    val myIdentity: StateFlow<MyIdentity?> = repository.myIdentity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contacts: StateFlow<List<Contact>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<GroupChat>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forums: StateFlow<List<Forum>> = repository.forums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blogs: StateFlow<List<BlogPost>> = repository.blogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // P2P State
    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _activeConnections = MutableStateFlow<List<String>>(emptyList())
    val activeConnections: StateFlow<List<String>> = _activeConnections.asStateFlow()

    init {
        // Collect identity to trigger P2P engine when ready
        viewModelScope.launch {
            myIdentity.collect { identity ->
                if (identity != null && p2pEngine == null) {
                    val db = BriarDatabase.getDatabase(getApplication())
                    p2pEngine = P2PEngine(getApplication(), db.briarDao()).apply {
                        start()
                    }
                    // Sync engine status back to VM flows
                    launch {
                        p2pEngine?.discoveredPeers?.collect {
                            _discoveredPeers.value = it
                        }
                    }
                    launch {
                        p2pEngine?.activeConnections?.collect {
                            _activeConnections.value = it
                        }
                    }
                }
            }
        }
    }

    fun registerNickname(nickname: String) {
        viewModelScope.launch {
            repository.registerIdentity(nickname)
        }
    }

    fun addContact(nickname: String, publicKey: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addContact(nickname, publicKey)
            onFinished(success)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun approveDiscoveredPeer(peer: DiscoveredPeer) {
        viewModelScope.launch {
            // Add as contact
            repository.addContact(peer.nickname, peer.publicKey)
            // Remove from discovered list
            _discoveredPeers.value = _discoveredPeers.value.filter { it.publicKey != peer.publicKey }
        }
    }

    fun sendPrivateMessage(contactPubKey: String, text: String) {
        viewModelScope.launch {
            repository.sendPrivateMessage(contactPubKey, text)
        }
    }

    fun getMessagesForContact(contactPubKey: String): Flow<List<Message>> {
        return repository.getMessagesForContact(contactPubKey)
    }

    // Group methods
    fun createGroup(name: String) {
        viewModelScope.launch {
            repository.createGroup(name)
        }
    }

    fun postToGroup(groupId: String, content: String) {
        viewModelScope.launch {
            repository.postToGroup(groupId, content)
        }
    }

    fun getMessagesForGroup(groupId: String): Flow<List<GroupMessage>> {
        return repository.getMessagesForGroup(groupId)
    }

    // Forum methods
    fun createForum(name: String) {
        viewModelScope.launch {
            repository.createForum(name)
        }
    }

    fun postToForum(forumId: String, content: String) {
        viewModelScope.launch {
            repository.postToForum(forumId, content)
        }
    }

    fun getPostsForForum(forumId: String): Flow<List<ForumPost>> {
        return repository.getPostsForForum(forumId)
    }

    // Blog methods
    fun publishBlogPost(title: String, content: String) {
        viewModelScope.launch {
            repository.createBlog(title, content)
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2pEngine?.stop()
    }
}

class BriarViewModelFactory(
    private val application: Application,
    private val repository: BriarRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BriarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BriarViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
