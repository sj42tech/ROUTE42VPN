package io.github.sj42tech.route42

internal object AppIds {
    fun actionConnect(packageName: String): String = "$packageName.action.CONNECT"

    fun actionDisconnect(packageName: String): String = "$packageName.action.DISCONNECT"
}
