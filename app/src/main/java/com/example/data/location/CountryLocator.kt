package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class DetectedRegion(
  val countryIso: String,
  val fromDeviceLocation: Boolean,
)

class CountryLocator(private val context: Context) {
  fun hasCoarseLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
      ) == PackageManager.PERMISSION_GRANTED
  }

  suspend fun detectCountry(): DetectedRegion =
    withContext(Dispatchers.IO) {
      if (hasCoarseLocationPermission()) {
        val location = getBestAvailableLocation()
        if (location != null) {
          val geoCountry = reverseGeocodeCountry(location.latitude, location.longitude)
          if (!geoCountry.isNullOrBlank()) {
            return@withContext DetectedRegion(geoCountry.uppercase(Locale.US), fromDeviceLocation = true)
          }
        }
      }
      val fallbackIso = fallbackCountryIso()
      DetectedRegion(fallbackIso, fromDeviceLocation = false)
    }

  @SuppressLint("MissingPermission")
  private suspend fun getBestAvailableLocation(): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = lm.getProviders(true)
    val lastKnown =
      providers
        .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }

    // If recent last-known fix (< 1 hour) exists, return immediately
    if (lastKnown != null && (System.currentTimeMillis() - lastKnown.time) < 3_600_000L) {
      return lastKnown
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val preferredProvider =
        when {
          lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
          lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
          else -> providers.firstOrNull()
        }
      if (preferredProvider != null) {
        val fresh =
          withTimeoutOrNull(4_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
              val signal = CancellationSignal()
              cont.invokeOnCancellation { signal.cancel() }
              runCatching {
                lm.getCurrentLocation(
                  preferredProvider,
                  signal,
                  ContextCompat.getMainExecutor(context),
                ) { loc ->
                  if (cont.isActive) cont.resume(loc)
                }
              }.onFailure {
                if (cont.isActive) cont.resume(null)
              }
            }
          }
        if (fresh != null) return fresh
      }
    }

    return lastKnown
  }

  @Suppress("DEPRECATION")
  private suspend fun reverseGeocodeCountry(lat: Double, lon: Double): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.US)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      withTimeoutOrNull(3_000L) {
        suspendCancellableCoroutine<String?> { cont ->
          runCatching {
            geocoder.getFromLocation(lat, lon, 1) { addresses ->
              val code = addresses.firstOrNull()?.countryCode
              if (cont.isActive) cont.resume(code)
            }
          }.onFailure {
            if (cont.isActive) cont.resume(null)
          }
        }
      }
    } else {
      runCatching { geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()?.countryCode }.getOrNull()
    }
  }

  private fun fallbackCountryIso(): String {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val networkCountry = tm?.networkCountryIso?.takeIf { it.length == 2 }
    val simCountry = tm?.simCountryIso?.takeIf { it.length == 2 }
    val localeCountry = Locale.getDefault().country.takeIf { it.length == 2 }
    return (networkCountry ?: simCountry ?: localeCountry ?: "US").uppercase(Locale.US)
  }
}
