package app.voyara.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.voyara.AppViewModel
import app.voyara.Geo
import app.voyara.Live
import app.voyara.MapLayer
import app.voyara.Mode
import app.voyara.Path
import app.voyara.Place
import app.voyara.Plan
import app.voyara.Sim
import app.voyara.ThemeMode
import app.voyara.distance
import app.voyara.formatDistance
import app.voyara.formatDuration
import app.voyara.hasLocationPermission
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PERMISSIONS = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private data class Speed(val label: String, val kmh: Double, val icon: ImageVector)

private val speeds = listOf(
    Speed("Walk", 5.0, Icons.AutoMirrored.Rounded.DirectionsWalk),
    Speed("Run", 10.0, Icons.AutoMirrored.Rounded.DirectionsRun),
    Speed("Cycle", 18.0, Icons.AutoMirrored.Rounded.DirectionsBike),
    Speed("Drive", 50.0, Icons.Rounded.DirectionsCar),
)

private val radii = listOf(50.0, 100.0, 250.0, 1000.0)

/** Inline inset of the sheet's content. */
private val Inset = 16.dp

/** Height of the sheet's drag handle; the whole strip is the tap/drag target. */
private val HandleHeight = 32.dp

private val MapLayer.label
    get() = when (this) {
        MapLayer.Mono -> "Monochrome"
        MapLayer.Street -> "Street"
        MapLayer.Satellite -> "Satellite"
        MapLayer.Terrain -> "Terrain"
    }

private val MapLayer.icon
    get() = when (this) {
        MapLayer.Mono -> Icons.Rounded.Contrast
        MapLayer.Street -> Icons.Rounded.Map
        MapLayer.Satellite -> Icons.Rounded.SatelliteAlt
        MapLayer.Terrain -> Icons.Rounded.Terrain
    }

private val ThemeMode.label
    get() = when (this) {
        ThemeMode.System -> "Match system"
        ThemeMode.Light -> "Light"
        ThemeMode.Dark -> "Dark"
    }

private val ThemeMode.icon
    get() = when (this) {
        ThemeMode.System -> Icons.Rounded.BrightnessAuto
        ThemeMode.Light -> Icons.Rounded.LightMode
        ThemeMode.Dark -> Icons.Rounded.DarkMode
    }

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Voyara", text))
}

/** Checks for pasteable text without reading it, so Android doesn't show its "pasted" notice. */
private fun clipboardHasText(context: Context): Boolean =
    context.getSystemService(ClipboardManager::class.java).primaryClipDescription
        ?.let { it.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || it.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) } == true

private fun readClipboard(context: Context): String? =
    context.getSystemService(ClipboardManager::class.java).primaryClip
        ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, dark: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val live by Sim.live.collectAsStateWithLifecycle()
    val plan by Sim.plan.collectAsStateWithLifecycle()
    val paused by Sim.paused.collectAsStateWithLifecycle()
    val joystick by Sim.joystick.collectAsStateWithLifecycle()
    var showSaved by remember { mutableStateOf(false) }

    val sheetState = rememberStandardBottomSheetState(skipHiddenState = true)
    val scaffold = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    var rootPx by remember { mutableIntStateOf(0) }
    var peekPx by remember { mutableIntStateOf(0) }
    var sheetPx by remember { mutableIntStateOf(0) }
    var fabPx by remember { mutableIntStateOf(0) }

    // Typing a search collapses the sheet away, so results and keyboard get the whole screen.
    var searchFocused by remember { mutableStateOf(false) }
    // Read unconditionally: a composable getter inside `&&` would only run sometimes and corrupt state.
    val imeVisible = WindowInsets.isImeVisible
    val searching = searchFocused && (imeVisible || vm.query.isNotEmpty())
    val closeSearch = {
        vm.query = ""
        focus.clearFocus()
    }
    BackHandler(enabled = searchFocused, onBack = closeSearch)
    BackHandler(enabled = !searchFocused && sheetState.targetValue == SheetValue.Expanded) {
        scope.launch { sheetState.partialExpand() }
    }
    BackHandler(enabled = !searchFocused && sheetState.targetValue != SheetValue.Expanded && vm.pin != null) {
        vm.clearPin()
    }
    LaunchedEffect(searching) { if (searching) sheetState.partialExpand() }

    val handlePx = with(density) { HandleHeight.toPx() }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val peek by animateDpAsState(
        if (searching) 0.dp else HandleHeight + with(density) { peekPx.toDp() } + navBottom,
        tween(220, easing = EaseOut),
        label = "peek",
    )
    // How much sheet shows above the bottom edge. Read only in layout/draw lambdas and
    // snapshotFlows, so dragging the sheet never recomposes the screen.
    val sheetVisible: () -> Float = {
        runCatching { rootPx - sheetState.requireOffset() }.getOrDefault(0f).coerceAtLeast(0f)
    }
    val expandProgress: () -> Float = {
        val collapsed = with(density) { peek.toPx() }
        val full = sheetPx + handlePx
        if (full <= collapsed) 0f else ((sheetVisible() - collapsed) / (full - collapsed)).coerceIn(0f, 1f)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshSetup() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.trackYou(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        vm.trackYou(false)
        vm.persist()
    }
    LaunchedEffect(vm.mode) { if (vm.mode == Mode.Route) vm.warmUpRouting() }
    LaunchedEffect(Unit) {
        for (m in vm.messages) {
            scaffold.snackbarHostState.showSnackbar(
                m.text,
                withDismissAction = m.error,
                duration = if (m.error) SnackbarDuration.Indefinite else SnackbarDuration.Short,
            )
        }
    }
    val gpxPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importGpx)
    }

    // Action to run once the location permission dialog comes back granted.
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val action = pending
        pending = null
        if (hasLocationPermission(context)) action?.invoke()
        else if (action != null) vm.warn("Location permission is needed to mock your location")
    }
    val withLocation: (() -> Unit) -> Unit = { action ->
        if (hasLocationPermission(context)) {
            action()
        } else {
            pending = action
            permissionLauncher.launch(PERMISSIONS)
        }
    }
    val toggleJoystick = {
        if (!joystick && !Settings.canDrawOverlays(context)) {
            vm.say("Allow “Display over other apps”, then tap Joystick again")
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
            )
        } else {
            vm.toggleJoystick()
        }
    }

    // First launch: ask for location once and start the map where the user actually is.
    LaunchedEffect(Unit) {
        if (!vm.hasSavedCamera && Sim.live.value == null) withLocation(vm::focusYou)
    }

    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 76.dp
    val runningRoute = plan as? Plan.Route
    val editingRoute = vm.mode == Mode.Route
    val mapColors = remember(colors, dark) {
        MapColors(
            route = colors.primary.toArgb(),
            casing = colors.surfaceContainerLowest.toArgb(),
            onRoute = colors.onPrimary.toArgb(),
            live = colors.tertiary.toArgb(),
            liveRing = (if (dark) colors.onSurface else colors.surfaceContainerLowest).toArgb(),
            you = (if (dark) YouDark else YouLight).toArgb(),
            chrome = colors.onSurfaceVariant.toArgb(),
        )
    }

    BottomSheetScaffold(
        scaffoldState = scaffold,
        modifier = Modifier.onSizeChanged { rootPx = it.height },
        sheetPeekHeight = peek,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = colors.surfaceContainerLow,
        sheetContentColor = colors.onSurface,
        sheetShadowElevation = 12.dp,
        sheetDragHandle = { SheetHandle() },
        sheetSwipeEnabled = !searching,
        snackbarHost = { SnackbarHost(it, Modifier.imePadding()) },
        containerColor = colors.surface,
        sheetContent = {
            SheetContent(
                vm, live, plan, paused, joystick,
                startMocking = withLocation,
                onJoystick = toggleJoystick,
                onImport = { gpxPicker.launch(arrayOf("*/*")) },
                expandProgress = expandProgress,
                onPeekMeasured = { peekPx = it },
                modifier = Modifier.onSizeChanged { sheetPx = it.height },
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            VoyaraMap(
                initial = vm.initialCamera,
                moves = vm.camera,
                route = runningRoute?.path?.points ?: if (editingRoute) vm.route else emptyList(),
                stops = if (editingRoute || runningRoute != null) vm.stops.toList() else emptyList(),
                live = live?.at,
                you = vm.you,
                layer = vm.mapLayer,
                traffic = vm.traffic,
                dark = dark,
                colors = mapColors,
                padTop = with(density) { topPad.toPx() },
                // The resting sheet height, not the live one: nothing re-pads the camera while
                // you drag the sheet or search, so a fly-to is never cut short.
                padBottom = with(density) { (HandleHeight + peekPx.toDp() + navBottom).toPx() },
                pin = if (editingRoute) null else vm.pin?.at,
                onPinMoved = { vm.dropPin(it) },
                onTap = { at ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    focus.clearFocus()
                    if (editingRoute) vm.addStop(at) else vm.dropPin(at)
                },
                onIdle = vm::onCameraIdle,
                modifier = Modifier.fillMaxSize(),
            )

            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                SearchField(
                    vm,
                    focused = searchFocused,
                    onFocusChange = { searchFocused = it },
                    onSaved = { showSaved = true },
                    onClose = closeSearch,
                )
                val pin = vm.pin
                when {
                    searchFocused && vm.query.isBlank() -> Suggestions(vm) {
                        focus.clearFocus()
                    }
                    vm.results.isNotEmpty() -> Results(vm) {
                        vm.fly(it)
                        focus.clearFocus()
                    }
                    !searching && !editingRoute && pin != null -> PlaceCard(vm, pin)
                }
                AnimatedVisibility(!vm.mockReady && !searching) { SetupCard(vm.devOptionsOn) }
            }

            // Map buttons ride on top of the sheet and step aside when it is fully open.
            AnimatedVisibility(
                visible = !searching && sheetState.targetValue != SheetValue.Expanded,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(0, (rootPx - sheetVisible() - fabPx - Inset.toPx()).roundToInt().coerceAtLeast(0))
                    }
                    .onSizeChanged { fabPx = it.height },
            ) {
                MapButtons(vm, onLocate = { withLocation(vm::focusYou) })
            }
        }
    }

    if (showSaved) SavedSheet(vm) { showSaved = false }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.fillMaxWidth().height(HandleHeight), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), CircleShape),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(
    vm: AppViewModel,
    live: Live?,
    plan: Plan?,
    paused: Boolean,
    joystick: Boolean,
    startMocking: (() -> Unit) -> Unit,
    onJoystick: () -> Unit,
    onImport: () -> Unit,
    expandProgress: () -> Float,
    onPeekMeasured: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        // Peek: what the collapsed sheet shows. Just the mode and the one action that matters.
        Column(
            Modifier.onSizeChanged { onPeekMeasured(it.height) }.padding(bottom = Inset),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (plan != null) StatusLine(vm, plan, paused, joystick) { live?.let { vm.flyTo(it.at, 16.0) } }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Inset)) {
                Mode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = vm.mode == m,
                        onClick = { vm.mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, Mode.entries.size),
                    ) { Text(m.name) }
                }
            }
            when (vm.mode) {
                Mode.Teleport -> TeleportActions(vm, live, plan, startMocking)
                Mode.Route -> RouteActions(vm, live, plan, paused, startMocking)
            }
        }
        // Details: revealed as the sheet is pulled up.
        Column(
            Modifier
                .graphicsLayer { alpha = expandProgress() }
                .navigationBarsPadding()
                .padding(bottom = Inset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (vm.mode) {
                Mode.Teleport -> TeleportDetails(vm, joystick, onJoystick)
                Mode.Route -> RouteDetails(vm, plan, onImport)
            }
        }
    }
}

@Composable
private fun SearchField(
    vm: AppViewModel,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onSaved: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Pill radius (28) = icon-button ripple radius (24) + the 4 dp inset.
    Surface(
        shape = CircleShape,
        color = colors.surfaceContainerLowest,
        shadowElevation = if (focused) 2.dp else 6.dp,
        border = if (focused) BorderStroke(1.5.dp, colors.outline) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (focused) onClose() else focusRequester.requestFocus() }) {
                IconSwap(
                    if (focused) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Search,
                    if (focused) "Close search" else "Search",
                    tint = colors.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                // Place names are proper nouns: capitalise words, don't "correct" them.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = {
                    vm.results.firstOrNull()?.let(vm::fly)
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        onFocusChange(it.isFocused)
                        if (it.isFocused) vm.warmUpSearch()
                    }
                    .semantics { contentDescription = "Search places or paste coordinates" },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (vm.query.isEmpty()) {
                            Text(
                                if (focused) "Place, address or lat, lng" else "Search places",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            if (vm.searching) {
                CircularProgressIndicator(Modifier.padding(horizontal = 8.dp).size(20.dp), strokeWidth = 2.dp)
            }
            val hasQuery = vm.query.isNotEmpty()
            IconButton(onClick = { if (hasQuery) vm.query = "" else onSaved() }) {
                IconSwap(
                    if (hasQuery) Icons.Rounded.Close else Icons.Rounded.Bookmarks,
                    if (hasQuery) "Clear search" else "Saved places",
                    tint = if (hasQuery) colors.onSurfaceVariant else colors.primary,
                )
            }
        }
    }
}

/** A floating card under the search bar. Radius (28) = row radius (20) + the 8 dp inset. */
@Composable
private fun FloatingCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 6.dp,
        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
        content = content,
    )
}

/** Shown while the search box is focused and empty: paste, then saved places to jump back to. */
@Composable
private fun Suggestions(vm: AppViewModel, onPicked: () -> Unit) {
    val context = LocalContext.current
    val canPaste = remember { clipboardHasText(context) }
    val saved = remember(vm.store.favorites.size, vm.store.recents.size) {
        (vm.store.favorites.map { it to true } + vm.store.recents.map { it to false })
            .distinctBy { it.first.at }
            .take(6)
    }
    if (!canPaste && saved.isEmpty()) return
    FloatingCard {
        LazyColumn(Modifier.heightIn(max = 400.dp), contentPadding = PaddingValues(8.dp)) {
            if (canPaste) {
                item {
                    PlaceRow(
                        title = "Paste from clipboard",
                        detail = "Coordinates or a map link",
                        icon = Icons.Rounded.ContentPaste,
                        onClick = {
                            readClipboard(context)?.let(vm::openShared)
                            onPicked()
                        },
                    )
                }
            }
            if (saved.isNotEmpty()) item { ListLabel("Saved") }
            items(saved) { (p, favorite) ->
                PlaceRow(
                    p.name, p.detail, if (favorite) Icons.Rounded.Star else Icons.Rounded.History,
                    trailing = vm.center?.let { formatDistance(distance(it, p.at)) },
                    onClick = {
                        vm.fly(p)
                        onPicked()
                    },
                )
            }
        }
    }
}

@Composable
private fun Results(vm: AppViewModel, onPick: (Place) -> Unit) {
    FloatingCard {
        LazyColumn(Modifier.heightIn(max = 400.dp), contentPadding = PaddingValues(8.dp)) {
            items(vm.results) {
                PlaceRow(
                    it.name, it.detail, Icons.Rounded.Place,
                    trailing = vm.center?.let { c -> formatDistance(distance(c, it.at)) },
                    onClick = { onPick(it) },
                )
            }
        }
    }
}

/** The picked place: name, address and coordinates, directly under the search bar. */
@Composable
private fun PlaceCard(vm: AppViewModel, place: Place) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val favorite = vm.store.isFavorite(place.at)
    var expanded by remember(place.at) { mutableStateOf(false) }
    FloatingCard {
        Row(
            Modifier
                .clickable(onClickLabel = if (expanded) "Show less" else "Show full address") { expanded = !expanded }
                .padding(start = 20.dp, top = 12.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    when {
                        vm.pinLoading -> "Looking up this spot…"
                        place.name.isNotBlank() -> place.name
                        else -> "Dropped pin"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                    maxLines = if (expanded) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (place.detail.isNotBlank()) {
                    Text(
                        place.detail,
                        style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
                        color = colors.onSurfaceVariant,
                        maxLines = if (expanded) 4 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        place.at.pretty(),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.onSurfaceVariant,
                    )
                    IconButton(onClick = {
                        copy(context, place.at.pretty())
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) vm.say("Coordinates copied")
                    }) {
                        Icon(Icons.Rounded.ContentCopy, "Copy coordinates", Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                    }
                }
            }
            IconToggleButton(checked = favorite, onCheckedChange = { vm.toggleFavorite() }) {
                IconSwap(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    "Favorite",
                    tint = if (favorite) colors.primary else colors.onSurfaceVariant,
                )
            }
            IconButton(onClick = vm::clearPin) {
                Icon(Icons.Rounded.Close, "Remove pin", tint = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SetupCard(devOptionsOn: Boolean) {
    val context = LocalContext.current
    val command = "adb shell appops set ${context.packageName} android:mock_location allow"
    // Card radius (28) = button radius (12) + the 16 dp padding.
    val buttonShape = RoundedCornerShape(12.dp)
    Card(
        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("One-time setup", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(
                if (devOptionsOn) {
                    "Open Developer options → Select mock location app → Voyara."
                } else {
                    "Turn on Developer options (Settings → About phone → tap Build number 7 times), " +
                        "then pick Voyara under Select mock location app."
                },
                style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(shape = buttonShape, onClick = {
                    val target = if (devOptionsOn) Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS else Settings.ACTION_DEVICE_INFO_SETTINGS
                    runCatching { context.startActivity(Intent(target)) }
                        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }) { Text("Open settings") }
                TextButton(shape = buttonShape, onClick = { copy(context, command) }) { Text("Copy adb command") }
            }
        }
    }
}

@Composable
private fun MapButtons(vm: AppViewModel, onLocate: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.padding(end = Inset), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            MapFab(Icons.Rounded.Layers, "Map style and appearance") { menu = true }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                ListLabel("Map", Modifier.padding(horizontal = 4.dp))
                // Satellite, terrain and traffic come from TomTom, so they need its key.
                for (layer in MapLayer.entries) {
                    if (!vm.tomtom && layer != MapLayer.Mono && layer != MapLayer.Street) continue
                    MenuChoice(layer.label, layer.icon, vm.mapLayer == layer) {
                        vm.setLayer(layer)
                        menu = false
                    }
                }
                if (vm.tomtom) {
                    DropdownMenuItem(
                        text = { Text("Live traffic") },
                        leadingIcon = { Icon(Icons.Rounded.Traffic, null) },
                        trailingIcon = { Switch(checked = vm.traffic, onCheckedChange = null) },
                        onClick = vm::toggleTraffic,
                        modifier = Modifier.semantics { stateDescription = if (vm.traffic) "On" else "Off" },
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                ListLabel("Appearance", Modifier.padding(horizontal = 4.dp))
                for (mode in ThemeMode.entries) {
                    MenuChoice(mode.label, mode.icon, vm.theme == mode) {
                        vm.setThemeMode(mode)
                        menu = false
                    }
                }
            }
        }
        MapFab(Icons.Rounded.MyLocation, "Show my real location", onLocate)
    }
}

@Composable
private fun MenuChoice(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, "Selected") }
        } else {
            null
        },
        onClick = onClick,
    )
}

@Composable
private fun MapFab(icon: ImageVector, label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick = onClick,
        modifier = pressScale(source),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.primary,
        interactionSource = source,
    ) { Icon(icon, label) }
}

/** The live session, as one tappable row: tap to see the mocked position. */
@Composable
private fun StatusLine(vm: AppViewModel, plan: Plan, paused: Boolean, joystick: Boolean, onShow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val text = when (plan) {
        is Plan.Hold -> if (joystick) "Mocking · joystick" else "Mocking location"
        is Plan.Roam -> "Roaming within ${formatDistance(plan.radius)}"
        is Plan.Route -> if (paused) "Route paused" else "Following route"
    }
    Surface(
        onClick = onShow,
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.padding(horizontal = Inset),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulsingDot(if (paused) colors.outline else colors.tertiary)
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (plan !is Plan.Hold || joystick) {
                Text(
                    "${vm.speedKmh.roundToInt()} km/h",
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TeleportActions(vm: AppViewModel, live: Live?, plan: Plan?, startMocking: (() -> Unit) -> Unit) {
    val pin = vm.pin
    val mocking = plan != null
    val here = pin != null && when (plan) {
        is Plan.Hold -> live != null && distance(live.at, pin.at) < 2
        is Plan.Roam -> distance(plan.center, pin.at) < 2
        else -> false
    }
    if (pin == null && !mocking) {
        Hint("Tap the map or search to choose a place")
        return
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = Inset), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (mocking) StopButton(vm::stop, Modifier.weight(if (pin == null || here) 1f else 0.6f))
        if (pin != null && !here) {
            PrimaryButton(
                label = if (mocking) "Teleport here" else "Start mocking here",
                icon = if (mocking) Icons.Rounded.NearMe else Icons.Rounded.PlayArrow,
                enabled = vm.mockReady,
                onClick = { startMocking(vm::teleport) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeleportDetails(vm: AppViewModel, joystick: Boolean, onJoystick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("Movement")
        ChipRow {
            ToggleChip("Joystick", Icons.Rounded.SportsEsports, joystick, onClick = onJoystick)
            ToggleChip("Roam", Icons.Rounded.Explore, vm.roam, onClick = vm::toggleRoam)
            ToggleChip("GPS drift", Icons.Rounded.Grain, vm.drift, onClick = vm::toggleDrift)
        }
        if (vm.roam) {
            ChipRow {
                for (r in radii) ToggleChip("Within ${formatDistance(r)}", null, vm.roamRadius == r) { vm.roamWithin(r) }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("Speed")
        SpeedChips(vm)
    }
}

@Composable
private fun RouteActions(vm: AppViewModel, live: Live?, plan: Plan?, paused: Boolean, startMocking: (() -> Unit) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val running = plan as? Plan.Route
    Column(Modifier.padding(horizontal = Inset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (running != null) {
            val progress = live?.progress ?: 0.0
            val left = running.path.length * (1 - progress)
            LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(
                "${formatDistance(left)} left · ${formatDuration(left / (vm.speedKmh / 3.6))}" +
                    if (running.loop) " · looping" else "",
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = colors.onSurfaceVariant,
            )
        } else {
            val length = remember(vm.route) { Path(vm.route).length }
            val eta = "${formatDistance(length)} · ${formatDuration(length / (vm.speedKmh / 3.6))}"
            Text(
                when {
                    vm.track != null -> "GPX track · $eta"
                    vm.stops.size >= 2 -> "${vm.stops.size} stops · $eta"
                    vm.stops.size == 1 -> "Tap the map to add the next stop"
                    else -> "Tap the map to add stops"
                },
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = colors.onSurfaceVariant,
            )
            if (vm.routing) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = Inset), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (running != null) {
            StopButton(vm::stop, Modifier.weight(0.6f))
            PrimaryButton(
                label = if (paused) "Resume" else "Pause",
                icon = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                onClick = vm::togglePause,
                modifier = Modifier.weight(1f),
            )
        } else {
            PrimaryButton(
                label = "Start route",
                icon = Icons.Rounded.PlayArrow,
                enabled = vm.route.size >= 2 && !vm.routing && vm.mockReady,
                onClick = { startMocking(vm::startRoute) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RouteDetails(vm: AppViewModel, plan: Plan?, onImport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("Speed")
        SpeedChips(vm)
    }
    if (plan is Plan.Route) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("Route")
        ChipRow {
            ToggleChip("Follow roads", Icons.Rounded.Route, vm.followRoads, enabled = vm.track == null, onClick = vm::toggleRoads)
            ToggleChip("Loop", Icons.Rounded.Repeat, vm.loop, onClick = vm::toggleLoop)
        }
        ChipRow {
            ActionChip("Undo", Icons.AutoMirrored.Rounded.Undo, enabled = vm.stops.isNotEmpty(), onClick = vm::undoStop)
            ActionChip("Clear", Icons.Rounded.DeleteSweep, enabled = vm.stops.isNotEmpty(), onClick = vm::clearStops)
            ActionChip("Import GPX", Icons.Rounded.FileOpen, onClick = onImport)
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Inset).semantics { heading() },
    )
}

/** A chip row that scrolls edge to edge, so a clipped chip hints there is more. */
@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Inset),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** Selected chips show a check as well as a fill, so state never rests on color alone. */
@Composable
private fun ToggleChip(label: String, icon: ImageVector?, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (icon != null || selected) {
            {
                IconSwap(
                    if (selected || icon == null) Icons.Rounded.Check else icon,
                    null,
                    Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ActionChip(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(icon, null, Modifier.size(AssistChipDefaults.IconSize)) },
    )
}

@Composable
private fun SpeedChips(vm: AppViewModel) {
    ChipRow {
        for (s in speeds) {
            val selected = vm.speedKmh == s.kmh
            ToggleChip(if (selected) "${s.label} · ${s.kmh.roundToInt()} km/h" else s.label, s.icon, selected) {
                vm.setSpeed(s.kmh)
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).then(pressScale(source)),
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        interactionSource = source,
    ) {
        IconSwap(icon, null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// Red here is the live color: stopping ends the live mock.
@Composable
private fun StopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).then(pressScale(source)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        interactionSource = source,
    ) {
        Icon(Icons.Rounded.Stop, null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("Stop", maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val favorites = vm.store.favorites
    val recents = vm.store.recents
    var tab by rememberSaveable { mutableIntStateOf(if (favorites.isEmpty() && recents.isNotEmpty()) 1 else 0) }
    var renaming by remember { mutableStateOf<Place?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 24.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Saved places",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (tab == 1 && recents.isNotEmpty()) {
                TextButton(onClick = vm.store::clearRecents) { Text("Clear all") }
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            SegmentedButton(tab == 0, { tab = 0 }, SegmentedButtonDefaults.itemShape(0, 2)) {
                Text("Favorites · ${favorites.size}", style = MaterialTheme.typography.labelLarge.tabular())
            }
            SegmentedButton(tab == 1, { tab = 1 }, SegmentedButtonDefaults.itemShape(1, 2)) {
                Text("Recent · ${recents.size}", style = MaterialTheme.typography.labelLarge.tabular())
            }
        }
        val list = if (tab == 0) favorites else recents
        if (list.isEmpty()) {
            EmptyState(
                icon = if (tab == 0) Icons.Rounded.StarBorder else Icons.Rounded.History,
                title = if (tab == 0) "No favorites yet" else "Nothing here yet",
                body = if (tab == 0) {
                    "Drop a pin on the map and tap the star to keep a place here."
                } else {
                    "Places you mock appear here, newest first."
                },
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
                items(list, key = { "${it.at.lat},${it.at.lng}" }) { p ->
                    SavedRow(
                        p,
                        favorite = tab == 0,
                        away = vm.center?.let { formatDistance(distance(it, p.at)) },
                        onClick = {
                            vm.fly(p)
                            onDismiss()
                        },
                        onRename = { renaming = p },
                        onRemove = { if (tab == 0) vm.store.removeFavorite(p) else vm.store.removeRecent(p) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
    renaming?.let { place ->
        RenameDialog(place, onDismiss = { renaming = null }) { name ->
            vm.store.renameFavorite(place, name)
            renaming = null
        }
    }
}

@Composable
private fun SavedRow(
    place: Place,
    favorite: Boolean,
    away: String?,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(place.detail.ifBlank { null }, away).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium.tabular(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Box(Modifier.size(40.dp).background(colors.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.History,
                    null,
                    Modifier.size(20.dp),
                    tint = if (favorite) colors.primary else colors.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More for ${place.name}") }
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = colors.surfaceContainer,
                ) {
                    if (favorite) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = {
                                menu = false
                                onRename()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = {
                            menu = false
                            onRemove()
                        },
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
    )
}

@Composable
private fun RenameDialog(place: Place, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(place.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename favorite") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(name) }),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).background(colors.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(28.dp), tint = colors.onSurfaceVariant)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ListLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Paragraph),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Inset, vertical = 8.dp),
    )
}

@Composable
private fun PlaceRow(
    title: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = detail.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = trailing?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
    )
}
