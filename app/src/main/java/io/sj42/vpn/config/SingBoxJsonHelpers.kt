package io.sj42.vpn.config

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

internal fun jsonArrayOf(vararg values: String): JsonArray = buildJsonArray {
    values.forEach { add(JsonPrimitive(it)) }
}

internal fun Map<String, List<String>>.firstValue(key: String): String? =
    get(key)?.firstOrNull()?.takeIf(String::isNotBlank)
