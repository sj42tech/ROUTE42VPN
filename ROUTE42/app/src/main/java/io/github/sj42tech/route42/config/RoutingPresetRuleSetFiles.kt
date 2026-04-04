package io.github.sj42tech.route42.config

object RoutingPresetRuleSetFiles {
    @Volatile
    var ruGeoipRuleSetPathProvider: () -> String? = { null }
}
