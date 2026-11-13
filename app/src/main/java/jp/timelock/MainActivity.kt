package jp.timelock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private data class LauncherApp(val packageName: String, val label: String)
private enum class AppFilter(val label: String) {
 ALL("すべて"), TODAY("今日制限あり"), PLANNED("翌日以降に制限あり")
}

@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val apps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
   .filter { it.activityInfo.packageName != packageName && it.activityInfo.packageName != "com.android.settings" && it.activityInfo.packageName != packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName }
   .distinctBy { it.activityInfo.packageName }
   .map { LauncherApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
   .sortedBy { it.label }
  setContent {
   MaterialTheme(colorScheme = darkColorScheme()) {
    val rules = remember { Rules(this) }
    var today by remember { mutableStateOf(rules.today()) }
    var plan by remember { mutableStateOf(rules.plan()) }
    var usage by remember { mutableStateOf(emptyMap<String,Long>()) }
    var allowed by remember { mutableStateOf(false) }
    var guardEnabled by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }
    val visibleApps = remember(apps, query, filter, today, plan) {
     val term = query.trim()
     apps.filter { app ->
      (term.isEmpty() || app.label.contains(term, ignoreCase = true) || app.packageName.contains(term, ignoreCase = true)) &&
       when (filter) {
        AppFilter.ALL -> true
        AppFilter.TODAY -> app.packageName in today
        AppFilter.PLANNED -> app.packageName in plan
       }
     }
    }
    var selected by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while (true) { allowed = Usage.allowed(this@MainActivity); guardEnabled = getSystemService(android.view.accessibility.AccessibilityManager::class.java).getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { it.resolveInfo.serviceInfo.packageName == packageName }; today = rules.today(); if (allowed) usage = Usage.totals(this@MainActivity); delay(2000) } }
    Surface(Modifier.fillMaxSize()) {
     LazyColumn(Modifier.safeDrawingPadding().widthIn(max = 840.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
       Text("TimeLock", style = MaterialTheme.typography.headlineLarge)
       Text("今日の時間を、大切に。", style = MaterialTheme.typography.titleMedium)
       Spacer(Modifier.height(16.dp))
       Text("上限は毎日0時に確定します。初回設定も明日から有効です。当日の変更・延長・解除はできません。")
       Text("計測: " + if (allowed) "許可済み" else "利用状況へのアクセスが必要")
       Button(onClick = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text("利用状況へのアクセス設定") }
       Text("ブロックサービス: " + if (guardEnabled) "有効" else "無効（ユーザー補助の許可が必要）")
       Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("TimeLockのユーザー補助を有効化") }
       Text("ユーザー補助で超過アプリからホームへ戻します。権限解除・削除で回避可能です。設定、ホーム、TimeLock自身は制限対象外です。")
       Spacer(Modifier.height(16.dp)); Text("アプリごとの上限", style = MaterialTheme.typography.titleLarge)
      }
      stickyHeader {
       Surface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
         OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("アプリ名・パッケージ名で検索") },
          singleLine = true,
          trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("クリア") } }
         )
         Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AppFilter.entries.forEach { option ->
           FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option.label) })
          }
         }
         Text("${visibleApps.size} / ${apps.size} 件", style = MaterialTheme.typography.bodySmall)
        }
       }
      }
      if (visibleApps.isEmpty()) {
       item {
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
         Text("条件に一致するアプリがありません")
         TextButton(onClick = { query = ""; filter = AppFilter.ALL }) { Text("絞り込みを解除") }
        }
       }
      }
      items(visibleApps, key = { it.packageName }) { app ->
       val pkg = app.packageName
       Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
         Text(app.label, style = MaterialTheme.typography.titleMedium)
         Text("今日: ${today[pkg]?.let { "$it 分" } ?: "制限なし"}  /  使用 ${((usage[pkg] ?: 0L) / 60000)} 分")
         Text("翌日以降: ${plan[pkg]?.let { "$it 分" } ?: "制限なし"}")
         TextButton(onClick = { selected = pkg; input = (plan[pkg] ?: 30).toString() }) { Text("翌日以降の設定") }
        }
       }
      }
     }
    }
    selected?.let { pkg ->
     AlertDialog(onDismissRequest = { selected = null }, title = { Text("明日からの1日上限") }, text = {
      Column { Text("1〜1440分。0は翌日以降の制限解除。今日の上限は変わりません。")
       OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("分") }, singleLine = true) }
     }, confirmButton = { TextButton(enabled = input.toIntOrNull() in 0..1440, onClick = { rules.schedule(pkg,input.toInt()); plan = rules.plan(); selected = null }) { Text("保存") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("キャンセル") } })
    }
   }
  }
 }
}
