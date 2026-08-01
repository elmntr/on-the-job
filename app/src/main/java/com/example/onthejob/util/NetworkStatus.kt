package com.example.onthejob.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Cheap upfront connectivity check. Used to avoid attempting network calls
 * (Cloudinary upload, Worker AI-format call) that would otherwise be left to
 * time out — on some devices/network states a doomed connection attempt can
 * hang well past OkHttp's configured timeouts rather than failing fast, so
 * it's more reliable to just not attempt the call at all when there's no
 * active internet-capable network.
 *
 * Requires android.permission.ACCESS_NETWORK_STATE — add to AndroidManifest.xml
 * if not already present (commonly already granted transitively by other
 * libraries, but worth confirming).
 *
 * Checks NET_CAPABILITY_VALIDATED in addition to NET_CAPABILITY_INTERNET —
 * a network can be nominally "internet-capable" by type (e.g. cellular radio
 * still shows connected) while actually unreachable, such as when mobile data
 * is toggled off in Settings without enabling airplane mode. In that state,
 * carriers often silently drop packets rather than rejecting them, so a real
 * connection attempt can hang far longer than any configured client timeout
 * while the OS retries the TCP handshake. VALIDATED reflects whether Android
 * has actually confirmed real internet reachability, not just interface type.
 */
object NetworkStatus {
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}