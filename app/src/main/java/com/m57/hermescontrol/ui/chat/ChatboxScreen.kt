package com.m57.hermescontrol.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Chatbox 风格界面: 左侧会话列表 + 右侧聊天区。
 * - 宽屏(平板/横屏,>=840dp): 固定显示双栏
 * - 窄屏(手机竖屏): 只显示聊天,会话切换走顶部标题下拉
 *
 * 会话状态统一由 ChatViewModel 持有(单一数据源),本组件只做展示与转发。
 */
@Composable
fun ChatboxScreen(
    modifier: Modifier = Modifier,
    onOpenDrawer: (() -> Unit)? = null,
    sessionId: String? = null,
    viewModel: ChatViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 窄屏是否显示会话侧栏(非抽屉,普通覆盖层由全局导航抽屉负责)
    var showSidebar by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        if (isWide) {
            // ── 宽屏: 双栏布局 ──
            Row(modifier = Modifier.fillMaxSize()) {
                SessionSidebar(
                    sessions = state.sessions,
                    activeId = state.currentSessionId,
                    onSelect = { viewModel.switchSession(it) },
                    onCreateNew = { viewModel.createNewSession() },
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                )
                ChatPane(
                    sessionId = state.currentSessionId,
                    onOpenDrawer = onOpenDrawer,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            // ── 窄屏: 仅聊天区(会话切换走顶部下拉)──
            // 不嵌套抽屉: 汉堡按钮保持打开全局导航抽屉
            ChatPane(
                sessionId = state.currentSessionId,
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 左侧会话列表栏(宽屏用) */
@Composable
private fun SessionSidebar(
    sessions: List<SessionUi>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        // 头部: 标题 + 新建
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCreateNew, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "New session",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider()
        // 会话列表
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No sessions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        ChatScreen(
            modifier = Modifier.fillMaxSize(),
            onOpenDrawer = onOpenDrawer,
            sessionId = sessionId,
        )
    }
}

@Composable
private fun SessionRow(
    session: SessionUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val displayTitle = session.title.ifBlank { "New session" }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (session.messageCount > 0) {
                Text(
                    text = "${session.messageCount} messages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
