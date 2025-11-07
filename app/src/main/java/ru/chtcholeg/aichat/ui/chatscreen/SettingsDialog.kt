package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.chtcholeg.aichat.core.CompositeAgent
import ru.chtcholeg.aichat.core.SingleAgent
import ru.chtcholeg.aichat.ui.chatscreen.ChatState.Dialog.Settings.Agent
import ru.chtcholeg.aichat.ui.theme.AIChatTheme
import ru.chtcholeg.aichat.ui.views.BottomSheet
import ru.chtcholeg.aichat.ui.views.FloatTextField

private val MAX_TAB_AGENT_COUNT = maxOf(
    ChatState.Dialog.Settings.Tab.SingleAgents.items.size,
    ChatState.Dialog.Settings.Tab.CompositeAgents.items.size
)

@Composable
fun SettingsDialog(
    onAction: (ChatAction) -> Unit,
    settings: ChatState.Dialog.Settings,
    modifier: Modifier = Modifier,
) {
    BottomSheet(
        onDismissRequest = { onAction(ChatAction.HideDialog) },
        modifier = modifier,
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
        )
        Temperature(settings.temperature) { onAction(ChatAction.SetTemperature(it)) }

        Agents(settings, onAction)
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(
                onClick = { onAction(ChatAction.ResetChat) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Reset chat")
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { onAction(ChatAction.RefreshToken) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Refresh token")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Agents(
    settings: ChatState.Dialog.Settings,
    onAction: (ChatAction) -> Unit,
) {
    val selectedTab = settings.selectedTab
    val selectedTabIndex = selectedTab.index
    SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
        AgentsTab(ChatState.Dialog.Settings.Tab.SingleAgents, selectedTabIndex, onAction)
        AgentsTab(ChatState.Dialog.Settings.Tab.CompositeAgents, selectedTabIndex, onAction)
    }

    val selectedAgent = settings.selectedAgent
    if (selectedTab == ChatState.Dialog.Settings.Tab.SingleAgents) {
        SingleAgentItems(selectedAgent, onAction)
    } else {
        CompositeAgentsItems(selectedAgent, onAction)
    }
}

@Composable
private fun AgentsTab(
    tab: ChatState.Dialog.Settings.Tab,
    selectedTabIndex: Int,
    onAction: (ChatAction) -> Unit,
) {
    Tab(
        selected = selectedTabIndex == tab.index,
        onClick = { onAction(ChatAction.SelectTab(tab)) },
        text = { Text(text = tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun SingleAgentItems(selectedAgent: Agent, onAction: (ChatAction) -> Unit) {
    val types = ChatState.Dialog.Settings.Tab.SingleAgents.items
    for (i in 0..<MAX_TAB_AGENT_COUNT) {
        val type = if (i < types.size) types[i] else types.last()
        val isFake = i >= types.size
        SingleAgentItem(
            type = type,
            selectedAgent = selectedAgent,
            onSelected = { if (!isFake) onAction(ChatAction.SetSingleAgent(type)) },
            modifier = Modifier
                .padding(start = 8.dp)
                .alpha(if (isFake) 0f else 1f)
        )
    }
}

@Composable
private fun CompositeAgentsItems(selectedAgent: Agent, onAction: (ChatAction) -> Unit) {
    val types = ChatState.Dialog.Settings.Tab.CompositeAgents.items
    for (i in 0..<MAX_TAB_AGENT_COUNT) {
        val type = if (i < types.size) types[i] else types.last()
        val isFake = i >= types.size
        CompositeAgentItem(
            type = type,
            selectedAgent = selectedAgent,
            onSelected = { if (!isFake) onAction(ChatAction.SetCompositeAgent(type)) },
            modifier = Modifier
                .padding(start = 8.dp)
                .alpha(if (isFake) 0f else 1f)
        )
    }
}

private val ChatState.Dialog.Settings.Tab.index: Int
    get() = when (this) {
        ChatState.Dialog.Settings.Tab.SingleAgents -> 0
        ChatState.Dialog.Settings.Tab.CompositeAgents -> 1
    }

private val ChatState.Dialog.Settings.Tab.title: String
    get() = when (this) {
        ChatState.Dialog.Settings.Tab.SingleAgents -> "Single agent"
        ChatState.Dialog.Settings.Tab.CompositeAgents -> "Composite agent"
    }

@Composable
private fun Temperature(
    temperature: Float,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(
            text = "Temperature\n(≈0 - deterministic answer)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f),
        )
        Spacer(Modifier.width(8.dp))
        FloatTextField(
            value = temperature,
            onValueChange = { onValueChange(it ?: 1f) },
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun SingleAgentItem(
    type: SingleAgent.Type,
    selectedAgent: Agent,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedAgent is Agent.Single && selectedAgent.type == type
    AgentItem(isSelected, type.text, onSelected, modifier)
}

@Composable
private fun CompositeAgentItem(
    type: CompositeAgent.Type,
    selectedAgent: Agent,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedAgent is Agent.Composite && selectedAgent.type == type
    AgentItem(isSelected, type.text, onSelected, modifier)
}

@Composable
private fun AgentItem(
    isSelected: Boolean,
    text: String,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val SingleAgent.Type.text: String
    get() = when (this) {
        SingleAgent.Type.Regular -> "Returns plain text"
        is SingleAgent.Type.Json -> "Returns JSON (fields hardcoded)"
        is SingleAgent.Type.Xml -> "Returns XML (fields hardcoded)"
        SingleAgent.Type.FullFledgedAssistant -> "Asks the questions at once"
        SingleAgent.Type.SequentialAssistant -> "Asks the questions sequentially"
    }

private val CompositeAgent.Type.text: String
    get() = when (this) {
        CompositeAgent.Type.SEPARATE_TASK_SOLVER -> "One task setter, one task solver"
        CompositeAgent.Type.SEVERAL_TASK_SOLVERS -> "One task setter, several experts"
    }

@Preview(showBackground = true)
@Composable
fun SettingsDialogPreview() {
    AIChatTheme {
        SettingsDialog(
            onAction = {},
            settings = ChatState.Dialog.Settings(),
        )
    }
}