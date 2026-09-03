package com.trakr.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class Entry(val id: String = UUID.randomUUID().toString(), val type: String, val note: String, val amount: Long)
private val Geist = FontFamily(Font(R.font.geist_variable))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.rgb(18, 18, 18)), SystemBarStyle.dark(android.graphics.Color.rgb(9, 9, 9)))
        setContent { TrakrApp() }
    }
}

@Composable private fun TrakrTheme(content: @Composable () -> Unit) {
    val accentColor = Color(0xFF3A3A3A)
    fun Color.tint(t: Float) = lerp(this, accentColor, t)
    val accentLight = lerp(accentColor, Color.White, 0.42f)
    fun Color.tintL(t: Float) = lerp(this, accentLight, t)
    val scheme = lightColorScheme(
        primary = accentColor, onPrimary = Color.White,
        primaryContainer = Color(0xFFF2F2F2).tint(0.32f), onPrimaryContainer = Color(0xFF1B1B1B),
        secondaryContainer = Color.White, onSecondaryContainer = Color(0xFF1B1B1B),
        background = Color.Transparent, surface = Color.Transparent,
        surfaceContainerLowest = Color(0xFFFFFFFF).tintL(0.12f), surfaceContainerLow = Color(0xFFF5F5F5).tintL(0.20f), surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFECECEC).tintL(0.30f), surfaceContainerHighest = Color(0xFFE6E6E6).tintL(0.34f),
        surfaceVariant = Color(0xFFE8E8E8).tintL(0.28f),
        onSurface = Color(0xFF1B1B1B).tint(0.22f), onSurfaceVariant = Color(0xFF565656).tint(0.20f), outline = Color(0xFF6B6B6B).tint(0.18f), outlineVariant = Color(0xFFBABABA).tintL(0.30f),
    )
    val view = LocalView.current
    SideEffect {
        val activity = view.context as ComponentActivity
        val transparent = Color.Transparent.toArgb()
        // Top of the backdrop is near-black, bottom is near-white: light status icons, dark nav icons.
        activity.enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.light(transparent, transparent))
    }
    MaterialTheme(colorScheme = scheme) {
        Box(Modifier.fillMaxSize().clipToBounds()) {
            Image(painterResource(R.drawable.bg_gradient), null, Modifier.fillMaxSize().graphicsLayer(scaleX = 1.25f, scaleY = 1.25f, transformOrigin = TransformOrigin(0.5f, 0f)), contentScale = ContentScale.Crop)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TrakrApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    TrakrTheme {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var entries by remember(month) { mutableStateOf(loadEntries(context, month)) }
    var currency by remember { mutableStateOf(loadCurrency(context)) }
    var settings by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var displayedForm by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var choosingMonth by remember { mutableStateOf(false) }
    fun save(newEntries: List<Entry>) { entries = newEntries; saveEntries(context, month, newEntries) }

    if (settings) { BackHandler { settings = false }; SettingsScreen(currency, onCurrency = { currency = it; saveCurrency(context, it) }, onBack = { settings = false }); return@TrakrTheme }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), fontFamily = Geist, modifier = Modifier.clickable { choosingMonth = true }) }, navigationIcon = { IconButton(onClick = { month = month.minusMonths(1) }) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous month") } }, actions = { IconButton(onClick = { month = month.plusMonths(1) }) { Icon(painterResource(R.drawable.ic_chevron_right), "Next month") } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)) },
        containerColor = Color.Transparent,
    ) { padding -> Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        LedgerContent(
            month, entries, currency,
            onDelete = { id -> save(entries.filterNot { it.id == id }) },
            formType = dialog, displayedForm = displayedForm,
            onFormDismiss = { dialog = null },
            onAdd = { save(entries + it); dialog = null },
            editingId = editingId,
            onEditStart = { editingId = it },
            onEditCommit = { updated -> save(entries.map { if (it.id == updated.id) updated else it }); editingId = null },
            onReorder = { ordered -> save(reorderWithin(entries, ordered)) },
            modifier = Modifier.fillMaxSize(),
        )
        QuickActions(onIncome = { editingId = null; displayedForm = "income"; dialog = "income" }, onExpense = { editingId = null; displayedForm = "expense"; dialog = "expense" }, onSettings = { settings = true }, modifier = Modifier.align(Alignment.BottomCenter))
    } }
    if (choosingMonth) MonthDialog(month, { choosingMonth = false }, { month = it; choosingMonth = false })
    }
}

@Composable private fun LedgerContent(month: YearMonth, entries: List<Entry>, currency: String, onDelete: (String) -> Unit, formType: String?, displayedForm: String?, onFormDismiss: () -> Unit, onAdd: (Entry) -> Unit, editingId: String?, onEditStart: (String) -> Unit, onEditCommit: (Entry) -> Unit, onReorder: (List<Entry>) -> Unit, modifier: Modifier = Modifier) {
    val income = entries.filter { it.type != "Expense" }.sumOf { it.amount }
    val expenses = entries.filter { it.type == "Expense" }.sumOf { it.amount }
    val balance = income - expenses
    val scrollState = rememberScrollState()
    LaunchedEffect(formType) { if (formType != null) scrollState.animateScrollTo(0) }
    Column(modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(
            visible = formType != null,
            enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120)),
        ) {
            Column { if (displayedForm != null) EntryCard(isIncome = displayedForm == "income", onDismiss = onFormDismiss, onSave = onAdd); Spacer(Modifier.height(16.dp)) }
        }
        ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White, contentColor = Color(0xFF1B1B1B))) { Column(Modifier.padding(20.dp)) {
            Text("Available", style = MaterialTheme.typography.labelLarge, fontFamily = Geist, color = Color(0xFF1B1B1B).copy(alpha = 0.8f))
            Text(money(balance, currency), style = MaterialTheme.typography.displaySmall, fontFamily = Geist, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1B1B1B).copy(alpha = 0.2f))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) { SummaryValue("Income", income, currency, Color(0xFF1B1B1B)); SummaryValue("Spent", expenses, currency, Color(0xFF1B1B1B)) }
        } }
        LedgerSection("Income", entries.filter { it.type != "Expense" }, "No income this month", currency, onDelete, editingId, onEditStart, onEditCommit, onReorder)
        LedgerSection("Expenses", entries.filter { it.type == "Expense" }, "No expenses this month", currency, onDelete, editingId, onEditStart, onEditCommit, onReorder)
        Spacer(Modifier.height(164.dp))
    }
}

@Composable private fun SummaryValue(label: String, amount: Long, currency: String, onColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) { Column { Text(label, style = MaterialTheme.typography.labelMedium, fontFamily = Geist, color = onColor.copy(alpha = 0.75f)); Text(money(amount, currency), style = MaterialTheme.typography.titleMedium, fontFamily = Geist, fontWeight = FontWeight.Bold, color = onColor) } }

/** Puts [ordered] (one section's rows, in their new order) back into [all] without moving the other section. */
private fun reorderWithin(all: List<Entry>, ordered: List<Entry>): List<Entry> {
    val ids = ordered.map { it.id }.toSet()
    val next = ordered.iterator()
    return all.map { if (it.id in ids) next.next() else it }
}

@Composable private fun LedgerSection(title: String, entries: List<Entry>, emptyLabel: String, currency: String, onDelete: (String) -> Unit, editingId: String?, onEditStart: (String) -> Unit, onEditCommit: (Entry) -> Unit, onReorder: (List<Entry>) -> Unit) {
    // Long-press reorder: the held row follows the finger; rows it passes slide out of the way by one row height.
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    var rowHeight by remember { mutableStateOf(0) }
    val dividerPx = with(LocalDensity.current) { 0.5.dp.toPx() }
    val slot = rowHeight + dividerPx
    val shift = if (draggingIndex < 0 || slot <= 0f) 0 else (dragOffset / slot).roundToInt().coerceIn(-draggingIndex, entries.lastIndex - draggingIndex)
    Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = Geist, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
    OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface), border = BorderStroke(0.dp, Color.Transparent)) {
        if (entries.isEmpty()) ListItem(headlineContent = { Text(emptyLabel, fontFamily = Geist) }, supportingContent = { Text("Use the buttons below to add one.", fontFamily = Geist) })
        else entries.forEachIndexed { index, entry ->
            val isDragging = index == draggingIndex
            val displaced = when {
                draggingIndex < 0 || isDragging -> 0f
                shift > 0 && index in (draggingIndex + 1)..(draggingIndex + shift) -> -slot
                shift < 0 && index in (draggingIndex + shift) until draggingIndex -> slot
                else -> 0f
            }
            val displacedAnim by animateFloatAsState(displaced, tween(160), label = "displace")
            TransactionRow(
                entry, currency, onDelete,
                editing = entry.id == editingId, onEditStart = { onEditStart(entry.id) }, onEditCommit = onEditCommit,
                dragging = isDragging,
                translationY = if (isDragging) dragOffset else displacedAnim,
                onMeasured = { rowHeight = it },
                onDragStart = { draggingIndex = index; dragOffset = 0f },
                onDrag = { dragOffset += it },
                onDragEnd = { commit ->
                    if (commit && shift != 0) { val list = entries.toMutableList(); val moved = list.removeAt(draggingIndex); list.add(draggingIndex + shift, moved); onReorder(list) }
                    draggingIndex = -1; dragOffset = 0f
                },
            )
            if (index < entries.lastIndex) HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Color(0xFFEDEDED))
        }
    }
}

@Composable private fun TransactionRow(entry: Entry, currency: String, onDelete: (String) -> Unit, editing: Boolean, onEditStart: () -> Unit, onEditCommit: (Entry) -> Unit, dragging: Boolean, translationY: Float, onMeasured: (Int) -> Unit, onDragStart: () -> Unit, onDrag: (Float) -> Unit, onDragEnd: (Boolean) -> Unit) {
    val density = LocalDensity.current
    val actionWidth = 96.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember(entry.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    // The pointerInput below is keyed on entry.id, so it would otherwise hold the first composition's lambdas.
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { this.translationY = translationY; shadowElevation = if (dragging) 8.dp.toPx() else 0f },
    ) {
        Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(Color(0xFFDC2626))
                    .clickable { onDelete(entry.id) },
                contentAlignment = Alignment.Center,
            ) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.ic_trash), null, tint = Color.White); Spacer(Modifier.width(6.dp)); Text("Delete", fontFamily = Geist, color = Color.White, style = MaterialTheme.typography.labelLarge) } }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(entry.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount -> change.consume(); scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-actionWidthPx, 0f)) } },
                        onDragEnd = { scope.launch { offsetX.animateTo(if (offsetX.value < -actionWidthPx / 2) -actionWidthPx else 0f, tween(200)) } },
                    )
                }
                .pointerInput(entry.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { offsetX.snapTo(0f) }; currentOnDragStart() },
                        onDrag = { change, dragAmount -> change.consume(); currentOnDrag(dragAmount.y) },
                        onDragEnd = { currentOnDragEnd(true) },
                        onDragCancel = { currentOnDragEnd(false) },
                    )
                },
        ) {
            if (editing) InlineEditor(entry, onCommit = onEditCommit)
            else ListItem(
                headlineContent = { Text(entry.note, fontFamily = Geist) },
                trailingContent = { Text(money(entry.amount, currency), style = MaterialTheme.typography.labelLarge, fontFamily = Geist, fontWeight = FontWeight.Bold) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { if (offsetX.value == 0f) onEditStart() else scope.launch { offsetX.animateTo(0f, tween(200)) } },
            )
        }
    }
}

/** Edits a row in place: description on the left, amount on the right. Done (or Back) commits; blank/zero values keep the old ones. */
@Composable private fun InlineEditor(entry: Entry, onCommit: (Entry) -> Unit) {
    var note by remember(entry.id) { mutableStateOf(entry.note) }
    var amount by remember(entry.id) { mutableStateOf(entry.amount.toString()) }
    val noteFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    fun commit() { keyboard?.hide(); onCommit(entry.copy(note = note.trim().ifBlank { entry.note }, amount = amount.toLongOrNull()?.takeIf { it > 0 } ?: entry.amount)) }
    LaunchedEffect(entry.id) { noteFocus.requestFocus(); keyboard?.show() }
    BackHandler { commit() }
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            note, { note = it },
            modifier = Modifier.weight(1f).focusRequester(noteFocus),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Geist, color = onSurface),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            amount, { amount = it.filter(Char::isDigit) },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.labelLarge.copy(fontFamily = Geist, fontWeight = FontWeight.Bold, color = onSurface, textAlign = TextAlign.End),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
        )
    }
}

@Composable private fun QuickActions(onIncome: () -> Unit, onExpense: () -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(164.dp)
            .background(Brush.verticalGradient(colorStops = arrayOf(0f to Color.Transparent, 0.60f to Color.White, 1f to Color.White))),
    ) {
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onIncome,
                modifier = Modifier.weight(1f).height(60.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black, contentColor = Color.White),
            ) { Icon(painterResource(R.drawable.ic_arrow_down), null); Spacer(Modifier.width(8.dp)); Text("Income", fontFamily = Geist) }
            FilledTonalButton(onClick = onExpense, modifier = Modifier.weight(1f).height(60.dp), shape = CircleShape, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black, contentColor = Color.White)) { Icon(painterResource(R.drawable.ic_arrow_up), null); Spacer(Modifier.width(8.dp)); Text("Expense", fontFamily = Geist) }
            FilledTonalIconButton(
                onClick = onSettings,
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Black, contentColor = Color.White),
            ) { Icon(painterResource(R.drawable.ic_settings), "Settings") }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsScreen(currency: String, onCurrency: (String) -> Unit, onBack: () -> Unit) {
    var currencyOpen by remember { mutableStateOf(false) }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("Settings", fontFamily = Geist) }, navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_chevron_left), "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("Preferences", style = MaterialTheme.typography.titleSmall, fontFamily = Geist, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface), border = BorderStroke(0.dp, Color.Transparent)) {
                SettingRow("Currency", currency, currencyOpen, { currencyOpen = it }, listOf("INR", "IDR", "USD", "CAD", "MYR"), onCurrency)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingRow(label: String, value: String, open: Boolean, setOpen: (Boolean) -> Unit, options: List<String>, onSelect: (String) -> Unit) {
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = setOpen, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(label, fontFamily = Geist) },
            supportingContent = { Text(value, fontFamily = Geist) },
            trailingContent = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { setOpen(false) }, modifier = Modifier.fillMaxWidth()) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option, fontFamily = Geist) }, onClick = { onSelect(option); setOpen(false) }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding) }
        }
    }
}

@Composable private fun EntryCard(isIncome: Boolean, onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    var amount by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    val amountFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { amountFocus.requestFocus(); keyboard?.show() }
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (isIncome) "Add income" else "Add expense", style = MaterialTheme.typography.titleLarge, fontFamily = Geist)
        OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount", fontFamily = Geist) }, prefix = { Text("$", fontFamily = Geist) }, textStyle = TextStyle(fontFamily = Geist), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().focusRequester(amountFocus))
        OutlinedTextField(note, { note = it }, label = { Text("Description", fontFamily = Geist) }, textStyle = TextStyle(fontFamily = Geist), singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Cancel", fontFamily = Geist) }; Button(onClick = { val value = amount.toLongOrNull() ?: 0; if (value > 0 && note.isNotBlank()) onSave(Entry(type = if (isIncome) "Income" else "Expense", note = note.trim(), amount = value)) }) { Text("Save", fontFamily = Geist) } }
    } }
}

@Composable private fun MonthDialog(current: YearMonth, onDismiss: () -> Unit, onPick: (YearMonth) -> Unit) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Choose month", fontFamily = Geist) }, text = { Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { selected = selected.minusYears(1) }) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous year") }; Text(selected.year.toString(), style = MaterialTheme.typography.titleMedium, fontFamily = Geist); IconButton(onClick = { selected = selected.plusYears(1) }) { Icon(painterResource(R.drawable.ic_chevron_right), "Next year") } }
        java.time.Month.entries.toList().chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { item -> TextButton(onClick = { selected = YearMonth.of(selected.year, item) }) { Text(item.name.take(3), fontFamily = Geist) } } } }
    } }, confirmButton = { TextButton(onClick = { onPick(selected) }) { Text("Done", fontFamily = Geist) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", fontFamily = Geist) } })
}

private fun money(amount: Long, currency: String): String = NumberFormat.getCurrencyInstance(Locale.US).apply { this.currency = java.util.Currency.getInstance(currency) }.format(amount)
private fun loadCurrency(context: Context) = context.getSharedPreferences("trakr", Context.MODE_PRIVATE).getString("currency", "USD") ?: "USD"
private fun saveCurrency(context: Context, currency: String) { context.getSharedPreferences("trakr", Context.MODE_PRIVATE).edit().putString("currency", currency).apply() }
private fun loadSetting(context: Context, key: String, fallback: String) = context.getSharedPreferences("trakr", Context.MODE_PRIVATE).getString(key, fallback) ?: fallback
private fun saveSetting(context: Context, key: String, value: String) { context.getSharedPreferences("trakr", Context.MODE_PRIVATE).edit().putString(key, value).apply() }
private fun key(month: YearMonth) = "entries_$month"
private fun loadEntries(context: Context, month: YearMonth): List<Entry> = runCatching { val raw = context.getSharedPreferences("trakr", Context.MODE_PRIVATE).getString(key(month), "[]") ?: "[]"; val json = JSONArray(raw); List(json.length()) { index -> json.getJSONObject(index).let { Entry(it.getString("id"), it.getString("type"), it.getString("note"), it.getLong("amount")) } } }.getOrDefault(emptyList())
private fun saveEntries(context: Context, month: YearMonth, entries: List<Entry>) { val json = JSONArray(); entries.forEach { json.put(JSONObject().put("id", it.id).put("type", it.type).put("note", it.note).put("amount", it.amount)) }; context.getSharedPreferences("trakr", Context.MODE_PRIVATE).edit().putString(key(month), json.toString()).apply() }
