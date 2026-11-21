package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import com.example.data.local.CachedChartPointEntity
import com.example.data.repository.SyncStatus
import com.example.domain.ShareExportHelper
import com.example.domain.ShareExportMode
import com.example.ui.AppTab
import com.example.ui.CurrencyConverterState
import com.example.ui.MetalsTrackerState
import com.example.ui.UnitConverterState
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
      assertTrue(localizedRes.getString(R.string.share_sheet_title).isNotBlank())
      assertTrue(localizedRes.getString(R.string.ai_sheet_title).isNotBlank())
      assertTrue(localizedRes.getString(R.string.ai_prompt_fx_predict).isNotBlank())
    }
  }

  @Test
  fun `shareExportHelper renders valid bitmap for summary and chart`() {
    val points =
      listOf(
        CachedChartPointEntity("FX_USD_EUR_M1_0", "FX_USD_EUR_M1", 0, 1_700_000_000_000L, 0.91, "Oct 21"),
        CachedChartPointEntity("FX_USD_EUR_M1_1", "FX_USD_EUR_M1", 1, 1_700_432_000_000L, 0.93, "Nov 05"),
        CachedChartPointEntity("FX_USD_EUR_M1_2", "FX_USD_EUR_M1", 2, 1_700_864_000_000L, 0.94, "Nov 21"),
      )
    val payload =
      ShareExportHelper.buildPayload(
        activeTab = AppTab.CURRENCY,
        unitState = UnitConverterState(),
        currencyState = CurrencyConverterState(fromCode = "USD", toCode = "EUR", inputText = "250", chartPoints = points),
        metalsState = MetalsTrackerState(),
        ratesMap = mapOf("USD" to 1.0, "EUR" to 0.94),
        syncStatus = SyncStatus(isLive = true, isSyncing = false, isNetworkAvailable = true, lastUpdatedMillis = 1_700_864_000_000L),
        displayLocale = Locale.US,
        isDarkTheme = true,
        categoryDisplayTitle = "",
        tabTitle = "Global Currency",
        chartDisplayTitle = "Historical Rate Trend",
        liveLabel = "Live",
        offlineLabel = "Offline",
      )

    val bitmap = ShareExportHelper.renderExportBitmap(payload, ShareExportMode.CHART)
    assertEquals(1080, bitmap.width)
    assertTrue(bitmap.height >= 900)
  }
}
