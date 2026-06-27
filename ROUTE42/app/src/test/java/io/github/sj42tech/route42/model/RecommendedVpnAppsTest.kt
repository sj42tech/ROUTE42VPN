package io.github.sj42tech.route42.model

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class RecommendedVpnAppsTest {
    @Test
    fun `returns only installed recommended packages in preset order`() {
        val packages = RecommendedVpnApps.installedPackagesFrom(
            listOf(
                "ru.sberbankmobile",
                "org.telegram.messenger",
                "com.google.android.youtube",
                "ru.ozon.app.android",
                "com.brave.browser",
            ),
        )

        assertEquals(
            listOf(
                "com.brave.browser",
                "org.telegram.messenger",
                "com.google.android.youtube",
            ),
            packages,
        )
    }

    @Test
    fun `does not include common banking or marketplace packages`() {
        assertFalse("ru.sberbankmobile" in RecommendedVpnApps.packageNames)
        assertFalse("com.idamob.tinkoff.android" in RecommendedVpnApps.packageNames)
        assertFalse("ru.ozon.app.android" in RecommendedVpnApps.packageNames)
        assertFalse("ru.dns.shop.android" in RecommendedVpnApps.packageNames)
    }
}
