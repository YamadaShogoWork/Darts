package com.dartspractice.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartspractice.app.DartsApplication
import com.dartspractice.app.domain.rule.Award
import com.dartspractice.app.ui.theme.DartsColors

/** アワードの設定: アワードごとの ON/OFF と、演出を試す「見る」ボタン。 */
@Composable
fun AwardSettingsScreen(onBack: () -> Unit) {
    ScreenOrientation(allowLandscape = false)
    val prefs = (LocalContext.current.applicationContext as DartsApplication).awardPrefs
    val enabled by prefs.enabled.collectAsStateWithLifecycle()
    var preview by remember { mutableStateOf<AwardEvent?>(null) }
    var count by remember { mutableLongStateOf(0L) }

    AwardOverlay(preview, Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackHeader(title = "アワードの設定", backLabel = "設定へ戻る", onBack = onBack)
            Text(
                "1ターンが終わったとき、当てはまるアワードのうち ON の中で上にあるものを1つ出します。" +
                    "マーク数は実際に入った数で数えます。「見る」で演出を試せます。",
                fontSize = 12.sp,
                color = DartsColors.SubText,
            )
            DartsCard(radius = 20.dp) {
                Award.entries.forEachIndexed { i, award ->
                    if (i > 0) HorizontalDivider(color = DartsColors.Border, modifier = Modifier.padding(vertical = 8.dp))
                    AwardRow(
                        award = award,
                        on = award in enabled,
                        onToggle = { prefs.setEnabled(award, it) },
                        onPreview = { preview = AwardEvent(award, ++count) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AwardRow(award: Award, on: Boolean, onToggle: (Boolean) -> Unit, onPreview: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                award.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (on) DartsColors.Text else DartsColors.SubText,
            )
            Text("${award.description} ・ 段階${award.tier}", fontSize = 12.sp, color = DartsColors.SubText)
        }
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier.padding(horizontal = 10.dp).height(32.dp).clip(shape)
                .border(1.dp, DartsColors.Border, shape)
                .clickable(onClick = onPreview)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("見る", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Switch(
            checked = on,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DartsColors.OnAccent,
                checkedTrackColor = DartsColors.Accent,
                uncheckedThumbColor = DartsColors.SubText,
                uncheckedTrackColor = DartsColors.Track,
                uncheckedBorderColor = DartsColors.Border,
            ),
        )
    }
}
