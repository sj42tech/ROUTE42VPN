package io.github.sj42tech.route42.config

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

internal fun jsonArrayOf(vararg values: String): JsonArray = buildJsonArray {
    values.forEach { add(JsonPrimitive(it)) }
}

internal fun Map<String, List<String>>.firstValue(key: String): String? =
    get(key)?.firstOrNull()?.takeIf(String::isNotBlank)

internal fun Map<String, List<String>>.firstValue(vararg keys: String): String? =
    keys.firstNotNullOfOrNull(::firstValue)

internal fun Map<String, List<String>>.csvValues(vararg keys: String): List<String> =
    firstValue(*keys)
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()
