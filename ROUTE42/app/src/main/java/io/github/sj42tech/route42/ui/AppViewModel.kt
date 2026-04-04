package io.github.sj42tech.route42.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sj42tech.route42.data.ProfilesRepository
import io.github.sj42tech.route42.data.ProfilesRecoveryNotice
import io.github.sj42tech.route42.config.SingBoxConfigGenerator
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.model.RoutingRuleSource
import io.github.sj42tech.route42.model.ThemeMode
import io.github.sj42tech.route42.model.migrated
import io.github.sj42tech.route42.model.routingProfileFor
import io.github.sj42tech.route42.tunnel.ProfileHealthCheck
import io.github.sj42tech.route42.tunnel.ProfileHealthCheckRunner
import io.github.sj42tech.route42.tunnel.TunnelRuntime
import io.github.sj42tech.route42.tunnel.TunnelState
import io.github.sj42tech.route42.tunnel.TunnelStatus
import io.nekohasekai.libbox.Libbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfilesRepository(application.applicationContext)
    private val mutableProfileHealthChecks = MutableStateFlow<Map<String, ProfileHealthCheck>>(emptyMap())
    private val autoHealthCheckSignatures = mutableMapOf<String, String>()

    val snapshot = repository.profiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfilesSnapshot(),
    )
    val storageRecoveryNotice = ProfilesRecoveryNotice.message.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
    val profileHealthChecks = mutableProfileHealthChecks.asStateFlow()

    init {
        viewModelScope.launch {
            TunnelRuntime.state.collectLatest(::handleTunnelStateForHealthCheck)
        }
    }

    suspend fun upsertProfile(profile: ConnectionProfileWithRouting): String = repository.upsertProfile(profile)

    fun dismissStorageRecoveryNotice() {
        ProfilesRecoveryNotice.dismiss()
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }

    fun setRoutingMode(profileId: String, mode: RoutingMode) {
        viewModelScope.launch {
            repository.setRoutingMode(profileId, mode)
        }
    }

    fun assignRoutingProfile(profileId: String, routingProfileId: String) {
        viewModelScope.launch {
            repository.assignRoutingProfile(profileId, routingProfileId)
        }
    }

    fun duplicateRoutingProfile(profileId: String) {
        viewModelScope.launch {
            repository.duplicateRoutingProfile(profileId)
        }
    }

    fun createPresetRoutingProfile(profileId: String, preset: RoutingPreset) {
        viewModelScope.launch {
            repository.createPresetRoutingProfile(profileId, preset)
        }
    }

    fun setDnsMode(profileId: String, dnsMode: DnsMode) {
        viewModelScope.launch {
            repository.setDnsMode(profileId, dnsMode)
        }
    }

    fun addRule(
        profileId: String,
        action: RoutingAction,
        matchType: MatchType = MatchType.DOMAIN,
        value: String = "",
    ) {
        viewModelScope.launch {
            repository.addRule(
                profileId = profileId,
                rule = RoutingRule(
                    action = action,
                    matchType = matchType,
                    value = value,
                    source = RoutingRuleSource.USER,
                ),
            )
        }
    }

    fun updateRule(profileId: String, rule: RoutingRule) {
        viewModelScope.launch {
            repository.updateRule(profileId, rule)
        }
    }

    fun deleteRule(profileId: String, ruleId: String) {
        viewModelScope.launch {
            repository.deleteRule(profileId, ruleId)
        }
    }

    fun runProfileHealthCheck(profile: ConnectionProfile, routingProfile: RoutingProfile) {
        mutableProfileHealthChecks.update { currentChecks ->
            currentChecks + (profile.id to ProfileHealthCheck(
                running = true,
                summary = "Checking profile and tunnel path...",
            ))
        }
        viewModelScope.launch {
            val resolvedProfile = ConnectionProfileWithRouting(
                profile = profile,
                routingProfile = routingProfile,
            )
            val result = ProfileHealthCheckRunner.run(
                profile = resolvedProfile,
                configCheck = {
                    val config = SingBoxConfigGenerator.generate(resolvedProfile)
                    Libbox.checkConfig(config)
                },
                tunnelState = TunnelRuntime.state.value,
            )
            mutableProfileHealthChecks.update { currentChecks ->
                currentChecks + (profile.id to result)
            }
        }
    }

    private fun handleTunnelStateForHealthCheck(tunnelState: TunnelState) {
        if (tunnelState.status !in setOf(TunnelStatus.RUNNING, TunnelStatus.STARTING)) {
            autoHealthCheckSignatures.clear()
            return
        }

        val profileId = tunnelState.profileId ?: return
        if (tunnelState.status != TunnelStatus.RUNNING) {
            return
        }

        val signature = listOf(
            tunnelState.status.name,
            tunnelState.publicIp.orEmpty(),
            tunnelState.directPublicIp.orEmpty(),
        ).joinToString("|")
        val latestCheck = mutableProfileHealthChecks.value[profileId]
        if (autoHealthCheckSignatures[profileId] == signature || latestCheck?.running == true) {
            return
        }

        val migratedSnapshot = snapshot.value.migrated()
        val profile = migratedSnapshot.profiles.firstOrNull { it.id == profileId } ?: return
        autoHealthCheckSignatures[profileId] = signature
        runProfileHealthCheck(
            profile = profile,
            routingProfile = migratedSnapshot.routingProfileFor(profile),
        )
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return AppViewModel(application) as T
            }
        }
    }
}
