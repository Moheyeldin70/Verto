package com.example.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.text.layoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ai.VertoAiContextBuilder
import com.example.data.local.AppLanguage
import com.example.data.local.NavBarPosition
import com.example.data.local.ThemeMode
import com.example.domain.CountryMetalsCatalog
import com.example.domain.ShareExportHelper
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.ShareExportSheet
import com.example.ui.components.StatusPill
import com.example.ui.screens.CurrencyConverterScreen
import com.example.ui.screens.MetalsTrackerScreen
import com.example.ui.screens.UnitConverterScreen
import com.example.ui.theme.LauncherSurface
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.VertoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private class LocalizedActivityContextWrapper(
  base: Context,
  private val localizedResources: Resources,
) : ContextWrapper(base) {
  override fun getResources(): Resources = localizedResources
}

@Composable
fun VertoApp(viewModel: MainViewModel = viewModel()) {
  val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
  val prefsLoaded by viewModel.prefsLoaded.collectAsStateWithLifecycle()
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
        if (!baseConfig.locales.isEmpty) {
          baseConfig.locales[0] ?: Locale.getDefault()
        } else {
          Locale.getDefault()
        }
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
      if (prefs.language == AppLanguage.SYSTEM) {
        baseContext
      } else {
        val localizedRes = baseContext.createConfigurationContext(localizedConfig).resources
        LocalizedActivityContextWrapper(baseContext, localizedRes)
      }
    }
  val layoutDir =
    if (targetLocale.layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl
    else LayoutDirection.Ltr

  var showLaunchSplash by remember { mutableStateOf(true) }
  LaunchedEffect(Unit) {
    delay(920L)
    showLaunchSplash = false
  }

  CompositionLocalProvider(
    LocalContext provides localizedContext,
    LocalConfiguration provides localizedConfig,
    LocalLayoutDirection provides layoutDir,
  ) {
    VertoTheme(darkTheme = useDarkTheme) {
      Box(modifier = Modifier.fillMaxSize()) {
        VertoMainContent(
          viewModel = viewModel,
          isDarkTheme = useDarkTheme,
          displayLocale = targetLocale,
          layoutDir = layoutDir,
          prefsLoaded = prefsLoaded,
        )

        AnimatedVisibility(
          visible = showLaunchSplash,
          exit =
            fadeOut(animationSpec = tween(280)) +
              scaleOut(targetScale = 1.06f, animationSpec = tween(280)),
        ) {
          VertoLaunchOverlay()
        }
      }
    }
  }
}

@Composable
private fun VertoLaunchOverlay() {
  val financial = LocalFinancialColors.current
  val iconScale = remember { Animatable(0.72f) }
  val iconOffsetDp = remember { Animatable(24f) }
  val iconAlpha = remember { Animatable(0f) }
  val glowScale = remember { Animatable(0.6f) }
  val textAlpha = remember { Animatable(0f) }
  val textOffsetDp = remember { Animatable(14f) }

  LaunchedEffect(Unit) {
    launch {
      iconAlpha.animateTo(targetValue = 1f, animationSpec = tween(260))
    }
    launch {
      iconScale.animateTo(
        targetValue = 1f,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
          ),
      )
    }
    launch {
      iconOffsetDp.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
      )
    }
    launch {
      glowScale.animateTo(
        targetValue = 1.15f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
      )
    }
    launch {
      delay(160L)
      launch { textAlpha.animateTo(targetValue = 1f, animationSpec = tween(360)) }
      launch {
        textOffsetDp.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        )
      }
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = LauncherSurface,
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                  financial.gold.copy(alpha = 0.08f),
                  Color.Transparent,
                ),
            ),
          ),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier
            .size(180.dp)
            .scale(glowScale.value)
            .background(
              Brush.radialGradient(
                colors =
                  listOf(
                    financial.gold.copy(alpha = 0.20f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    Color.Transparent,
                  ),
              ),
            ),
      )

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = RoundedCornerShape(30.dp),
          color = LauncherSurface,
          border =
            BorderStroke(
              width = 1.5.dp,
              brush =
                Brush.linearGradient(
                  colors =
                    listOf(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                      financial.gold.copy(alpha = 0.85f),
                    ),
                ),
            ),
          shadowElevation = 18.dp,
          modifier =
            Modifier
              .size(108.dp)
              .offset { IntOffset(0, iconOffsetDp.value.dp.roundToPx()) }
              .scale(iconScale.value)
              .alpha(iconAlpha.value),
        ) {
          Image(
            painter = painterResource(R.drawable.ic_launcher_art),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().scale(1.12f),
          )
        }
        Spacer(Modifier.height(20.dp))
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          modifier =
            Modifier
              .offset { IntOffset(0, textOffsetDp.value.dp.roundToPx()) }
              .alpha(textAlpha.value),
        )
        Spacer(Modifier.height(10.dp))
        Box(
          modifier =
            Modifier
              .width(52.dp)
              .height(3.dp)
              .alpha(textAlpha.value)
              .clip(RoundedCornerShape(999.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(MaterialTheme.colorScheme.primary, financial.gold),
                ),
              ),
        )
      }
    }
  }
}

private data class NavTabItem(
  val tab: AppTab,
  val labelRes: Int,
  val activeIcon: ImageVector,
  val inactiveIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VertoMainContent(
  viewModel: MainViewModel,
  isDarkTheme: Boolean,
  displayLocale: Locale,
  layoutDir: LayoutDirection,
  prefsLoaded: Boolean,
) {
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val unitState by viewModel.unitState.collectAsStateWithLifecycle()
  val currencyState by viewModel.currencyState.collectAsStateWithLifecycle()
  val metalsState by viewModel.metalsState.collectAsStateWithLifecycle()
  val ratesMap by viewModel.currencyRatesMap.collectAsStateWithLifecycle()
  val savedConversions by viewModel.savedConversions.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
  val isDetectingLocation by viewModel.isDetectingLocation.collectAsStateWithLifecycle()
  val aiState by viewModel.aiState.collectAsStateWithLifecycle()

  var showSettingsSheet by remember { mutableStateOf(false) }
  var showShareSheet by remember { mutableStateOf(false) }
  var showAiSheet by remember { mutableStateOf(false) }
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
      NavTabItem(AppTab.UNITS, R.string.tab_units, Icons.Rounded.Straighten, Icons.Outlined.Straighten),
      NavTabItem(
        AppTab.CURRENCY,
        R.string.tab_currency,
        Icons.Rounded.CurrencyExchange,
        Icons.Outlined.CurrencyExchange,
      ),
      NavTabItem(AppTab.METALS, R.string.tab_metals, Icons.Rounded.Diamond, Icons.Outlined.Diamond),
    )

  val topTitleRes =
    when (activeTab) {
      AppTab.UNITS -> R.string.unit_converter_title
      AppTab.CURRENCY -> R.string.currency_converter_title
      AppTab.METALS -> R.string.metals_title
    }

  val navPos = prefs.navBarPosition

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
              onClick = { showShareSheet = true },
              modifier = Modifier.size(48.dp).testTag("share_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.action_share),
              )
            }
            IconButton(
              onClick = { showSettingsSheet = true },
              modifier = Modifier.size(48.dp).testTag("settings_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.action_settings),
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
      floatingActionButton = {
        ExtendedFloatingActionButton(
          onClick = { showAiSheet = true },
          icon = {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
            )
          },
          text = {
            Text(
              text = stringResource(R.string.ai_fab_label),
              fontWeight = FontWeight.SemiBold,
            )
          },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.testTag("ai_assistant_button"),
        )
      },
      bottomBar = {
        if (navPos == NavBarPosition.BOTTOM) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            tabs.forEach { item ->
              val selected = activeTab == item.tab
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(item.tab) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                    contentDescription = stringResource(item.labelRes),
                  )
                },
                label = {
                  Text(
                    text = stringResource(item.labelRes),
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
                modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      ) {
        if (navPos == NavBarPosition.TOP) {
          VertoTopNavBar(
            tabs = tabs,
            activeTab = activeTab,
            onSelectTab = viewModel::selectTab,
          )
        }

        val isRail = navPos == NavBarPosition.LEFT || navPos == NavBarPosition.RIGHT
        val railBeforeInRow =
          (navPos == NavBarPosition.LEFT) xor (layoutDir == LayoutDirection.Rtl)

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
          if (isRail && railBeforeInRow) {
            VertoSideRail(
              tabs = tabs,
              activeTab = activeTab,
              onSelectTab = viewModel::selectTab,
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                      onSelectChartUnit = viewModel::setMetalChartUnit,
                      onSelectChartPurity = viewModel::setMetalChartPurity,
                      onCalculatorGramsChange = viewModel::updateBullionGrams,
                      onCalculatorPurityChange = viewModel::setBullionPurity,
                    )
                }
              }
            }
          }

          if (isRail && !railBeforeInRow) {
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VertoSideRail(
              tabs = tabs,
              activeTab = activeTab,
              onSelectTab = viewModel::selectTab,
            )
          }
        }
      }
    }
  }

  if (showSettingsSheet) {
    SettingsSheet(
      selectedLanguage = prefs.language,
      selectedTheme = prefs.themeMode,
      selectedNavBarPosition = prefs.navBarPosition,
      selectedStartScreen = prefs.startScreen,
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
      onSelectNavBarPosition = viewModel::setNavBarPosition,
      onSelectStartScreen = viewModel::setStartScreen,
      onSelectLanguage = viewModel::setLanguage,
      onSelectTheme = viewModel::setThemeMode,
      onDismiss = { showSettingsSheet = false },
    )
  }

  if (showShareSheet) {
    val categoryDisplayTitle = stringResource(unitState.category.labelRes)
    val tabDisplayTitle = stringResource(topTitleRes)
    val chartDisplayTitle =
      when (activeTab) {
        AppTab.CURRENCY -> stringResource(R.string.historical_chart_title)
        AppTab.METALS -> stringResource(R.string.metal_chart_title)
        AppTab.UNITS -> ""
      }
    val liveLabel = stringResource(R.string.status_live)
    val offlineLabel = stringResource(R.string.status_cached)

    val sharePayload =
      remember(
        activeTab,
        unitState,
        currencyState,
        metalsState,
        ratesMap,
        syncStatus,
        displayLocale,
        isDarkTheme,
        categoryDisplayTitle,
        tabDisplayTitle,
        chartDisplayTitle,
        liveLabel,
        offlineLabel,
      ) {
        ShareExportHelper.buildPayload(
          activeTab = activeTab,
          unitState = unitState,
          currencyState = currencyState,
          metalsState = metalsState,
          ratesMap = ratesMap,
          syncStatus = syncStatus,
          displayLocale = displayLocale,
          isDarkTheme = isDarkTheme,
          categoryDisplayTitle = categoryDisplayTitle,
          tabTitle = tabDisplayTitle,
          chartDisplayTitle = chartDisplayTitle,
          liveLabel = liveLabel,
          offlineLabel = offlineLabel,
        )
      }

    ShareExportSheet(
      payload = sharePayload,
      onShowMessage = showSnackbar,
      onDismiss = { showShareSheet = false },
    )
  }

  if (showAiSheet) {
    val contextLabel =
      remember(activeTab, unitState, currencyState, metalsState, displayLocale) {
        VertoAiContextBuilder.buildShortContextLabel(
          activeTab = activeTab,
          unitState = unitState,
          currencyState = currencyState,
          metalsState = metalsState,
          displayLocale = displayLocale,
        )
      }
    AiAssistantSheet(
      activeTab = activeTab,
      contextLabel = contextLabel,
      isNetworkAvailable = syncStatus.isNetworkAvailable,
      aiState = aiState,
      onSendMessage = { prompt -> viewModel.sendAiMessage(prompt, displayLocale) },
      onClearChat = viewModel::clearAiChat,
      onDismissError = viewModel::dismissAiError,
      onDismiss = { showAiSheet = false },
    )
  }
}

@Composable
private fun VertoTopNavBar(
  tabs: List<NavTabItem>,
  activeTab: AppTab,
  onSelectTab: (AppTab) -> Unit,
) {
  Surface(color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        tabs.forEach { item ->
          val selected = activeTab == item.tab
          Surface(
            onClick = { onSelectTab(item.tab) },
            shape = RoundedCornerShape(12.dp),
            color =
              if (selected) MaterialTheme.colorScheme.primaryContainer
              else Color.Transparent,
            modifier =
              Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("top_nav_${item.tab.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                contentDescription = stringResource(item.labelRes),
                tint =
                  if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color =
                  if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
  }
}

@Composable
private fun VertoSideRail(
  tabs: List<NavTabItem>,
  activeTab: AppTab,
  onSelectTab: (AppTab) -> Unit,
) {
  NavigationRail(
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxHeight(),
  ) {
    Spacer(Modifier.weight(1f))
    tabs.forEachIndexed { index, item ->
      if (index > 0) Spacer(Modifier.height(12.dp))
      val selected = activeTab == item.tab
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectTab(item.tab) },
        icon = {
          Icon(
            imageVector = if (selected) item.activeIcon else item.inactiveIcon,
            contentDescription = stringResource(item.labelRes),
          )
        },
        label = {
          Text(
            text = stringResource(item.labelRes),
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
        modifier = Modifier.testTag("rail_${item.tab.name.lowercase()}"),
      )
    }
    Spacer(Modifier.weight(1f))
  }
}
