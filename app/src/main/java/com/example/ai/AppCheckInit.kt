package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

@Volatile
private var appCheckInstalled = false

fun ensureFirebaseAppInitialized(context: Context): Boolean {
  val appContext = context.applicationContext ?: context
  if (FirebaseApp.getApps(appContext).isNotEmpty()) {
    return true
  }

  // 1. Standard resource-based initialization (google-services plugin generated strings)
  val initialized = FirebaseApp.initializeApp(appContext)
  if (initialized != null || FirebaseApp.getApps(appContext).isNotEmpty()) {
    return true
  }

  // 2. Fallback: explicitly build FirebaseOptions from package resources in case StringResourceValueReader missed package lookup
  val options = buildFirebaseOptionsFromPackageResources(appContext) ?: return false
  return try {
    FirebaseApp.initializeApp(appContext, options)
    FirebaseApp.getApps(appContext).isNotEmpty()
  } catch (e: Exception) {
    Log.e(TAG, "Explicit FirebaseApp initialization failed", e)
    FirebaseApp.getApps(appContext).isNotEmpty()
  }
}

fun hasFirebaseBuildConfigResources(context: Context): Boolean {
  val appContext = context.applicationContext ?: context
  val appIdRes = appContext.resources.getIdentifier("google_app_id", "string", appContext.packageName)
  val apiKeyRes = appContext.resources.getIdentifier("google_api_key", "string", appContext.packageName)
  return appIdRes != 0 && apiKeyRes != 0
}

private fun buildFirebaseOptionsFromPackageResources(context: Context): FirebaseOptions? {
  fun readString(name: String): String? {
    val resId = context.resources.getIdentifier(name, "string", context.packageName)
    if (resId == 0) return null
    return runCatching { context.getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
  }

  val appId = readString("google_app_id") ?: return null
  val apiKey = readString("google_api_key") ?: return null

  return FirebaseOptions.Builder()
    .setApplicationId(appId)
    .setApiKey(apiKey)
    .apply {
      readString("project_id")?.let { setProjectId(it) }
      readString("google_storage_bucket")?.let { setStorageBucket(it) }
      readString("gcm_defaultSenderId")?.let { setGcmSenderId(it) }
    }
    .build()
}

fun configureAppCheck(context: Context, intent: Intent?) {
  val appContext = context.applicationContext ?: context
  try {
    if (!ensureFirebaseAppInitialized(appContext)) {
      Log.e(
        TAG,
        "FirebaseApp could not be initialized. Check that app/google-services.json was included in the build.",
      )
      return
    }

    if (BuildConfig.DEBUG) {
      val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

      if (!debugToken.isNullOrEmpty()) {
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs =
          appContext.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE,
          )
        // Use synchronous commit() so the debug token is persisted before DebugAppCheckProviderFactory reads it.
        prefs
          .edit()
          .putString(DEBUG_SECRET_KEY, debugToken)
          .commit()
        Log.d(TAG, "App Check debug secret configured successfully.")
      } else {
        Log.w(TAG, "No debug token found in Intent extra.")
      }

      if (!appCheckInstalled) {
        FirebaseAppCheck.getInstance()
          .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        appCheckInstalled = true
        Log.d(TAG, "DebugAppCheckProviderFactory installed.")
      }
    } else if (!appCheckInstalled) {
      val resId =
        appContext.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", appContext.packageName)
      if (resId != 0) {
        val siteKey = appContext.getString(resId)
        if (siteKey.isNotBlank()) {
          FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(RecaptchaAppCheckProviderFactory.getInstance(siteKey))
          appCheckInstalled = true
          Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
        } else {
          Log.e(TAG, "recaptcha_site_key resource string is blank.")
        }
      } else {
        Log.e(TAG, "recaptcha_site_key string resource not found in package.")
      }
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize Firebase App Check", e)
  }
}
