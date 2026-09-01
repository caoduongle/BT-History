package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val address: String?
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationResult = withContext(Dispatchers.IO) {
        if (!BluetoothHelper.hasLocationPermission(context)) {
            return@withContext LocationResult(null, null, null, null)
        }

        var location: Location? = null

        // Try FusedLocationProviderClient first with 4-second timeout
        try {
            val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            location = withTimeoutOrNull(4000L) {
                suspendCancellableCoroutine { continuation ->
                    val cts = CancellationTokenSource()
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            continuation.resume(loc)
                        }
                        .addOnFailureListener {
                            continuation.resume(null)
                        }
                    continuation.invokeOnCancellation {
                        cts.cancel()
                    }
                }
            }

            if (location == null) {
                // Try last known location
                location = suspendCancellableCoroutine { continuation ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> continuation.resume(loc) }
                        .addOnFailureListener { continuation.resume(null) }
                }
            }
        } catch (e: Exception) {
            // Fallback to LocationManager below
        }

        // Fallback to system LocationManager
        if (location == null) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val passiveLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                    location = listOfNotNull(gpsLoc, netLoc, passiveLoc).maxByOrNull { it.time }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (location != null) {
            val address = reverseGeocode(context, location.latitude, location.longitude)
            LocationResult(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                address = address
            )
        } else {
            LocationResult(null, null, null, null)
        }
    }

    private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("vi", "VN"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    formatAddress(addresses?.firstOrNull())
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    formatAddress(addresses?.firstOrNull())
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatAddress(address: Address?): String? {
        if (address == null) return null
        val parts = mutableListOf<String>()
        address.featureName?.let { if (it.isNotBlank()) parts.add(it) }
        address.thoroughfare?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.subLocality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.locality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.adminArea?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0)
        }
    }

    fun openLocationInMap(context: Context, latitude: Double?, longitude: Double?, label: String? = null) {
        if (latitude == null || longitude == null) {
            Toast.makeText(context, context.getString(com.example.R.string.toast_no_coordinates), Toast.LENGTH_SHORT).show()
            return
        }

        val resolvedLabel = label ?: context.getString(com.example.R.string.cd_location)

        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(resolvedLabel)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to web browser Google Maps
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, context.getString(com.example.R.string.toast_cannot_open_map), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
