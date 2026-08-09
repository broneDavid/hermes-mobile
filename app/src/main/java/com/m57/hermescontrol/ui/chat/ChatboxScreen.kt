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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m57.hermescontrol.data.remote.ApiClient
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Chatbox 风格界面: 左侧会话列表(按时间分组) + 右侧聊天区。
 * - 宽屏(平板/横屏,>=840dp): 固定显示双栏
 * - 窄屏(手机竖屏): 只显示聊天,会话切换走顶部标题下拉
 *
 * 会话选中态统一由 ChatViewModel 持有;侧栏列表自带 REST 数据(含时间戳用于分组)。
 */
@Composable
fun ChatboxScreen(
    modifier: Modifier = Modifier,
    onOpenDrawer: (() -> Unit)? = null,
    sessionId: String? = null,
    viewModel: ChatViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 侧栏会话列表(REST 拉取,带时间戳)
    var sessions by remember { mutableStateOf<List<SidebarSession>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 拉取会话列表
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val api = ApiClient.hermesApi
            val resp = api.getSessions(limit = 100, offset = 0, order = "recent")
            val body = resp.body()
            sessions =
                (body?.sessions ?: emptyList()).map { s ->
                    SidebarSession(
                        id = s.id,
                        title = s.title ?: s.id,
                        messageCount = s.message_count ?: 0,
                        startedAt = (s.started_at ?: 0.0).toLong(),
                    )
                }
        } catch (e: Exception) {
            // 加载失败: 保持空列表(聊天区仍可用)
        } finally {
            isLoading = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        if (isWide) {
            // ── 宽屏: 双栏布局 ──
            Row(modifier = Modifier.fillMaxSize()) {
                SessionSidebar(
                    sessions = sessions,
                    activeId = state.currentSessionId,
                    isLoading = isLoading,
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
            ChatPane(
                sessionId = state.currentSessionId,
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 侧栏会话项(含时间戳用于分组) */
private data class SidebarSession(
    val id: String,
    val title: String,
    val messageCount: Int,
    val startedAt: Long,
)

/** 按时间分组 */
private enum class SessionGroup(val label: String) {
    TODAY("今天"),
    YESTERDAY("昨天"),
    THIS_WEEK("本周"),
    EARLIER("更早"),
}

private fun groupFor(epochSeconds: Long): SessionGroup {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = TimeUnit.SECONDS.toMillis(epochSeconds) }

    val today =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    val yesterdayStart = today.clone() as Calendar
    yesterdayStart.add(Calendar.DAY_OF_YEAR, -1)
    val weekStart = today.clone() as Calendar
    weekStart.add(Calendar.DAY_OF_YEAR, -7)

    return when {
        !cal.before(today) -> SessionGroup.TODAY
        !cal.before(yesterdayStart) -> SessionGroup.YESTERDAY
        !cal.before(weekStart) -> SessionGroup.THIS_WEEK
        else -> SessionGroup.EARLIER
    }
}

/** 左侧会话列表栏(宽屏用,按时间分组) */
@Composable
private fun SessionSidebar(
    sessions: List<SidebarSession>,
    activeId: String?,
    isLoading: Boolean,
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
        // 会话列表(按时间分组)
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (sessions.isEmpty()) {
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
            // 按组聚合
            val grouped = sessions.groupBy { groupFor(it.startedAt) }
            val groupOrder = SessionGroup.entries
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupOrder.forEach { group ->
                    val groupSessions = grouped[group].orEmpty()
                    if (groupSessions.isNotEmpty()) {
                        // 分组标题
                        item(key = "header-${group.name}") {
                            Text(
                                text = group.label,
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                        // 组内会话
                        items(groupSessions, key = { it.id }) { session ->
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
    session: SidebarSession,
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
