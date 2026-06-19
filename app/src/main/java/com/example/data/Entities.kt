package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_identity")
data class MyIdentity(
    @PrimaryKey val id: Int = 1,
    val nickname: String,
    val publicKey: String,
    val privateKey: String
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val publicKey: String, // Base64 of public key
    val nickname: String,
    val localIp: String? = null,
    val localPort: Int? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val senderPublicKey: String,
    val recipientPublicKey: String,
    val encryptedAesKey: String,
    val encryptedContent: String,
    val iv: String,
    val timestamp: Long,
    val isSentByMe: Boolean,
    val isRead: Boolean = false
) {
    // Decrypted cache (transient/non-persistent field can be held in state, or decrypted on demand)
    @androidx.room.Ignore
    var decryptedBody: String? = null
}

@Entity(tableName = "groups")
data class GroupChat(
    @PrimaryKey val groupId: String,
    val name: String,
    val creatorPublicKey: String,
    val timestamp: Long
)

@Entity(tableName = "group_messages")
data class GroupMessage(
    @PrimaryKey val id: String,
    val groupId: String,
    val senderPublicKey: String,
    val senderNickname: String,
    val content: String,
    val timestamp: Long
)

@Entity(tableName = "forums")
data class Forum(
    @PrimaryKey val forumId: String,
    val name: String,
    val creatorPublicKey: String,
    val timestamp: Long
)

@Entity(tableName = "forum_posts")
data class ForumPost(
    @PrimaryKey val id: String,
    val forumId: String,
    val senderPublicKey: String,
    val senderNickname: String,
    val content: String,
    val signature: String,
    val timestamp: Long
)

@Entity(tableName = "blog_posts")
data class BlogPost(
    @PrimaryKey val id: String,
    val authorPublicKey: String,
    val authorNickname: String,
    val title: String,
    val content: String,
    val signature: String,
    val timestamp: Long
)
