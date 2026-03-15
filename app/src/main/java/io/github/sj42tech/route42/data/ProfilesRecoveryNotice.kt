package io.github.sj42tech.route42.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProfilesRecoveryNotice {
    private const val StorageResetMessage =
        "Protected local profile storage was reset because the saved encrypted data could not be recovered on this device. Import your profiles again."

    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()

    fun notifyStorageReset() {
        mutableMessage.value = StorageResetMessage
    }

    fun dismiss() {
        mutableMessage.value = null
    }
}
