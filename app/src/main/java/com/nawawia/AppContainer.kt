package com.nawawia

import android.content.Context
import com.nawawia.core.clock.AppClock
import com.nawawia.core.clock.SystemAppClock
import com.nawawia.core.result.AppResult
import com.nawawia.data.audio.AudioController
import com.nawawia.data.content.ContentBundle
import com.nawawia.data.content.ContentLoader
import com.nawawia.data.db.NawawiaDao
import com.nawawia.data.db.NawawiaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual Dependency Injection container owned by NawawiaApp.
 * No reflection, no KSP/annotation processing for DI, fast cold start.
 */
interface AppContainer {
    val clock: AppClock
    val applicationScope: CoroutineScope
    val database: NawawiaDatabase
    val dao: NawawiaDao
    val audioController: AudioController
    val contentResult: AppResult<ContentBundle>
}

class DefaultAppContainer(
    val context: Context,
    inMemoryDb: Boolean = false
) : AppContainer {
    override val clock: AppClock = SystemAppClock()
    override val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val database: NawawiaDatabase by lazy { NawawiaDatabase.build(context, inMemory = inMemoryDb) }
    override val dao: NawawiaDao by lazy { database.dao() }
    override val audioController: AudioController by lazy { AudioController(context) }
    override val contentResult: AppResult<ContentBundle> by lazy { ContentLoader.loadFromAssets(context) }
}
