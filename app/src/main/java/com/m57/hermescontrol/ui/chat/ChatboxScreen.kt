package com.m57.hermescontrol.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m57.hermescontrol.data.remote.ApiClient
import kotlinx.coroutines.launch

/** 宽屏阈值: 超过则直接双栏,否则抽屉式侧栏 */
private val WIDE_SCREEN_DP = 600

/**
 * Chatbox 风格界面: 左侧会话列表 + 右侧聊天区。
 * - 宽屏(平板/横屏): 固定显示双栏
 * - 窄屏(手机竖屏): 侧栏收起,点聊天页左上角菜单按钮弹出
 */
@Composable
fun ChatboxScreen(
    modifier: Modifier = Modifier,
    onOpenDrawer: (() -> Unit)? = null,
    sessionId: String? = null,
) {
    val api = remember { ApiClient.hermesApi }
    var sessions by remember { mutableStateOf<List<SessionItem>>(emptyList()) }
    var currentSessionId by remember(sessionId) { mutableStateOf(sessionId) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // 加载会话列表
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val resp = api.getSessions(limit = 50, offset = 0, order = "recent")
            val body = resp.body()
            val list = body?.sessions ?: emptyList()
            sessions = list.map { SessionItem(it.id, it.title ?: it.id) }
            if (currentSessionId == null) {
                currentSessionId = sessions.firstOrNull()?.id
            }
        } catch (e: Exception) {
            // 加载失败静默,聊天区仍可用
        } finally {
            isLoading = false
        }
    }

    val activeId = currentSessionId
    // 窄屏抽屉状态(由 ChatboxScreen 控制)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isWide by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        isWide = maxWidth >= WIDE_SCREEN_DP.dp

        if (isWide) {
            // ── 宽屏: 双栏布局 ──
            Row(modifier = Modifier.fillMaxSize()) {
                SessionSidebar(
                    sessions = sessions,
                    activeId = activeId,
                    isLoading = isLoading,
                    onSelect = { currentSessionId = it },
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                )
                ChatPane(
                    sessionId = activeId,
                    onOpenDrawer = onOpenDrawer,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            // ── 窄屏: 抽屉式侧栏 + 聊天 ──
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                        SessionSidebar(
                            sessions = sessions,
                            activeId = activeId,
                            isLoading = isLoading,
                            onSelect = {
                                currentSessionId = it
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            ) {
                // 聊天区: 菜单按钮打开会话侧栏
                ChatPane(
                    sessionId = activeId,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** 左侧会话列表栏 */
@Composable
private fun SessionSidebar(
    sessions: List<SessionItem>,
    activeId: String?,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        // 头部: 标题 + 新建
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()
        // 会话列表
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sessions, key = { it.id }) { session ->
                    val selected = session.id == activeId
                    SessionRow(
                        session = session,
                        selected = selected,
                        onClick = { onSelect(session.id) },
                    )
                }
            }
        }
    }
}

/** 右侧聊天区 */
@Composable
private fun ChatPane(
    sessionId: String?,
    onOpenDrawer: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        key(sessionId) {
            ChatScreen(
                modifier = Modifier.fillMaxSize(),
                onOpenDrawer = onOpenDrawer,
                sessionId = sessionId,
            )
        }
    }
}

/** 会话列表项 */
private data class SessionItem(
    val id: String,
    val title: String,
)

@Composable
private fun SessionRow(
    session: SessionItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = session.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
