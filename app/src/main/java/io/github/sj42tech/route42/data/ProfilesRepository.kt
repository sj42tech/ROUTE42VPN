package io.github.sj42tech.route42.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.model.defaultDnsMode

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
