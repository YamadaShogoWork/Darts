package com.dartspractice.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object GraphRoute

/** 日別詳細。date は "yyyy-MM-dd"。null のときは最新の日(下部ナビの「履歴」から開く)。 */
@Serializable data class DetailRoute(val date: String? = null)

@Serializable data class RecordRoute(val sessionId: Long)
@Serializable data class ResultRoute(val sessionId: Long = 0L)
@Serializable object SettingsRoute
