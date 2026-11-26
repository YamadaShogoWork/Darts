package com.dartspractice.app.data

import android.content.Context
import com.dartspractice.app.data.db.MenuEntity
import com.dartspractice.app.domain.rule.PracticeRule
import com.dartspractice.app.domain.rule.PracticeRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** メニューとそのルール。 */
data class SelectedMenu(val menu: MenuEntity, val rule: PracticeRule)

/**
 * ホームで選んでいるメニュー(端末内の小さな設定ファイルに保存)。
 * ホーム・練習開始・グラフ・履歴は、ここで選んでいるメニューのデータだけを出す。
 */
class MenuSelection(context: Context, private val repo: PracticeRepository) {
    private val prefs = context.getSharedPreferences("menu", Context.MODE_PRIVATE)
    private val selectedId = MutableStateFlow(prefs.getLong(KEY_SELECTED, -1L))

    /** 選んでいるメニュー。保存した id のメニューがなければ(初回・読み込み後など)先頭のメニュー。 */
    val selected: Flow<SelectedMenu> = selectedId.map { id ->
        val menus = repo.menus()
        val menu = menus.firstOrNull { it.id == id } ?: menus.first()
        SelectedMenu(menu, PracticeRules.of(menu.ruleType))
    }

    /** 並べるメニューの一覧。 */
    suspend fun menus(): List<MenuEntity> = repo.menus()

    fun select(menuId: Long) {
        prefs.edit().putLong(KEY_SELECTED, menuId).apply()
        selectedId.value = menuId
    }

    private companion object {
        const val KEY_SELECTED = "selectedMenuId"
    }
}
