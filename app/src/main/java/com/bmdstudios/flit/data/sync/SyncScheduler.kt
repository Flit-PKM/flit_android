package com.bmdstudios.flit.data.sync

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton

private const val MUTATION_DEBOUNCE_MS = 3_000L
private const val PERIODIC_SYNC_INTERVAL_MS = 5 * 60 * 1000L
private const val SYNC_TAG = "Sync"

/**
 * Schedules background sync with Flit Core when connected.
 * Runs sync on startup, after entity mutations (debounced), and every 5 minutes while app is in foreground.
 * Does not update UI sync state; manual sync in Settings uses [SettingsViewModel.sync] instead.
 */
@Singleton
class SyncScheduler constructor(
    private val syncRepository: SyncRepository,
    private val settings: SettingsRepository,
    private val context: Context
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mutationDebounceJob: kotlinx.coroutines.Job? = null
    private var periodicSyncJob: kotlinx.coroutines.Job? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Runs sync if an access token exists. Returns null when not connected, otherwise the sync result.
     * Logs errors; does not expose to UI.
     */
    suspend fun runSyncIfConnected(): SyncRepository.SyncResult? = withContext(Dispatchers.IO) {
        val token = settings.getAccessToken()
        if (token == null) {
            Timber.tag(SYNC_TAG).d("SyncScheduler: not connected, skipping")
            return@withContext null
        }
        val result = syncRepository.runSync()
        when (result) {
            is SyncRepository.SyncResult.NotAuthenticated ->
                Timber.tag(SYNC_TAG).d("SyncScheduler: sync not authenticated")
            is SyncRepository.SyncResult.Error ->
                Timber.tag(SYNC_TAG).w("SyncScheduler: sync error: ${result.message}")
            is SyncRepository.SyncResult.Success ->
                Timber.tag(SYNC_TAG).d("SyncScheduler: sync success")
        }
        result
    }

    /**
     * Schedules a sync after a short debounce. Call after any entity create/update/delete.
     * If not connected, the scheduled run will no-op.
     */
    fun scheduleSyncAfterMutation() {
        mutationDebounceJob?.cancel()
        mutationDebounceJob = scope.launch {
            delay(MUTATION_DEBOUNCE_MS)
            runSyncIfConnected()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        startPeriodicSync()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopPeriodicSync()
    }

    private fun startPeriodicSync() {
        stopPeriodicSync()
        periodicSyncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                runSyncIfConnected()
                delay(PERIODIC_SYNC_INTERVAL_MS)
            }
        }
        Timber.tag(SYNC_TAG).d("SyncScheduler: periodic sync started (every 5 min)")
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
        Timber.tag(SYNC_TAG).d("SyncScheduler: periodic sync stopped")
    }

    /** Call from Application or tests to release observer and scope. Not required for normal app lifecycle. */
    fun shutdown() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        stopPeriodicSync()
        mutationDebounceJob?.cancel()
        scope.cancel()
    }
}
