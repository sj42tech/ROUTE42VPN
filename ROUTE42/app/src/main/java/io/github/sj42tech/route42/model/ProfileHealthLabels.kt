package io.github.sj42tech.route42.model

import io.github.sj42tech.route42.tunnel.ProfileHealthGrade
import io.github.sj42tech.route42.tunnel.TunnelExitStatus

fun ProfileHealthGrade.label(): String = when (this) {
    ProfileHealthGrade.READY -> "Ready"
    ProfileHealthGrade.HEALTHY -> "Healthy"
    ProfileHealthGrade.DEGRADED -> "Degraded"
    ProfileHealthGrade.BROKEN -> "Broken"
}

fun TunnelExitStatus.label(): String = when (this) {
    TunnelExitStatus.NOT_CHECKED -> "Not checked"
    TunnelExitStatus.DETECTED -> "Exit IP detected"
    TunnelExitStatus.UNAVAILABLE -> "Exit IP unavailable"
    TunnelExitStatus.MATCHES_DIRECT -> "Matches direct IP"
}
