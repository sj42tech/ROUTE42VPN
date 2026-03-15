package io.sj42.vpn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.sj42.vpn.data.ProfilesRepository
import io.sj42.vpn.model.ConnectionProfile
import io.sj42.vpn.model.DnsMode
import io.sj42.vpn.model.MatchType
import io.sj42.vpn.model.ProfilesSnapshot
import io.sj42.vpn.model.RoutingAction
import io.sj42.vpn.model.RoutingMode
import io.sj42.vpn.model.RoutingRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfilesRepository(application.applicationContext)

    val snapshot = repository.profiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfilesSnapshot(),
    )

    suspend fun upsertProfile(profile: ConnectionProfile): String = repository.upsertProfile(profile)

    fun setRoutingMode(profileId: String, mode: RoutingMode) {
        viewModelScope.launch {
            repository.setRoutingMode(profileId, mode)
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

