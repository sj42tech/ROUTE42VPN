package io.sj42.vpn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.sj42.vpn.model.ConnectionProfile
import io.sj42.vpn.model.DnsMode
import io.sj42.vpn.model.ProfilesSnapshot
import io.sj42.vpn.model.RoutingMode
import io.sj42.vpn.model.RoutingRule
import io.sj42.vpn.model.defaultDnsMode

private val Context.profilesDataStore: DataStore<ProfilesSnapshot> by dataStore(
    fileName = "profiles.json",
    serializer = ProfilesSnapshotSerializer,
)

class ProfilesRepository(private val context: Context) {
    val profiles = context.profilesDataStore.data

    suspend fun upsertProfile(profile: ConnectionProfile): String {
        context.profilesDataStore.updateData { snapshot ->
            val remaining = snapshot.profiles.filterNot { it.id == profile.id }
            snapshot.copy(profiles = (remaining + profile).sortedByDescending(ConnectionProfile::createdAtEpochMillis))
        }
        return profile.id
    }

    suspend fun setRoutingMode(profileId: String, mode: RoutingMode) {
        updateProfile(profileId) { profile ->
            profile.copy(
                routing = profile.routing.copy(
                    mode = mode,
                    dnsMode = profile.routing.dnsMode.takeUnless { it == profile.routing.mode.defaultDnsMode() }
                        ?: mode.defaultDnsMode(),
                ),
            )
        }
    }

    suspend fun setDnsMode(profileId: String, dnsMode: DnsMode) {
        updateProfile(profileId) { profile ->
            profile.copy(routing = profile.routing.copy(dnsMode = dnsMode))
        }
    }

    suspend fun addRule(profileId: String, rule: RoutingRule) {
        updateProfile(profileId) { profile ->
            profile.copy(routing = profile.routing.copy(rules = profile.routing.rules + rule))
        }
    }

    suspend fun updateRule(profileId: String, rule: RoutingRule) {
        updateProfile(profileId) { profile ->
            profile.copy(
                routing = profile.routing.copy(
                    rules = profile.routing.rules.map { existing ->
                        if (existing.id == rule.id) rule else existing
                    },
                ),
            )
        }
    }

    suspend fun deleteRule(profileId: String, ruleId: String) {
        updateProfile(profileId) { profile ->
            profile.copy(
                routing = profile.routing.copy(
                    rules = profile.routing.rules.filterNot { it.id == ruleId },
                ),
            )
        }
    }

    private suspend fun updateProfile(
        profileId: String,
        transform: (ConnectionProfile) -> ConnectionProfile,
    ) {
        context.profilesDataStore.updateData { snapshot ->
            snapshot.copy(
                profiles = snapshot.profiles.map { profile ->
                    if (profile.id == profileId) transform(profile) else profile
                },
            )
        }
    }
}
