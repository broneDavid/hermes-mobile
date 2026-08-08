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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m57.hermescontrol.data.remote.ApiClient

/**
 * Chatbox 风格界面: 左侧会话列表 + 右侧聊天区。
 * 会话列表固定宽 280dp(类 Chatbox/Telegram 桌面版)。
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

    // 当前会话在列表中的选中态同步
    val activeId = currentSessionId

    Row(modifier = modifier.fillMaxSize()) {
        // ── 左栏: 会话列表 ──
        Surface(
            modifier = Modifier.width(280.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 头部: 标题 + 新建
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Sessions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { /* 新建会话由右侧 ChatScreen 处理 */ }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "New session", modifier = Modifier.size(18.dp))
                    }
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
                                onClick = { currentSessionId = session.id },
                            )
                        }
                    }
                }
            }
        }

        // ── 右栏: 聊天区 ──
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            key(activeId) {
                ChatScreen(
                    modifier = Modifier.fillMaxSize(),
                    onOpenDrawer = onOpenDrawer,
                    sessionId = activeId,
                )
            }
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
