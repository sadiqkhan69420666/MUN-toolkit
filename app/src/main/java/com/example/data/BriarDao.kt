package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BriarDao {

    // Identity
    @Query("SELECT * FROM my_identity WHERE id = 1 LIMIT 1")
    fun getMyIdentity(): Flow<MyIdentity?>

    @Query("SELECT * FROM my_identity WHERE id = 1 LIMIT 1")
    suspend fun getMyIdentityOnce(): MyIdentity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyIdentity(identity: MyIdentity)

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY nickname ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts ORDER BY nickname ASC")
    suspend fun getAllContactsOnce(): List<Contact>

    @Query("SELECT * FROM contacts WHERE publicKey = :pubKey LIMIT 1")
    suspend fun getContact(pubKey: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("UPDATE contacts SET isOnline = :isOnline, lastSeen = :lastSeen WHERE publicKey = :pubKey")
    suspend fun updateContactConnection(pubKey: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE contacts SET localIp = :ip, localPort = :port, isOnline = :isOnline, lastSeen = :lastSeen WHERE publicKey = :pubKey")
    suspend fun updateContactDiscovery(pubKey: String, ip: String, port: Int, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE contacts SET isOnline = 0")
    suspend fun markAllContactsOffline()

    // Private Messages
    @Query("""
        SELECT * FROM messages 
        WHERE (senderPublicKey = :contactPubKey AND recipientPublicKey = :myPubKey) 
           OR (senderPublicKey = :myPubKey AND recipientPublicKey = :contactPubKey) 
        ORDER BY timestamp ASC
    """)
    fun getMessagesForContact(myPubKey: String, contactPubKey: String): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    // Group Chats
    @Query("SELECT * FROM groups ORDER BY timestamp DESC")
    fun getAllGroups(): Flow<List<GroupChat>>

    @Query("SELECT * FROM groups ORDER BY timestamp DESC")
    suspend fun getAllGroupsOnce(): List<GroupChat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupChat)

    @Query("DELETE FROM groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    // Group Messages
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroup(groupId: String): Flow<List<GroupMessage>>

    @Query("SELECT * FROM group_messages ORDER BY timestamp ASC")
    suspend fun getAllGroupMessagesOnce(): List<GroupMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMessage(message: GroupMessage)

    // Forums
    @Query("SELECT * FROM forums ORDER BY timestamp DESC")
    fun getAllForums(): Flow<List<Forum>>

    @Query("SELECT * FROM forums ORDER BY timestamp DESC")
    suspend fun getAllForumsOnce(): List<Forum>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForum(forum: Forum)

    @Query("DELETE FROM forums WHERE forumId = :forumId")
    suspend fun deleteForum(forumId: String)

    // Forum Posts
    @Query("SELECT * FROM forum_posts WHERE forumId = :forumId ORDER BY timestamp ASC")
    fun getPostsForForum(forumId: String): Flow<List<ForumPost>>

    @Query("SELECT * FROM forum_posts ORDER BY timestamp ASC")
    suspend fun getAllForumPostsOnce(): List<ForumPost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForumPost(post: ForumPost)

    // Blogs
    @Query("SELECT * FROM blog_posts ORDER BY timestamp DESC")
    fun getAllBlogs(): Flow<List<BlogPost>>

    @Query("SELECT * FROM blog_posts ORDER BY timestamp DESC")
    suspend fun getAllBlogsOnce(): List<BlogPost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlog(blog: BlogPost)

    @Query("DELETE FROM blog_posts WHERE id = :blogId")
    suspend fun deleteBlog(blogId: String)
}
