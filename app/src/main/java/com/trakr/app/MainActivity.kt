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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable private fun TrakrTheme(mode: String, accent: String, content: @Composable () -> Unit) {
    val monochromeDark = darkColorScheme(
        primary = Color(0xFFE8E8E8), onPrimary = Color(0xFF1B1B1B), primaryContainer = Color(0xFF3D3D3D), onPrimaryContainer = Color(0xFFF2F2F2),
        secondary = Color(0xFFD0D0D0), onSecondary = Color(0xFF242424), secondaryContainer = Color(0xFF444444), onSecondaryContainer = Color(0xFFF0F0F0),
        tertiary = Color(0xFFD0D0D0), onTertiary = Color(0xFF242424), tertiaryContainer = Color(0xFF444444), onTertiaryContainer = Color(0xFFF0F0F0),
        error = Color(0xFFE8E8E8), onError = Color(0xFF1B1B1B), errorContainer = Color(0xFF444444), onErrorContainer = Color(0xFFF0F0F0),
        background = Color(0xFF121212), onBackground = Color(0xFFE8E8E8), surface = Color(0xFF121212), onSurface = Color(0xFFE8E8E8),
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color(0xFFC8C8C8), outline = Color(0xFF808080), outlineVariant = Color(0xFF363636),
        scrim = Color(0xFF000000), inverseSurface = Color(0xFFE8E8E8), inverseOnSurface = Color(0xFF1B1B1B), inversePrimary = Color(0xFF3D3D3D), surfaceTint = Color(0xFFE8E8E8),
        surfaceDim = Color(0xFF090909), surfaceBright = Color(0xFF3A3A3A), surfaceContainerLowest = Color(0xFF090909), surfaceContainerLow = Color(0xFF121212), surfaceContainer = Color(0xFF1B1B1B), surfaceContainerHigh = Color(0xFF242424), surfaceContainerHighest = Color(0xFF303030),
    )
    val accentColor = when (accent) { "Blue" -> Color(0xFF2563EB); "Green" -> Color(0xFF15803D); "Purple" -> Color(0xFF7E22CE); else -> Color(0xFF3A3A3A) }
    fun Color.tint(t: Float) = lerp(this, accentColor, t)
    val accentLight = lerp(accentColor, Color.White, 0.42f)
    fun Color.tintL(t: Float) = lerp(this, accentLight, t)
    val scheme = if (mode == "Light") lightColorScheme(
        primary = accentColor, onPrimary = Color.White,
        primaryContainer = Color(0xFFF2F2F2).tint(0.32f), onPrimaryContainer = Color(0xFF1B1B1B),
        secondaryContainer = Color(0xFFE8E8E8).tintL(0.45f), onSecondaryContainer = Color(0xFF1B1B1B),
        background = Color(0xFFF8F8F8).tintL(0.16f), surface = Color(0xFFF8F8F8).tintL(0.16f),
        surfaceContainerLowest = Color(0xFFFFFFFF).tintL(0.12f), surfaceContainerLow = Color(0xFFF5F5F5).tintL(0.20f), surfaceContainer = Color(0xFFF2F2F2).tintL(0.26f), surfaceContainerHigh = Color(0xFFECECEC).tintL(0.30f), surfaceContainerHighest = Color(0xFFE6E6E6).tintL(0.34f),
        surfaceVariant = Color(0xFFE8E8E8).tintL(0.28f),
        onSurface = Color(0xFF1B1B1B).tint(0.22f), onSurfaceVariant = Color(0xFF565656).tint(0.20f), outline = Color(0xFF6B6B6B).tint(0.18f), outlineVariant = Color(0xFFBABABA).tintL(0.30f),
    ) else monochromeDark.copy(
        primary = accentColor, onPrimary = Color.White,
        primaryContainer = Color(0xFF1B1B1B).tint(0.24f), onPrimaryContainer = Color(0xFFF2F2F2),
        secondaryContainer = Color(0xFF3A3A3A).tint(0.28f), onSecondaryContainer = Color(0xFFF0F0F0),
        background = Color(0xFF121212).tint(0.05f), surface = Color(0xFF121212).tint(0.05f),
        surfaceVariant = Color(0xFF2C2C2C).tint(0.08f),
        surfaceContainerLowest = Color(0xFF090909).tint(0.05f), surfaceContainerLow = Color(0xFF121212).tint(0.06f), surfaceContainer = Color(0xFF1B1B1B).tint(0.08f), surfaceContainerHigh = Color(0xFF242424).tint(0.09f), surfaceContainerHighest = Color(0xFF303030).tint(0.10f),
        onBackground = Color(0xFFE8E8E8).tintL(0.16f), onSurface = Color(0xFFE8E8E8).tintL(0.16f), onSurfaceVariant = Color(0xFFC8C8C8).tintL(0.20f),
    )
    val view = LocalView.current
    SideEffect {
        val activity = view.context as ComponentActivity
        val barColor = scheme.background.toArgb()
        if (mode == "Light") {
            activity.enableEdgeToEdge(SystemBarStyle.light(barColor, barColor), SystemBarStyle.light(barColor, barColor))
        } else {
            activity.enableEdgeToEdge(SystemBarStyle.dark(barColor), SystemBarStyle.dark(barColor))
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TrakrApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var appearance by remember { mutableStateOf(loadSetting(context, "appearance", "Dark")) }
    var accent by remember { mutableStateOf(loadSetting(context, "accent", "Monochrome").takeIf { it in setOf("Monochrome", "Blue", "Green", "Purple") } ?: "Monochrome") }
    TrakrTheme(appearance, accent) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var entries by remember(month) { mutableStateOf(loadEntries(context, month)) }
    var currency by remember { mutableStateOf(loadCurrency(context)) }
    var settings by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var displayedForm by remember { mutableStateOf<String?>(null) }
    var choosingMonth by remember { mutableStateOf(false) }
    fun save(newEntries: List<Entry>) { entries = newEntries; saveEntries(context, month, newEntries) }

    if (settings) { BackHandler { settings = false }; SettingsScreen(currency, appearance, accent, onCurrency = { currency = it; saveCurrency(context, it) }, onAppearance = { appearance = it; saveSetting(context, "appearance", it) }, onAccent = { accent = it; saveSetting(context, "accent", it) }, onBack = { settings = false }); return@TrakrTheme }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), fontFamily = Geist, modifier = Modifier.clickable { choosingMonth = true }) }, navigationIcon = { IconButton(onClick = { month = month.minusMonths(1) }) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous month") } }, actions = { IconButton(onClick = { month = month.plusMonths(1) }) { Icon(painterResource(R.drawable.ic_chevron_right), "Next month") } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
    ) { padding -> Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        LedgerContent(month, entries, currency, { id -> save(entries.filterNot { it.id == id }) }, dialog, displayedForm, { dialog = null }, { save(entries + it); dialog = null }, Modifier.fillMaxSize())
        QuickActions(onIncome = { displayedForm = "income"; dialog = "income" }, onExpense = { displayedForm = "expense"; dialog = "expense" }, onSettings = { settings = true }, modifier = Modifier.align(Alignment.BottomCenter))
    } }
    if (choosingMonth) MonthDialog(month, { choosingMonth = false }, { month = it; choosingMonth = false })
    }
}

@Composable private fun LedgerContent(month: YearMonth, entries: List<Entry>, currency: String, onDelete: (String) -> Unit, formType: String?, displayedForm: String?, onFormDismiss: () -> Unit, onAdd: (Entry) -> Unit, modifier: Modifier = Modifier) {
    val income = entries.filter { it.type != "Expense" }.sumOf { it.amount }
    val expenses = entries.filter { it.type == "Expense" }.sumOf { it.amount }
    val balance = income - expenses
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(
            visible = formType != null,
            enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120)),
        ) {
            Column { if (displayedForm != null) EntryCard(isIncome = displayedForm == "income", onDismiss = onFormDismiss, onSave = onAdd); Spacer(Modifier.height(16.dp)) }
        }
        ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Column(Modifier.padding(20.dp)) {
            Text("Available", style = MaterialTheme.typography.labelLarge, fontFamily = Geist, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            Text(money(balance, currency), style = MaterialTheme.typography.displaySmall, fontFamily = Geist, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) { SummaryValue("Income", income, currency, MaterialTheme.colorScheme.onPrimary); SummaryValue("Spent", expenses, currency, MaterialTheme.colorScheme.onPrimary) }
        } }
        LedgerSection("Income", entries.filter { it.type != "Expense" }, "No income this month", currency, onDelete)
        LedgerSection("Expenses", entries.filter { it.type == "Expense" }, "No expenses this month", currency, onDelete)
        Spacer(Modifier.height(164.dp))
    }
}

@Composable private fun SummaryValue(label: String, amount: Long, currency: String, onColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) { Column { Text(label, style = MaterialTheme.typography.labelMedium, fontFamily = Geist, color = onColor.copy(alpha = 0.75f)); Text(money(amount, currency), style = MaterialTheme.typography.titleMedium, fontFamily = Geist, fontWeight = FontWeight.Bold, color = onColor) } }

@Composable private fun LedgerSection(title: String, entries: List<Entry>, emptyLabel: String, currency: String, onDelete: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = Geist, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
    OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface), border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        if (entries.isEmpty()) ListItem(headlineContent = { Text(emptyLabel, fontFamily = Geist) }, supportingContent = { Text("Use the buttons below to add one.", fontFamily = Geist) })
        else entries.forEachIndexed { index, entry -> TransactionRow(entry, currency, onDelete); if (index < entries.lastIndex) HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp) }
    }
}

@Composable private fun TransactionRow(entry: Entry, currency: String, onDelete: (String) -> Unit) {
    val density = LocalDensity.current
    val actionWidth = 96.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember(entry.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth()) {
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
                },
        ) {
            ListItem(
                headlineContent = { Text(entry.note, fontFamily = Geist) },
                trailingContent = { Text(money(entry.amount, currency), style = MaterialTheme.typography.labelLarge, fontFamily = Geist, fontWeight = FontWeight.Bold) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@Composable private fun QuickActions(onIncome: () -> Unit, onExpense: () -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxWidth()
            .height(164.dp)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.60f to background,
                        1f to background,
                    ),
                ),
            ),
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
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
            ) { Icon(painterResource(R.drawable.ic_arrow_down), null); Spacer(Modifier.width(8.dp)); Text("Income", fontFamily = Geist) }
            Button(onClick = onExpense, modifier = Modifier.weight(1f).height(60.dp), shape = CircleShape) { Icon(painterResource(R.drawable.ic_arrow_up), null); Spacer(Modifier.width(8.dp)); Text("Expense", fontFamily = Geist) }
            FilledTonalIconButton(
                onClick = onSettings,
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
            ) { Icon(painterResource(R.drawable.ic_settings), "Settings") }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsScreen(currency: String, appearance: String, color: String, onCurrency: (String) -> Unit, onAppearance: (String) -> Unit, onAccent: (String) -> Unit, onBack: () -> Unit) {
    var currencyOpen by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }
    var colorOpen by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Settings", fontFamily = Geist) }, navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_chevron_left), "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Preferences", style = MaterialTheme.typography.titleSmall, fontFamily = Geist, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingRow("Currency", currency, currencyOpen, { currencyOpen = it }, listOf("INR", "IDR", "USD", "CAD", "MYR"), onCurrency)
            HorizontalDivider(thickness = 0.5.dp)
            SettingRow("Appearance", appearance, appearanceOpen, { appearanceOpen = it }, listOf("Dark", "Light"), onAppearance)
            HorizontalDivider(thickness = 0.5.dp)
            SettingRow("Theme color", color, colorOpen, { colorOpen = it }, listOf("Monochrome", "Blue", "Green", "Purple"), onAccent)
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
