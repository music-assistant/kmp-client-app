package io.music_assistant.client.player.sendspin.connection

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class MdnsAdvertiser actual constructor(
    private val serviceName: String,
    private val port: Int
) {
    private val logger = Logger.withTag("MdnsAdvertiser")

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    private val _incomingConnections = MutableSharedFlow<ServerConnectionRequest>(extraBufferCapacity = 10)
    actual val incomingConnections: Flow<ServerConnectionRequest> = _incomingConnections

    companion object {
        private const val SERVICE_TYPE = "_sendspin._tcp."
        private var context: Context? = null

        fun initialize(appContext: Context) {
            context = appContext.applicationContext
        }
    }

    actual suspend fun start() = suspendCoroutine { continuation ->
        val ctx = context ?: run {
            continuation.resumeWithException(IllegalStateException("MdnsAdvertiser not initialized. Call MdnsAdvertiser.initialize() first."))
            return@suspendCoroutine
        }

        logger.i { "Starting mDNS advertising: $serviceName on port $port" }

        nsdManager = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@MdnsAdvertiser.serviceName
            serviceType = SERVICE_TYPE
            port = this@MdnsAdvertiser.port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                logger.e { "Service registration failed: $errorCode" }
                isRegistered = false
                continuation.resumeWithException(
                    RuntimeException("mDNS registration failed with error code: $errorCode")
                )
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                logger.e { "Service unregistration failed: $errorCode" }
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                logger.i { "Service registered: ${serviceInfo?.serviceName}" }
                isRegistered = true
                continuation.resume(Unit)
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                logger.i { "Service unregistered: ${serviceInfo?.serviceName}" }
                isRegistered = false
            }
        }

        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            logger.e(e) { "Failed to register service" }
            continuation.resumeWithException(e)
        }
    }

    actual fun stop() {
        logger.i { "Stopping mDNS advertising" }

        if (isRegistered && registrationListener != null) {
            try {
                nsdManager?.unregisterService(registrationListener)
            } catch (e: Exception) {
                logger.e(e) { "Error unregistering service" }
            }
        }

        registrationListener = null
        nsdManager = null
        isRegistered = false
    }
}
