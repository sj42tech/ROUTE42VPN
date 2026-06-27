package io.github.sj42tech.route42.model

object RecommendedVpnApps {
    val packageNames: List<String> = listOf(
        "com.android.chrome",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.opera.browser",
        "com.duckduckgo.mobile.android",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.instagram.android",
        "com.instagram.lite",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.facebook.orca",
        "com.discord",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.linkedin.android",
        "org.thoughtcrime.securesms",
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.openai.chatgpt",
    )

    fun installedPackagesFrom(installedPackageNames: Iterable<String>): List<String> {
        val installed = installedPackageNames.toSet()
        return packageNames.filter { packageName -> packageName in installed }
    }
}
