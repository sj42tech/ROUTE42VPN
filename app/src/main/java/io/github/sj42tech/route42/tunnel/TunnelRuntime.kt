package io.github.sj42tech.route42.tunnel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TunnelStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR,
}

data class TunnelState(
    val status: TunnelStatus = TunnelStatus.STOPPED,
    val profileId: String? = null,
    val profileName: String? = null,
    val errorMessage: String? = null,
    val publicIp: String? = null,
    val directPublicIp: String? = null,
    val localNetworkIp: String? = null,
    val resolvingPublicIp: Boolean = false,
    val logs: List<String> = emptyList(),
)

object TunnelRuntime {
    private const val MaxLogLines = 120
    @Volatile
    private var diagnosticsEnabled = false

    private val mutableState = MutableStateFlow(TunnelState())
    val state = mutableState.asStateFlow()

    fun setDiagnosticsEnabled(enabled: Boolean) {
        diagnosticsEnabled = enabled
        if (!enabled) {
            mutableState.update { it.copy(logs = emptyList()) }
        }
    }

    fun setStarting(profileId: String, profileName: String) {
        mutableState.update {
            it.copy(
                status = TunnelStatus.STARTING,
                profileId = profileId,
                profileName = profileName,
                errorMessage = null,
                publicIp = null,
                directPublicIp = null,
                localNetworkIp = null,
                resolvingPublicIp = false,
            )
        }
        appendLog("starting tunnel for $profileName")
    }

    fun setRunning(profileId: String, profileName: String) {
        mutableState.update {
            it.copy(
                status = TunnelStatus.RUNNING,
                profileId = profileId,
                profileName = profileName,
                errorMessage = null,
                publicIp = null,
                directPublicIp = null,
                localNetworkIp = null,
                resolvingPublicIp = true,
            )
        }
        appendLog("tunnel is running")
    }

    fun setStopping() {
        mutableState.update { it.copy(status = TunnelStatus.STOPPING) }
        appendLog("stopping tunnel")
    }

    fun setStopped(reason: String? = null) {
        mutableState.update {
            it.copy(
                status = TunnelStatus.STOPPED,
                profileId = null,
                profileName = null,
                errorMessage = null,
                publicIp = null,
                directPublicIp = null,
                localNetworkIp = null,
                resolvingPublicIp = false,
            )
        }
        if (!reason.isNullOrBlank()) {
            appendLog(reason)
        }
    }

    fun setError(message: String) {
        mutableState.update {
            it.copy(
                status = TunnelStatus.ERROR,
                errorMessage = message,
                publicIp = null,
                directPublicIp = null,
                localNetworkIp = null,
                resolvingPublicIp = false,
            )
        }
        appendLog(message)
    }

    fun setResolvedAddresses(publicIp: String?, directPublicIp: String?, localNetworkIp: String?) {
        mutableState.update {
            it.copy(
                publicIp = publicIp,
                directPublicIp = directPublicIp,
                localNetworkIp = localNetworkIp,
                resolvingPublicIp = false,
            )
        }
    }

    fun appendLog(message: String) {
        if (!diagnosticsEnabled) {
            return
        }
        mutableState.update { state ->
            val nextLogs = (state.logs + message.trim()).takeLast(MaxLogLines)
            state.copy(logs = nextLogs)
        }
    }
}
