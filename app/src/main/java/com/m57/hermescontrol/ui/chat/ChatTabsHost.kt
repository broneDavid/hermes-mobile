package com.m57.hermescontrol.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 多窗口标签宿主 — 在单个 ChatScreen 之上提供浏览器式标签页。
 *
 * 每个标签绑定一个独立 sessionId,内部用 key(sessionId) 强制 Compose
 * 为不同会话创建独立的 ChatViewModel 实例,互不干扰。
 */
@Composable
fun ChatTabsHost(
    modifier: Modifier = Modifier,
    onOpenDrawer: (() -> Unit)? = null,
    initialSessionId: String? = null,
) {
    // 标签列表: (tabId, sessionId, title)
    var tabs by remember { mutableStateOf(listOf<TabEntry>()) }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    var tabCounter by remember { mutableIntStateOf(0) }

    // 首次: 如果还没有标签,创建第一个
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            val first =
                TabEntry(
                    id = "tab-${tabCounter++}",
                    sessionId = initialSessionId,
                    title = "会话 1",
                )
            tabs = listOf(first)
            activeTabId = first.id
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部标签栏
        if (tabs.isNotEmpty()) {
            TabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelect = { id -> activeTabId = id },
                onClose = { id ->
                    val idx = tabs.indexOfFirst { it.id == id }
                    tabs = tabs.filterNot { it.id == id }
                    if (activeTabId == id) {
                        activeTabId = tabs.getOrNull(idx.coerceAtMost(tabs.size - 1))?.id
                    }
                },
                onAdd = {
                    val newTab =
                        TabEntry(
                            id = "tab-${tabCounter++}",
                            sessionId = null,
                            title = "会话 ${tabs.size + 1}",
                        )
                    tabs = tabs + newTab
                    activeTabId = newTab.id
                },
            )
        }

        // 内容区: 每个标签一个 ChatScreen(用 key 隔离状态)
        val active = tabs.firstOrNull { it.id == activeTabId }
        if (active != null) {
            KeyedChatScreen(
                key = active.id,
                sessionId = active.sessionId,
                onOpenDrawer = onOpenDrawer,
                onTitleChange = { title ->
                    tabs =
                        tabs.map {
                            if (it.id == active.id) it.copy(title = title) else it
                        }
                },
            )
        }
    }
}

/** 单个标签的聊天界面(带 key 隔离) */
@Composable
private fun KeyedChatScreen(
    key: String,
    sessionId: String?,
    onOpenDrawer: (() -> Unit)?,
    onTitleChange: (String) -> Unit,
) {
    // key() 强制 Compose 为不同标签创建独立状态
    key(key) {
        ChatScreen(
            sessionId = sessionId,
            onOpenDrawer = onOpenDrawer,
        )
    }
}

/** 标签数据 */
data class TabEntry(
    val id: String,
    val sessionId: String?,
    val title: String,
)

/** 顶部标签栏 */
@Composable
private fun TabBar(
    tabs: List<TabEntry>,
    activeTabId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTabId
            Surface(
                modifier =
                    Modifier
                        .padding(horizontal = 2.dp)
                        .clickable { onSelect(tab.id) },
                shape = MaterialTheme.shapes.small,
                color =
                    if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 100.dp),
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭标签",
                        modifier =
                            Modifier
                                .size(14.dp)
                                .clickable { onClose(tab.id) },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // 新建标签按钮
        IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建会话",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 标签宿主 ViewModel(占位,后续可扩展会话元数据同步) */
