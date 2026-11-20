package com.example.ui

import android.Manifest
import android.content.res.Configuration
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.text.layoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ThemeMode
import com.example.domain.CountryMetalsCatalog
import com.example.ui.components.LanguageAndThemeSheet
import com.example.ui.components.StatusPill
import com.example.ui.screens.CurrencyConverterScreen
import com.example.ui.screens.MetalsTrackerScreen
import com.example.ui.screens.UnitConverterScreen
import com.example.ui.theme.VertoTheme
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun VertoApp(viewModel: MainViewModel = viewModel()) {
  val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
  val systemDark = isSystemInDarkTheme()
  val useDarkTheme =
    when (prefs.themeMode) {
      ThemeMode.SYSTEM -> systemDark
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val baseContext = LocalContext.current
  val baseConfig = LocalConfiguration.current
  val targetLocale =
    remember(prefs.language, baseConfig) {
      if (prefs.language == AppLanguage.SYSTEM) {
        baseConfig.locales[0] ?: Locale.getDefault()
      } else {
        Locale.forLanguageTag(prefs.language.code)
      }
    }

  val localizedConfig =
    remember(baseConfig, targetLocale) {
      Configuration(baseConfig).apply {
        setLocale(targetLocale)
        setLayoutDirection(targetLocale)
      }
    }
  val localizedContext =
    remember(baseContext, localizedConfig, prefs.language) {
      if (prefs.language == AppLanguage.SYSTEM) baseContext
      else baseContext.createConfigurationContext(localizedConfig)
    }
  val layoutDir =
    if (targetLocale.layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl
    else LayoutDirection.Ltr

  CompositionLocalProvider(
    LocalContext provides localizedContext,
    LocalConfiguration provides localizedConfig,
    LocalLayoutDirection provides layoutDir,
  ) {
    VertoTheme(darkTheme = useDarkTheme) {
      VertoMainContent(
        viewModel = viewModel,
        isDarkTheme = useDarkTheme,
        displayLocale = targetLocale,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VertoMainContent(
  viewModel: MainViewModel,
  isDarkTheme: Boolean,
  displayLocale: Locale,
) {
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val unitState by viewModel.unitState.collectAsStateWithLifecycle()
  val currencyState by viewModel.currencyState.collectAsStateWithLifecycle()
  val metalsState by viewModel.metalsState.collectAsStateWithLifecycle()
  val ratesMap by viewModel.currencyRatesMap.collectAsStateWithLifecycle()
  val savedConversions by viewModel.savedConversions.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
  val prefsLoaded by viewModel.prefsLoaded.collectAsStateWithLifecycle()
  val isDetectingLocation by viewModel.isDetectingLocation.collectAsStateWithLifecycle()

  var showSettingsSheet by remember { mutableStateOf(false) }
  var explicitLocationRequest by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  val showSnackbar: (String) -> Unit = { msg ->
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(msg)
    }
  }

  val locationDeniedText = stringResource(R.string.location_denied_feedback)
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      val wasExplicit = explicitLocationRequest
      explicitLocationRequest = false
      viewModel.onLocationPermissionResult(granted = granted, forceApplyAll = wasExplicit)
      if (!granted && wasExplicit) {
        showSnackbar(locationDeniedText)
      }
    }

  LaunchedEffect(prefsLoaded) {
    if (!prefsLoaded) return@LaunchedEffect
    if (viewModel.hasCoarseLocationPermission()) {
      viewModel.detectAndApplyLocation(forceApplyAll = false)
    } else if (!prefs.hasPromptedLocation) {
      explicitLocationRequest = false
      viewModel.markLocationPrompted()
      locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
  }

  LaunchedEffect(displayLocale) {
    viewModel.detectedRegionEvent.collect { iso ->
      val name = CountryMetalsCatalog.find(iso).localizedName(displayLocale)
      showSnackbar(context.getString(R.string.location_detected_feedback, name))
    }
  }

  val tabs =
    listOf(
      Triple(AppTab.UNITS, R.string.tab_units, Icons.Rounded.Straighten to Icons.Outlined.Straighten),
      Triple(
        AppTab.CURRENCY,
        R.string.tab_currency,
        Icons.Rounded.CurrencyExchange to Icons.Outlined.CurrencyExchange,
      ),
      Triple(AppTab.METALS, R.string.tab_metals, Icons.Rounded.Diamond to Icons.Outlined.Diamond),
    )

  val topTitleRes =
    when (activeTab) {
      AppTab.UNITS -> R.string.unit_converter_title
      AppTab.CURRENCY -> R.string.currency_converter_title
      AppTab.METALS -> R.string.metals_title
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp
    val isWideLayout = maxWidth >= 720.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = stringResource(topTitleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (activeTab != AppTab.UNITS) {
                StatusPill(isLive = syncStatus.isLive, isSyncing = syncStatus.isSyncing)
              }
            }
          },
          actions = {
            if (activeTab != AppTab.UNITS) {
              IconButton(
                onClick = { viewModel.refreshAllRates() },
                modifier = Modifier.size(48.dp).testTag("refresh_rates_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = stringResource(R.string.action_refresh),
                )
              }
            }
            IconButton(
              onClick = {
                viewModel.setThemeMode(if (isDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
              },
              modifier = Modifier.size(48.dp).testTag("quick_theme_toggle"),
            ) {
              Icon(
                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = stringResource(R.string.action_theme),
              )
            }
            IconButton(
              onClick = { showSettingsSheet = true },
              modifier = Modifier.size(48.dp).testTag("language_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Translate,
                contentDescription = stringResource(R.string.action_language),
              )
            }
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            tabs.forEach { (tab, labelRes, icons) ->
              val selected = activeTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = if (selected) icons.first else icons.second,
                    contentDescription = stringResource(labelRes),
                  )
                },
                label = {
                  Text(
                    text = stringResource(labelRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  )
                },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  ),
                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            Spacer(Modifier.size(16.dp))
            tabs.forEach { (tab, labelRes, icons) ->
              val selected = activeTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = if (selected) icons.first else icons.second,
                    contentDescription = stringResource(labelRes),
                  )
                },
                label = {
                  Text(
                    text = stringResource(labelRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  ),
                modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
              )
            }
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(modifier = Modifier.widthIn(max = 1240.dp).fillMaxSize()) {
            Crossfade(targetState = activeTab, label = "tabCrossfade") { tab ->
              when (tab) {
                AppTab.UNITS ->
                  UnitConverterScreen(
                    state = unitState,
                    isWideLayout = isWideLayout,
                    onSelectCategory = viewModel::selectUnitCategory,
                    onSelectFromUnit = viewModel::setFromUnit,
                    onSelectToUnit = viewModel::setToUnit,
                    onSwap = viewModel::swapUnits,
                    onInputChange = viewModel::updateUnitInput,
                    onChartModeChange = viewModel::setUnitChartMode,
                    onShowMessage = showSnackbar,
                  )

                AppTab.CURRENCY ->
                  CurrencyConverterScreen(
                    state = currencyState,
                    ratesMap = ratesMap,
                    savedConversions = savedConversions,
                    syncStatus = syncStatus,
                    displayLocale = displayLocale,
                    isWideLayout = isWideLayout,
                    onInputChange = viewModel::setCurrencyInput,
                    onSelectFromCurrency = viewModel::setFromCurrency,
                    onSelectToCurrency = viewModel::setToCurrency,
                    onSwap = viewModel::swapCurrencies,
                    onSelectRange = viewModel::setCurrencyChartRange,
                    onSaveConversion = viewModel::saveCurrentCurrencyConversion,
                    onApplySaved = viewModel::applySavedConversion,
                    onDeleteSaved = viewModel::deleteSavedConversion,
                    onShowMessage = showSnackbar,
                  )

                AppTab.METALS ->
                  MetalsTrackerScreen(
                    state = metalsState,
                    ratesMap = ratesMap,
                    syncStatus = syncStatus,
                    displayLocale = displayLocale,
                    isWideLayout = isWideLayout,
                    onSelectMetal = viewModel::selectMetal,
                    onSelectCountry = viewModel::selectCountry,
                    onSelectRange = viewModel::setMetalChartRange,
                    onCalculatorGramsChange = viewModel::updateBullionGrams,
                    onCalculatorPurityChange = viewModel::setBullionPurity,
                  )
              }
            }
          }
        }
      }
    }
  }

  if (showSettingsSheet) {
    LanguageAndThemeSheet(
      selectedLanguage = prefs.language,
      selectedTheme = prefs.themeMode,
      selectedMeasurementLocale = prefs.measurementLocale,
      detectedCountryIso = prefs.detectedCountryIso,
      isDetectingLocation = isDetectingLocation,
      displayLocale = displayLocale,
      onDetectLocation = {
        if (viewModel.hasCoarseLocationPermission()) {
          viewModel.detectAndApplyLocation(forceApplyAll = true)
        } else {
          explicitLocationRequest = true
          locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
      },
      onSelectMeasurementLocale = viewModel::setMeasurementLocale,
      onSelectLanguage = viewModel::setLanguage,
      onSelectTheme = viewModel::setThemeMode,
      onDismiss = { showSettingsSheet = false },
    )
  }
}
