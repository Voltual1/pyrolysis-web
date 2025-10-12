//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.bot.ui.bot

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.bbq.xq.bot.BotStatus
import cc.bbq.xq.bot.data.BotConfigDataStore
import cc.bbq.xq.bot.ui.theme.BBQButton
import cc.bbq.xq.bot.ui.theme.BBQCard
import cc.bbq.xq.bot.ui.theme.BBQOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotSettingsScreen(
    viewModel: BotSettingsViewModel,
  //  onBackClick: () -> Unit,
    onNavigateToBotLogin: () -> Unit,
    modifier: Modifier = Modifier // 新增：接收外部 modifier
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val status by viewModel.botStatus.collectAsStateWithLifecycle()
    val botUsername by viewModel.botAccountUsername.collectAsStateWithLifecycle()
//    val context = LocalContext.current
    
    // 核心修改 #2: 在屏幕可见时，刷新机器人账号状态
    LaunchedEffect(Unit) {
        viewModel.checkBotAccountStatus()
    }

    // 移除 Scaffold 包装，直接使用 LazyColumn
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ControlCard(
                status = status,
                isEnabled = config?.isBotEnabled ?: false,
                onEnabledChange = { viewModel.onBotEnabledChanged(it) }
            )
        }

        item {
            BotAccountCard(
                username = botUsername,
                onConfigureClick = onNavigateToBotLogin,
                onLogoutClick = { viewModel.logoutBotAccount() }
            )
        }

        item {
            ApiConfigCard(
                apiKey = config?.apiKey ?: "",
                modelName = config?.modelName ?: "",
                apiEndpoint = config?.apiEndpoint ?: "",
                onApiKeyChange = { viewModel.updateApiKey(it) },
                onModelNameChange = { viewModel.updateModelName(it) },
                onApiEndpointChange = { viewModel.updateApiEndpoint(it) }
            )
        }

        item {
            BehaviorConfigCard(
                pollingInterval = config?.pollingIntervalSeconds?.toString() ?: "",
                onPollingIntervalChange = { viewModel.updatePollingInterval(it) }
            )
        }

        item {
            PromptTemplateCard(
                prompt = config?.promptTemplate ?: "",
                onPromptChange = { viewModel.updatePromptTemplate(it) }
            )
        }
        
        item {
            AdvancedFeaturesCard(
                isSuperCacheEnabled = config?.isSuperCacheEnabled ?: false,
                onSuperCacheEnabledChange = { viewModel.onSuperCacheEnabledChanged(it) },
                onClearCacheClick = { viewModel.clearNetworkCache() }
            )
        }
    }
}

// 其他 Composable 函数保持不变...
@Composable
private fun AdvancedFeaturesCard(
    isSuperCacheEnabled: Boolean,
    onSuperCacheEnabledChange: (Boolean) -> Unit,
    onClearCacheClick: () -> Unit
) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("高级功能", style = MaterialTheme.typography.titleMedium)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用超级缓存", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "开启后，所有可缓存的网络请求将被记录。在\"缓存模式\"下可实现离线浏览。", // 修复引号转义
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(checked = isSuperCacheEnabled, onCheckedChange = onSuperCacheEnabledChange)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            BBQOutlinedButton(
                onClick = onClearCacheClick,
                text = { Text("清空所有网络缓存") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BotAccountCard(
    username: String?,
    onConfigureClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("机器人专用账号", style = MaterialTheme.typography.titleMedium)

            if (username != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("当前账号:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    BBQOutlinedButton(
                        onClick = onLogoutClick,
                        text = { Text("登出") }
                    )
                }
            } else {
                Text(
                    "提示：如果未配置专用账号，机器人将使用您当前登录的主账号执行操作。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BBQButton(
                    onClick = onConfigureClick,
                    text = { Text("配置 / 登录机器人账号") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ControlCard(
    status: BotStatus,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("启用机器人", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
            }
            Spacer(Modifier.height(8.dp))
            
            val statusText = when (status) {
                BotStatus.IDLE -> "已停止"
                BotStatus.RUNNING -> "运行中..."
                BotStatus.ERROR -> "遇到错误，将重试"
                BotStatus.STOPPING -> "正在停止..."
            }
            val statusColor = when (status) {
                BotStatus.ERROR -> MaterialTheme.colorScheme.error
                BotStatus.RUNNING -> Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = "状态: $statusText",
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ApiConfigCard(
    apiKey: String,
    modelName: String,
    apiEndpoint: String,
    onApiKeyChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onApiEndpointChange: (String) -> Unit
) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("API 配置 (兼容 OpenAI 格式)", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = apiEndpoint,
                onValueChange = onApiEndpointChange,
                label = { Text("API 端点 (Endpoint)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(BotConfigDataStore.DEFAULT_API_ENDPOINT) }
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key (sk-...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = modelName,
                onValueChange = onModelNameChange,
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun BehaviorConfigCard(
    pollingInterval: String,
    onPollingIntervalChange: (String) -> Unit
) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("行为配置", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = pollingInterval,
                onValueChange = onPollingIntervalChange,
                label = { Text("轮询间隔 (秒)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
private fun PromptTemplateCard(
    prompt: String,
    onPromptChange: (String) -> Unit
) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Prompt 模板", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                label = { Text("在此处编辑 Prompt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}