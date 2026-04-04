package io.github.sj42tech.route42.tunnel

import android.net.DnsResolver
import android.net.Network
import android.os.Build
import android.os.CancellationSignal
import android.system.ErrnoException
import io.nekohasekai.libbox.ExchangeContext
import io.nekohasekai.libbox.Func
import io.nekohasekai.libbox.LocalDNSTransport
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AndroidLocalDnsTransport(
    private val networkProvider: () -> Network,
) : LocalDNSTransport {
    private companion object {
        const val RCODE_NXDOMAIN = 3
    }

    override fun raw(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun exchange(ctx: ExchangeContext, message: ByteArray) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Raw DNS is only supported on Android 10+"
        }
        runBlocking {
            val network = networkProvider()
            suspendCoroutine { continuation ->
                val signal = CancellationSignal()
                ctx.onCancel(Func { signal.cancel() })
                DnsResolver.getInstance().rawQuery(
                    network,
                    message,
                    DnsResolver.FLAG_NO_RETRY,
                    Dispatchers.IO.asExecutor(),
                    signal,
                    object : DnsResolver.Callback<ByteArray> {
                        override fun onAnswer(answer: ByteArray, rcode: Int) {
                            if (rcode == 0) {
                                ctx.rawSuccess(answer)
                            } else {
                                ctx.errorCode(rcode)
                            }
                            continuation.resumeSafely()
                        }

                        override fun onError(error: DnsResolver.DnsException) {
                            when (val cause = error.cause) {
                                is ErrnoException -> {
                                    ctx.errnoCode(cause.errno)
                                    continuation.resumeSafely()
                                    return
                                }
                            }
                            continuation.resumeSafelyWithException(error)
                        }
                    },
                )
            }
        }
    }

    override fun lookup(ctx: ExchangeContext, network: String, domain: String) {
        runBlocking {
            val activeNetwork = networkProvider()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                suspendCoroutine { continuation ->
                    val signal = CancellationSignal()
                    ctx.onCancel(Func { signal.cancel() })
                    val callback = object : DnsResolver.Callback<Collection<InetAddress>> {
                        override fun onAnswer(answer: Collection<InetAddress>, rcode: Int) {
                            if (rcode == 0) {
                                ctx.success(answer.mapNotNull(InetAddress::getHostAddress).joinToString("\n"))
                            } else {
                                ctx.errorCode(rcode)
                            }
                            continuation.resumeSafely()
                        }

                        override fun onError(error: DnsResolver.DnsException) {
                            when (val cause = error.cause) {
                                is ErrnoException -> {
                                    ctx.errnoCode(cause.errno)
                                    continuation.resumeSafely()
                                    return
                                }
                            }
                            continuation.resumeSafelyWithException(error)
                        }
                    }
                    val queryType = when {
                        network.endsWith("4") -> DnsResolver.TYPE_A
                        network.endsWith("6") -> DnsResolver.TYPE_AAAA
                        else -> null
                    }
                    if (queryType == null) {
                        DnsResolver.getInstance().query(
                            activeNetwork,
                            domain,
                            DnsResolver.FLAG_NO_RETRY,
                            Dispatchers.IO.asExecutor(),
                            signal,
                            callback,
                        )
                    } else {
                        DnsResolver.getInstance().query(
                            activeNetwork,
                            domain,
                            queryType,
                            DnsResolver.FLAG_NO_RETRY,
                            Dispatchers.IO.asExecutor(),
                            signal,
                            callback,
                        )
                    }
                }
            } else {
                val answer = try {
                    activeNetwork.getAllByName(domain)
                } catch (_: UnknownHostException) {
                    ctx.errorCode(RCODE_NXDOMAIN)
                    return@runBlocking
                }
                ctx.success(answer.mapNotNull(InetAddress::getHostAddress).joinToString("\n"))
            }
        }
    }
}

private fun Continuation<Unit>.resumeSafely() {
    runCatching { resume(Unit) }
}

private fun Continuation<Unit>.resumeSafelyWithException(error: Throwable) {
    runCatching { resumeWithException(error) }
}
