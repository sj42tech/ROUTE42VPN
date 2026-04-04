package io.github.sj42tech.route42.tunnel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileHealthCheckTest {
    @Test
    fun `returns ready when server is reachable but tunnel is not running`() {
        val grade = deriveProfileHealthGrade(
            tcpProbe = ProbeOutcome(reachable = true, latencyMs = 180),
            tlsProbe = ProbeOutcome(reachable = true, latencyMs = 320),
            tunnelState = TunnelState(),
            profileId = "profile-1",
        )

        assertEquals(ProfileHealthGrade.READY, grade)
        assertEquals(
            "Profile is ready to connect",
            summarizeProfileHealth(
                tcpProbe = ProbeOutcome(reachable = true, latencyMs = 180),
                tlsProbe = ProbeOutcome(reachable = true, latencyMs = 320),
                tunnelState = TunnelState(),
                profileId = "profile-1",
            ),
        )
    }

    @Test
    fun `returns broken when tcp probe fails`() {
        val grade = deriveProfileHealthGrade(
            tcpProbe = ProbeOutcome(reachable = false, errorMessage = "timeout"),
            tlsProbe = null,
            tunnelState = TunnelState(),
            profileId = "profile-1",
        )

        assertEquals(ProfileHealthGrade.BROKEN, grade)
        assertEquals(
            "Server is not reachable",
            summarizeProfileHealth(
                tcpProbe = ProbeOutcome(reachable = false, errorMessage = "timeout"),
                tlsProbe = null,
                tunnelState = TunnelState(),
                profileId = "profile-1",
            ),
        )
    }

    @Test
    fun `returns healthy when tunnel exit ip is detected`() {
        val grade = deriveProfileHealthGrade(
            tcpProbe = ProbeOutcome(reachable = true, latencyMs = 210),
            tlsProbe = ProbeOutcome(reachable = true, latencyMs = 450),
            tunnelState = TunnelState(
                status = TunnelStatus.RUNNING,
                profileId = "profile-1",
                publicIp = "203.0.113.10",
                directPublicIp = "198.51.100.10",
            ),
            profileId = "profile-1",
        )

        assertEquals(ProfileHealthGrade.HEALTHY, grade)
        assertEquals(
            TunnelExitStatus.DETECTED,
            deriveTunnelExitStatus(
                tunnelState = TunnelState(
                    status = TunnelStatus.RUNNING,
                    profileId = "profile-1",
                    publicIp = "203.0.113.10",
                    directPublicIp = "198.51.100.10",
                ),
                profileId = "profile-1",
            ),
        )
    }

    @Test
    fun `returns degraded when tunnel exit matches direct ip`() {
        val grade = deriveProfileHealthGrade(
            tcpProbe = ProbeOutcome(reachable = true, latencyMs = 200),
            tlsProbe = ProbeOutcome(reachable = true, latencyMs = 300),
            tunnelState = TunnelState(
                status = TunnelStatus.RUNNING,
                profileId = "profile-1",
                publicIp = "198.51.100.10",
                directPublicIp = "198.51.100.10",
            ),
            profileId = "profile-1",
        )

        assertEquals(ProfileHealthGrade.DEGRADED, grade)
        assertTrue(
            summarizeProfileHealth(
                tcpProbe = ProbeOutcome(reachable = true, latencyMs = 200),
                tlsProbe = ProbeOutcome(reachable = true, latencyMs = 300),
                tunnelState = TunnelState(
                    status = TunnelStatus.RUNNING,
                    profileId = "profile-1",
                    publicIp = "198.51.100.10",
                    directPublicIp = "198.51.100.10",
                ),
                profileId = "profile-1",
            ).contains("suspicious", ignoreCase = true),
        )
    }
}
