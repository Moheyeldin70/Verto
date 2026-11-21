package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Verto", appName)
  }

  @Test
  fun `switching across all supported languages loads resources safely`() {
    val base = ApplicationProvider.getApplicationContext<Context>()
    AppLanguage.entries.filter { it != AppLanguage.SYSTEM }.forEach { lang ->
      val locale = Locale.forLanguageTag(lang.code)
      val cfg =
        Configuration(base.resources.configuration).apply {
          setLocale(locale)
          setLayoutDirection(locale)
        }
      val localizedRes = base.createConfigurationContext(cfg).resources
      assertTrue(localizedRes.getString(R.string.action_settings).isNotBlank())
      assertTrue(localizedRes.getString(R.string.location_detected_feedback, "Egypt").isNotBlank())
      assertTrue(localizedRes.getString(R.string.range_5y).isNotBlank())
      assertTrue(localizedRes.getString(R.string.graph_unit_gold_coin).isNotBlank())
    }
  }
}
