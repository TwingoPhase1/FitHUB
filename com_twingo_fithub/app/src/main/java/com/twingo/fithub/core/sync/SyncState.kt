package com.twingo.fithub.core.sync

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    object Connected : SyncState
    data class Error(val message: String) : SyncState
}