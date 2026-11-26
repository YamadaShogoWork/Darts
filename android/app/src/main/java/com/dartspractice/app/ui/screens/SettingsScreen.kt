package com.dartspractice.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartspractice.app.BuildConfig
import com.dartspractice.app.appViewModel
import com.dartspractice.app.data.backup.BackupCodec
import com.dartspractice.app.data.debug.SampleData
import com.dartspractice.app.ui.Format
import com.dartspractice.app.ui.settings.ImportPreview
import com.dartspractice.app.ui.settings.SettingsMessage
import com.dartspractice.app.ui.settings.SettingsViewModel
import com.dartspractice.app.ui.theme.DartsColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private enum class DevAction { Insert, DeleteAll }

/** 前回のバックアップからこの日数以上空いたら赤字にする。 */
private const val BACKUP_WARN_DAYS = 7

/** 設定。バックアップの書き出し・読み込み(Storage Access Framework)。開発用の欄は debug ビルドだけに出す。 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAwards: () -> Unit) {
    ScreenOrientation(allowLandscape = false)
    val vm = appViewModel {
        SettingsViewModel(it.repository, it.stats, it.backup, it.backupPrefs, it.contentResolver, it.menuSelection)
    }
    val count by vm.count.collectAsStateWithLifecycle()
    val lastBackupAt by vm.lastBackupAt.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf<DevAction?>(null) }

    // ファイル保存画面 / ファイル選択画面
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::prepareImport)
    }

    preview?.let { ImportConfirmDialog(it, count.sessions, vm::confirmImport, vm::cancelImport) }
    confirm?.let { action -> DevConfirmDialog(action, onDismiss = { confirm = null }) {
        confirm = null
        if (action == DevAction.Insert) vm.insertSamples() else vm.deleteAll()
    } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackHeader(title = "設定", backLabel = "ホームへ戻る", onBack = onBack)

        SectionLabel("バックアップ")
        DartsCard(radius = 20.dp) {
            LastBackup(lastBackupAt)
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentButton("バックアップを書き出す", height = 52.dp, fontSizeSp = 16) {
                    exportLauncher.launch(BackupCodec.FILE_NAME)
                }
                Text(
                    "ファイル保存の画面で、保存する場所とファイル名を選びます。Download の直下に保存できないときは、" +
                        "保存画面の「新しいフォルダ」で専用のフォルダ(例: DartsBackup)を作って、その中に保存してください。",
                    fontSize = 12.sp,
                    color = DartsColors.SubText,
                )
                OutlineButton("バックアップを読み込む", Modifier.padding(top = 6.dp)) {
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*"))
                }
                Text(
                    "機種変更やデータが消えたときに使います。今のデータは読み込んだ内容に置き換わります(読み込む前に確認が出ます)。",
                    fontSize = 12.sp,
                    color = DartsColors.SubText,
                )
                message?.let { MessageText(it) }
            }
        }

        SectionLabel("アワード")
        DartsCard(radius = 20.dp, onClick = onOpenAwards) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("アワードの設定", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("アワードごとの ON/OFF と、演出のお試し", fontSize = 12.sp, color = DartsColors.SubText)
                }
                Text("›", fontSize = 18.sp, color = DartsColors.SubText)
            }
        }

        SectionLabel("データ")
        DartsCard(radius = 20.dp) {
            InfoRow("保存されている練習", "${count.sessions}回(${count.days}日分)")
            HorizontalDivider(color = DartsColors.Border, modifier = Modifier.padding(vertical = 10.dp))
            InfoRow("バックアップの形式", "JSON(${BackupCodec.FILE_NAME})")
        }

        if (BuildConfig.DEBUG) {
            SectionLabel("開発用(開発用ビルドだけに表示)")
            DartsCard(radius = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("サンプルデータを入れる") { confirm = DevAction.Insert }
                    Text(
                        "今日までの${SampleData.DAYS}日分の練習(1日1〜3回、休みの日あり)を追加します。",
                        fontSize = 12.sp,
                        color = DartsColors.SubText,
                    )
                    OutlineButton("すべてのデータを消す", Modifier.padding(top = 6.dp), color = DartsColors.Accent) {
                        confirm = DevAction.DeleteAll
                    }
                }
            }
        }
    }
}

/** 前回のバックアップ日時と、何日前か。長く空いたら赤字。 */
@Composable
private fun LastBackup(lastBackupAt: Long?) {
    Text("前回のバックアップ", fontSize = 12.sp, color = DartsColors.SubText)
    if (lastBackupAt == null) {
        Text("まだありません", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DartsColors.Accent)
        return
    }
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(lastBackupAt).atZone(zone).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(day, LocalDate.now(zone))
    val old = daysAgo >= BACKUP_WARN_DAYS
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${Format.dayWithWeek(day)} ${Format.time(lastBackupAt)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            when (daysAgo) {
                0L -> "今日"
                else -> "${daysAgo}日前"
            },
            fontSize = 13.sp,
            fontWeight = if (old) FontWeight.Bold else FontWeight.Normal,
            color = if (old) DartsColors.Accent else DartsColors.SubText,
        )
    }
    if (old) {
        Text("前回から${daysAgo}日空いています。書き出しておきましょう。", fontSize = 12.sp, color = DartsColors.Accent)
    }
}

@Composable
private fun MessageText(m: SettingsMessage) {
    Text(
        m.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (m.error) DartsColors.Accent else DartsColors.Text,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ImportConfirmDialog(p: ImportPreview, currentSessions: Int, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val exported = p.data.exportedAt?.let {
        val d = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        "${Format.dayWithWeek(d)} ${Format.time(it)} に書き出したファイルです。\n"
    }.orEmpty()
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = DartsColors.Surface,
        title = { Text("バックアップを読み込みますか?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "${exported}ファイルの中身: 練習${p.sessions}回(${p.days}日分)\n\n" +
                    "今の端末のデータ(練習${currentSessions}回)は、ファイルの内容に置き換わります。この操作は取り消せません。",
                color = DartsColors.SubText,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("読み込む", color = DartsColors.Accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("キャンセル", color = DartsColors.SubText) }
        },
    )
}

@Composable
private fun DevConfirmDialog(action: DevAction, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val (title, body, ok) = when (action) {
        DevAction.Insert -> Triple(
            "サンプルデータを入れますか?",
            "${SampleData.DAYS}日分のサンプルの練習を、今あるデータに追加します。",
            "入れる",
        )
        DevAction.DeleteAll -> Triple(
            "すべてのデータを消しますか?",
            "練習と1投ごとの記録がすべて消えます。この操作は取り消せません。",
            "消す",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DartsColors.Surface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body, color = DartsColors.SubText) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(ok, color = DartsColors.Accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル", color = DartsColors.SubText) }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DartsColors.SubText, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = DartsColors.SubText)
    }
}
