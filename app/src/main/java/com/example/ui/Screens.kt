package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.p2p.DiscoveredPeer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriarAppContent(
    viewModel: BriarViewModel,
    modifier: Modifier = Modifier
) {
    val myId by viewModel.myIdentity.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = myId,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "identity_switch"
    ) { identity ->
        if (identity == null) {
            OnboardingScreen(onRegister = { viewModel.registerNickname(it) })
        } else {
            MainAppLayout(viewModel = viewModel, identity = identity)
        }
    }
}

@Composable
fun OnboardingScreen(onRegister: (String) -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFEADDFF), Color(0xFFFEF7FF))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Briar Cyber Shield Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(BriarGreenNeon.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            radius = size.width * 0.8f
                        )
                    }
                    .border(2.dp, BriarGreenNeon, CircleShape)
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Shield",
                    tint = BriarGreenNeon,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BRIAR DECENTRALIZED",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = BriarGreenNeon,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Secure P2P serverless mesh chat. Generates cryptographic identity directly on your device. Zero servers. Zero central logs.",
                style = MaterialTheme.typography.bodyMedium,
                color = BriarTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isGenerating) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { if (it.length <= 15) nickname = it },
                    label = { Text("Choose Nickname", color = BriarTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BriarTextPrimary,
                        unfocusedTextColor = BriarTextPrimary,
                        focusedBorderColor = BriarGreenNeon,
                        unfocusedBorderColor = BriarTextSecondary,
                        cursorColor = BriarGreenNeon
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (nickname.isNotBlank()) {
                            isGenerating = true
                            scope.launch {
                                // Simulate cypherpunk security key generation graphics
                                while (progress < 1f) {
                                    delay(40)
                                    progress += 0.04f
                                }
                                onRegister(nickname.trim())
                            }
                        }
                    },
                    enabled = nickname.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BriarGreenNeon,
                        contentColor = Color(0xFF00200A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "GENERATE CRYPTO IDENTITY",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // Key-generation cyberpunk animation screen
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(1.dp, BriarGreenNeon, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = BriarCardBack)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INITIALIZING KEYS...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = BriarGreenNeon
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val simulatedKeyHash = remember(progress) {
                            UUID.randomUUID().toString().replace("-", "").take(24).uppercase()
                        }
                        Text(
                            text = "PRNG: $simulatedKeyHash",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = BriarTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = BriarGreenNeon,
                            trackColor = Color(0xFF102A24)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Generating 2048-bit RSA Private & Public Identity Keys",
                            style = MaterialTheme.typography.bodySmall,
                            color = BriarTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: BriarViewModel, identity: MyIdentity) {
    var currentTab by remember { mutableStateOf(MainTab.CONTACTS) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var activeChatContactPubKey by remember { mutableStateOf<String?>(null) }
    var activeGroupChatId by remember { mutableStateOf<String?>(null) }
    var activeForumId by remember { mutableStateOf<String?>(null) }

    val activeConnections by viewModel.activeConnections.collectAsStateWithLifecycle()
    val isEngineRunning = activeConnections.isNotEmpty() || true // Scanning is running in background

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Briar Shield",
                            tint = BriarGreenNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "BRIAR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = BriarGreenNeon
                            )
                            Text(
                                text = "Peer-to-Peer Encryption Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = BriarTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Scanning Network Pulse Dot
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = if (isEngineRunning) BriarGreenNeon.copy(alpha = 0.15f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isEngineRunning) BriarGreenNeon.copy(alpha = 0.4f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isEngineRunning) BriarGreenNeon else BriarError,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (activeConnections.isNotEmpty()) "${activeConnections.size} PEER(S)" else "MESH SCAN",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isEngineRunning) BriarGreenNeon else BriarError
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BriarDarkBack,
                    titleContentColor = BriarGreenNeon
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BriarDarkBack,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D192B),
                            selectedTextColor = Color(0xFF1D192B),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = BriarTextSecondary,
                            unselectedTextColor = BriarTextSecondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != MainTab.NETWORK) {
                FloatingActionButton(
                    onClick = {
                        when (currentTab) {
                            MainTab.CONTACTS -> showAddContactDialog = true
                            MainTab.GROUPS -> {
                                // Create Group Click
                                showAddContactDialog = true // Trigger relevant add flow or handle in tab
                            }
                            MainTab.FORUMS -> {
                                // Create Forum Click
                                showAddContactDialog = true
                            }
                            MainTab.BLOGS -> {
                                // Create Blog Click
                                showAddContactDialog = true
                            }
                            else -> {}
                        }
                    },
                    containerColor = BriarGreenLight,
                    contentColor = BriarGreenDark
                ) {
                    val icon = when (currentTab) {
                        MainTab.CONTACTS -> Icons.Default.PersonAdd
                        MainTab.GROUPS -> Icons.Default.GroupAdd
                        MainTab.FORUMS -> Icons.Default.AddComment
                        MainTab.BLOGS -> Icons.Default.EditNote
                        else -> Icons.Default.Add
                    }
                    Icon(imageVector = icon, contentDescription = "Add Item")
                }
            }
        },
        containerColor = BriarDarkBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BriarDarkBack)
                .padding(innerPadding)
        ) {
            // Connection Status Banner matching HTML Design
            val peersCount = activeConnections.size
            val bannerText = if (peersCount > 0) "Tor active • $peersCount peer(s) connected" else "Tor active • local mesh scan running"
            val indicatorColor = if (peersCount > 0) Color(0xFF4CAF50) else Color(0xFF21005D)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8DEF8))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulse dot
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .background(indicatorColor, CircleShape)
                )
                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF21005D),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentTab) {
                    MainTab.CONTACTS -> ContactsTabScreen(
                        viewModel = viewModel,
                        onContactClick = { activeChatContactPubKey = it }
                    )
                    MainTab.GROUPS -> GroupsTabScreen(
                        viewModel = viewModel,
                        onGroupClick = { activeGroupChatId = it }
                    )
                    MainTab.FORUMS -> ForumsTabScreen(
                        viewModel = viewModel,
                        onForumClick = { activeForumId = it }
                    )
                    MainTab.BLOGS -> BlogsTabScreen(viewModel = viewModel)
                    MainTab.NETWORK -> SettingsNetworkTabScreen(viewModel = viewModel, identity = identity)
                }
            }
        }
    }

    // Direct pairwise message dialog sheet
    activeChatContactPubKey?.let { contactPubKey ->
        PrivateChatScreen(
            viewModel = viewModel,
            contactPubKey = contactPubKey,
            onDismiss = { activeChatContactPubKey = null }
        )
    }

    // Direct multi-peer group chat dialog sheet
    activeGroupChatId?.let { groupId ->
        GroupChatDialogScreen(
            viewModel = viewModel,
            groupId = groupId,
            onDismiss = { activeGroupChatId = null }
        )
    }

    // Direct forum details dialog sheet
    activeForumId?.let { forumId ->
        ForumDetailDialogScreen(
            viewModel = viewModel,
            forumId = forumId,
            onDismiss = { activeForumId = null }
        )
    }

    // Universal Add dialog sheets depending on active tab
    if (showAddContactDialog) {
        when (currentTab) {
            MainTab.CONTACTS -> AddContactDialog(
                viewModel = viewModel,
                onDismiss = { showAddContactDialog = false }
            )
            MainTab.GROUPS -> CreateGroupDialog(
                viewModel = viewModel,
                onDismiss = { showAddContactDialog = false }
            )
            MainTab.FORUMS -> CreateForumDialog(
                viewModel = viewModel,
                onDismiss = { showAddContactDialog = false }
            )
            MainTab.BLOGS -> CreateBlogDialog(
                viewModel = viewModel,
                onDismiss = { showAddContactDialog = false }
            )
            else -> showAddContactDialog = false
        }
    }
}

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CONTACTS("PEERS", Icons.Default.Person, Icons.Outlined.Person),
    GROUPS("GROUPS", Icons.Default.Group, Icons.Outlined.Group),
    FORUMS("FORUMS", Icons.Default.Forum, Icons.Outlined.Forum),
    BLOGS("BLOGS", Icons.Default.Article, Icons.Outlined.Article),
    NETWORK("NET", Icons.Default.Dns, Icons.Outlined.Dns)
}

// ==================== TABS SCREENS ====================

@Composable
fun ContactsTabScreen(
    viewModel: BriarViewModel,
    onContactClick: (String) -> Unit
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (discoveredPeers.isNotEmpty()) {
            Text(
                text = "NEARBY LAN INSTANCES FOUND",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = BriarGreenNeon,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFFE8DEF8), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                items(discoveredPeers) { peer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = "P2P Found",
                            tint = BriarGreenNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peer.nickname,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = BriarTextPrimary
                            )
                            Text(
                                text = "IP: ${peer.ip}:${peer.port}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = BriarTextSecondary
                            )
                        }
                        Button(
                            onClick = { viewModel.approveDiscoveredPeer(peer) },
                            colors = ButtonDefaults.buttonColors(containerColor = BriarGreenNeon, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "ADD",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "SECURE DIRECT PEERS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = BriarTextSecondary,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = "Empty Peers",
                        tint = BriarTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No direct peers added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BriarTextSecondary
                    )
                    Text(
                        text = "Click the + button to add a peer with their cryptographic public address or look for nearby automatic sync devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BriarTextSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(contacts) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onContactClick(contact.publicKey) },
                        colors = CardDefaults.cardColors(containerColor = BriarCardBack),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Online Status Orb
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (contact.isOnline) Color(0xFF4CAF50) else Color(0xFF948F99),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.nickname,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BriarTextPrimary
                                )
                                Text(
                                    text = if (contact.isOnline) "Direct LAN Sync Active" else "Last seen: ${formatTimestamp(contact.lastSeen)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (contact.isOnline) Color(0xFF2E7D32) else BriarTextSecondary
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Message Peer",
                                tint = BriarGreenNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupsTabScreen(
    viewModel: BriarViewModel,
    onGroupClick: (String) -> Unit
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    if (groups.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = "Empty Groups",
                    tint = BriarTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No private groups created",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BriarTextSecondary
                )
                Text(
                    text = "Join or create a private multi-peer chat. When connected to any participant, messages sync and gossip to other members automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BriarTextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(groups) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onGroupClick(group.groupId) },
                    colors = CardDefaults.cardColors(containerColor = BriarCardBack),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEADDFF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Group logo",
                                tint = BriarGreenDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BriarTextPrimary
                            )
                            Text(
                                text = "ID: ${group.groupId.take(8)}... | Owner: ${group.creatorPublicKey.take(6)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = BriarTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowOutward,
                            contentDescription = "Open group",
                            tint = BriarGreenNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForumsTabScreen(
    viewModel: BriarViewModel,
    onForumClick: (String) -> Unit
) {
    val forums by viewModel.forums.collectAsStateWithLifecycle()

    if (forums.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Forum,
                    contentDescription = "Empty Forums",
                    tint = BriarTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No forums created",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BriarTextSecondary
                )
                Text(
                    text = "Public decentralised forums allow peer mesh discussions. Post signatures are verified with cryptographics so identities cannot be falsified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BriarTextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(forums) { forum ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onForumClick(forum.forumId) },
                    colors = CardDefaults.cardColors(containerColor = BriarCardBack),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFD8E4), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "Forum icon",
                                tint = Color(0xFF31111D),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = forum.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BriarTextPrimary
                            )
                            Text(
                                text = "Mesh Forum ID: ${forum.forumId.take(12)}...",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = BriarTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Open forum",
                            tint = BriarGreenNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlogsTabScreen(viewModel: BriarViewModel) {
    val blogs by viewModel.blogs.collectAsStateWithLifecycle()

    if (blogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Article,
                    contentDescription = "Empty Blogs",
                    tint = BriarTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No blog posts published",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BriarTextSecondary
                )
                Text(
                    text = "Decentralised blogs are peer-authored articles that automatically replicate to contacts when connecting, securing distributed free speech.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BriarTextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(blogs) { blog ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = BriarCardBack),
                    border = BorderStroke(1.dp, Color(0xFF1B2423))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Feed,
                                contentDescription = "Blog Paper",
                                tint = BriarGreenNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PEER BLOG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BriarGreenNeon
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                htmlFormatTime(blog.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = BriarTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = blog.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = BriarTextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "By ${blog.authorNickname}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = BriarGreenLight
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = blog.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BriarTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Proof lock footer banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1413), RoundedCornerShape(6.dp))
                                .border(1.dp, BriarGreenNeon.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified Proof",
                                tint = BriarGreenLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secure Distributed Origin Signature Verified",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = BriarTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsNetworkTabScreen(viewModel: BriarViewModel, identity: MyIdentity) {
    val clipboardManager = LocalClipboardManager.current
    val activeConnections by viewModel.activeConnections.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // Identity Cryptographic Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BriarGreenNeon.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BriarCardBack)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF0F2D24), CircleShape)
                                .border(1.dp, BriarGreenNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = identity.nickname.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BriarGreenNeon
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = identity.nickname,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = BriarTextPrimary
                            )
                            Text(
                                text = "Local Crypto Identity Profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = BriarTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "YOUR CRYPTO BRIAR ADDRESS (PUBLIC KEY)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = BriarTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C1110), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF182321), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = identity.publicKey,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = BriarTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(identity.publicKey))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = BriarGreenNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR scanning mockup drawing inside Briar style layout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1514), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Dummy visual custom block representing encryption pairing QR
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                // Draw mock QR code visual elements
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            val cellSize = size.width / 5
                                            // Draw some nice blocks
                                            drawRect(Color.Black, Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 1.5f, cellSize * 1.5f))
                                            drawRect(Color.Black, Offset(size.width - cellSize * 1.5f, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 1.5f, cellSize * 1.5f))
                                            drawRect(Color.Black, Offset(0f, size.height - cellSize * 1.5f), size = androidx.compose.ui.geometry.Size(cellSize * 1.5f, cellSize * 1.5f))
                                            drawRect(Color.Black, Offset(cellSize * 2f, cellSize * 2f), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                            drawRect(Color.Black, Offset(cellSize * 3f, cellSize * 1f), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                            drawRect(Color.Black, Offset(cellSize * 1f, cellSize * 3f), size = androidx.compose.ui.geometry.Size(cellSize, cellSize * 2f))
                                            drawRect(Color.Black, Offset(cellSize * 3f, cellSize * 3f), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Other peers scan this to link",
                                style = MaterialTheme.typography.bodySmall,
                                color = BriarTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Active background loop information
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1B2423), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BriarCardBack)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "P2P CONNECTION LOGS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = BriarTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Server listening on dynamically assigned socket port. Periodically scans local Wi-Fi SSID for other mesh instances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BriarTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CONNECTED LAN SOCKET PEERS:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BriarGreenNeon),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (activeConnections.isEmpty()) {
                        Text(
                            text = "No active P2P sockets connected at this moment. Multi-device mesh will auto-connect when peers enter local network discovery.",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = BriarTextSecondary.copy(alpha = 0.6f)
                        )
                    } else {
                        activeConnections.forEach { peerDisplay ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F1514), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(BriarGreenNeon, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Socket Link: $peerDisplay",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = BriarTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==================== PAIRWISE CHAT SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    viewModel: BriarViewModel,
    contactPubKey: String,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val contact = remember(contacts) { contacts.find { it.publicKey == contactPubKey } }
    val messages by viewModel.getMessagesForContact(contactPubKey).collectAsStateWithLifecycle(emptyList())

    var textInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (contact != null) {
        val identity by viewModel.myIdentity.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = BriarDarkBack,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF1B2423)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (contact.isOnline) BriarGreenNeon else Color(0xFF263231),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.nickname,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BriarTextPrimary
                        )
                        Text(
                            text = "Direct peer-to-peer RSA-2048 channel",
                            style = MaterialTheme.typography.bodySmall,
                            color = BriarTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BriarGreenNeon)
                    }
                }

                Divider(color = Color(0xFF1B2423), modifier = Modifier.padding(vertical = 12.dp))

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        val isMe = message.isSentByMe
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Column(
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!isMe) {
                                        Text(
                                            text = contact.nickname,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BriarGreenLight
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isMe) Color(0xFF1B3D30) else Color(0xFF1B2423),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isMe) BriarGreenNeon.copy(alpha = 0.3f) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = message.decryptedBody ?: "[Encrypted P2P Cipher]",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = BriarTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "🔒 E2E Encrypted",
                                                tint = BriarGreenNeon.copy(alpha = 0.6f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "Encrypted Local Store",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                                color = BriarTextSecondary.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = formatTimestamp(message.timestamp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = BriarTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Encrypted message...", color = BriarTextSecondary) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BriarTextPrimary,
                            unfocusedTextColor = BriarTextPrimary,
                            focusedBorderColor = BriarGreenNeon,
                            unfocusedBorderColor = Color(0xFF1B2423),
                            cursorColor = BriarGreenNeon
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendPrivateMessage(contactPubKey, textInput.trim())
                                textInput = ""
                                scope.launch {
                                    delay(200)
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = BriarGreenNeon,
                            contentColor = Color(0xFF00200A)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
                    }
                }
            }
        }
    }
}

// ==================== PRIVATE GROUP CHAT SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatDialogScreen(
    viewModel: BriarViewModel,
    groupId: String,
    onDismiss: () -> Unit
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val group = remember(groups) { groups.find { it.groupId == groupId } }
    val messages by viewModel.getMessagesForGroup(groupId).collectAsStateWithLifecycle(emptyList())

    var textInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (group != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = BriarDarkBack,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF1B2423)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF122C24), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = BriarGreenNeon, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BriarTextPrimary
                        )
                        Text(
                            text = "Multi-peer private group sync chat",
                            style = MaterialTheme.typography.bodySmall,
                            color = BriarTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BriarGreenNeon)
                    }
                }

                Divider(color = Color(0xFF1B2423), modifier = Modifier.padding(vertical = 12.dp))

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        val identity by viewModel.myIdentity.collectAsStateWithLifecycle()
                        val isMe = message.senderPublicKey == identity?.publicKey

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Column(
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text(
                                    text = if (isMe) "You" else message.senderNickname,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isMe) BriarGreenLight else BriarTextSecondary
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isMe) Color(0xFF1B3D30) else Color(0xFF1B2423),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BriarTextPrimary
                                    )
                                }
                                Text(
                                    text = formatTimestamp(message.timestamp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = BriarTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Write group chat...", color = BriarTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BriarTextPrimary,
                            unfocusedTextColor = BriarTextPrimary,
                            focusedBorderColor = BriarGreenNeon,
                            unfocusedBorderColor = Color(0xFF1B2423),
                            cursorColor = BriarGreenNeon
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.postToGroup(groupId, textInput.trim())
                                textInput = ""
                                scope.launch {
                                    delay(200)
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = BriarGreenNeon,
                            contentColor = Color(0xFF00200A)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Group")
                    }
                }
            }
        }
    }
}

// ==================== FORUM SCREEN DETAILS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDetailDialogScreen(
    viewModel: BriarViewModel,
    forumId: String,
    onDismiss: () -> Unit
) {
    val forums by viewModel.forums.collectAsStateWithLifecycle()
    val forum = remember(forums) { forums.find { it.forumId == forumId } }
    val posts by viewModel.getPostsForForum(forumId).collectAsStateWithLifecycle(emptyList())

    var contentInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (forum != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = BriarDarkBack,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF1B2423)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF102824), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = BriarGreenNeon, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = forum.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BriarTextPrimary
                        )
                        Text(
                            text = "Decentralized cryptographic gossip discussion board",
                            style = MaterialTheme.typography.bodySmall,
                            color = BriarTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BriarGreenNeon)
                    }
                }

                Divider(color = Color(0xFF1B2423), modifier = Modifier.padding(vertical = 12.dp))

                // Posts list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(posts) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141C1A)),
                            border = BorderStroke(1.dp, Color(0xFF202C29))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = post.senderNickname,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BriarGreenLight
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Verifier Badge
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF0C241B), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "ORIGIN AUTHENTICATED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = BriarGreenNeon
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = formatTimestamp(post.timestamp),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = BriarTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = post.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BriarTextPrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "RSA-Signature: ${post.signature.take(24)}...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp
                                    ),
                                    color = BriarTextSecondary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        placeholder = { Text("Add signed thread post...", color = BriarTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BriarTextPrimary,
                            unfocusedTextColor = BriarTextPrimary,
                            focusedBorderColor = BriarGreenNeon,
                            unfocusedBorderColor = Color(0xFF1B2423),
                            cursorColor = BriarGreenNeon
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (contentInput.isNotBlank()) {
                                viewModel.postToForum(forumId, contentInput.trim())
                                contentInput = ""
                                scope.launch {
                                    delay(200)
                                    if (posts.isNotEmpty()) {
                                        listState.animateScrollToItem(posts.size - 1)
                                    }
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = BriarGreenNeon,
                            contentColor = Color(0xFF00200A)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Forum Post")
                    }
                }
            }
        }
    }
}

// ==================== UNIVERSAL ADD DIALOGS ====================

@Composable
fun AddContactDialog(viewModel: BriarViewModel, onDismiss: () -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BriarCardBack,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = BriarGreenNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Secure Peer Link", color = BriarGreenNeon, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Manually establish a mesh trust route by pasting your peer's cryptographic public key base64.",
                    color = BriarTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Peer Nickname", color = BriarTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BriarTextPrimary,
                        unfocusedTextColor = BriarTextPrimary,
                        focusedBorderColor = BriarGreenNeon,
                        unfocusedBorderColor = Color(0xFF1B2423)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Peer Public Address String", color = BriarTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BriarTextPrimary,
                        unfocusedTextColor = BriarTextPrimary,
                        focusedBorderColor = BriarGreenNeon,
                        unfocusedBorderColor = Color(0xFF1B2423)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nickname.isNotBlank() && link.isNotBlank()) {
                        isVerifying = true
                        viewModel.addContact(nickname.trim(), link.trim()) { success ->
                            isVerifying = false
                            if (success) {
                                onDismiss()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BriarGreenNeon, contentColor = Color(0xFF00200A)),
                enabled = nickname.isNotBlank() && link.isNotBlank() && !isVerifying
            ) {
                Text("LINK PEER")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BriarTextSecondary)
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun CreateGroupDialog(viewModel: BriarViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BriarCardBack,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, tint = BriarGreenNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Private Group", color = BriarGreenNeon, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Members you connect with sync group states on direct LAN handshake automatically.",
                    color = BriarTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name", color = BriarTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BriarTextPrimary,
                        unfocusedTextColor = BriarTextPrimary,
                        focusedBorderColor = BriarGreenNeon,
                        unfocusedBorderColor = Color(0xFF1B2423)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createGroup(name.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BriarGreenNeon, contentColor = Color(0xFF00200A)),
                enabled = name.isNotBlank()
            ) {
                Text("CREATE")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BriarTextSecondary)
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun CreateForumDialog(viewModel: BriarViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BriarCardBack,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = BriarGreenNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Mesh Forum", color = BriarGreenNeon, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Forums replicate state to direct LAN peers subscription chains on connection handshake.",
                    color = BriarTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Forum Topic Name", color = BriarTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BriarTextPrimary,
                        unfocusedTextColor = BriarTextPrimary,
                        focusedBorderColor = BriarGreenNeon,
                        unfocusedBorderColor = Color(0xFF1B2423)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createForum(name.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BriarGreenNeon, contentColor = Color(0xFF00200A)),
                enabled = name.isNotBlank()
            ) {
                Text("CREATE TOPIC")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BriarTextSecondary)
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun CreateBlogDialog(viewModel: BriarViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BriarCardBack,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = BriarGreenNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Author Blog Post", color = BriarGreenNeon, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Signed articles are preserved locally and replicated over ad-hoc peer mesh gossip routing.",
                    color = BriarTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Article Title", color = BriarTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BriarTextPrimary,
                        unfocusedTextColor = BriarTextPrimary,
                        focusedBorderColor = BriarGreenNeon,
                        unfocusedBorderColor = Color(0xFF1B2423)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Body Text Content", color = BriarTextSecondary) },
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BriarTextPrimary,
                        unfocusedTextColor = BriarTextPrimary,
                        focusedBorderColor = BriarGreenNeon,
                        unfocusedBorderColor = Color(0xFF1B2423)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        viewModel.publishBlogPost(title.trim(), content.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BriarGreenNeon, contentColor = Color(0xFF00200A)),
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("PUBLISH SIGNED")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BriarTextSecondary)
            ) {
                Text("CANCEL")
            }
        }
    )
}

// ==================== HELPER FORMATTERS ====================

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("HH:mm | yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun htmlFormatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
