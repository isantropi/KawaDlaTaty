package pl.kawadlataty.dad

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext

object CoffeeColors {
    val Dark = Color(0xFF2A1810)
    val Espresso = Color(0xFF3B2418)
    val Brown = Color(0xFF6F4E37)
    val Caramel = Color(0xFFB57A45)
    val Latte = Color(0xFFFFF7EC)
    val Cream = Color(0xFFFFE8C7)
    val Surface = Color(0xFFF4E7D3)
    val SurfaceDeep = Color(0xFFE7CFAF)
    val Success = Color(0xFF4F7A45)
    val Warning = Color(0xFFD68A2D)
    val Danger = Color(0xFFA9472B)
}

@Composable
fun coffeeColorScheme() = lightColorScheme(
    primary = CoffeeColors.Brown,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D8C3),
    onPrimaryContainer = Color(0xFF2A1810),

    secondary = CoffeeColors.Caramel,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1DFC7),
    onSecondaryContainer = Color(0xFF2A1810),

    background = CoffeeColors.Surface,
    onBackground = CoffeeColors.Dark,

    surface = CoffeeColors.Latte,
    onSurface = CoffeeColors.Dark,

    surfaceVariant = Color(0xFFE8D8C3),
    onSurfaceVariant = Color(0xFF3B2418),

    error = Color(0xFF8B2F23),
    onError = Color.White
)

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions.launch(
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            kotlinx.coroutines.MainScope().launch {
                runCatching { Api.registerToken("dad", token) }
            }
        }

        setContent {

            val view = LocalView.current

            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = CoffeeColors.Brown.toArgb()
                window.navigationBarColor = CoffeeColors.Brown.toArgb()
            }

            MaterialTheme(
                colorScheme = coffeeColorScheme()
            ) {
                DadScreen()
            }
        }
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
}

@Composable
fun DadScreen() {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(AppState(date = "")) }
    var message by remember { mutableStateOf("") }
    var pendingCoffee by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<WarningResponse?>(null) }
    var confirmMeal by remember { mutableStateOf(false) }
    var confirmCancelMeal by remember { mutableStateOf(false) }

    suspend fun refresh() {
        state = Api.state()
    }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { refresh() }
            delay(10_000)
        }
    }

    val drinks = listOf(
        Drink("AMERICANO", "AMERICANO", "czarna kawa • kubek Władcy Sieci"),
        Drink("LATTE", "LATTE", "kawa z mlekiem • kubek Władcy Sieci"),
        Drink("ESPRESSO", "ESPRESSO", "czarna kawa • filiżanka"),
        Drink("CAPPUCCINO", "CAPPUCCINO", "kawa z mlekiem • filiżanka"),
        Drink("KAWKAO", "KAWKAO", "kawa z kakao • kubek Władcy Sieci"),
        Drink("MILK_KAWKAO", "Mleczne KAWKAO", ""),
        Drink("KAWKAO_MINI", "KAWKAO MINI", "kawa z kakao • filiżanka"),
        Drink("MILK_KAWKAO_MINI", "Mleczne KAWKAO MINI", "")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFF8EF),
                        Color(0xFFEBC799),
                        Color(0xFFB57A45)
                    )
                )
            )
            .padding(18.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.minutesSinceMeal != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MealTimerCard(
                        minutesSinceMeal = state.minutesSinceMeal,
                        onCancelMeal = { confirmCancelMeal = true }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeColors.Brown,
                        contentColor = Color.White
                    ),
                    onClick = { confirmMeal = true }
                ) {
                    Text("Zjadłem posiłek \uD83C\uDF7D\uFE0F", fontSize = 25.sp)
                }
            }

            if (message.isNotBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { message = "" },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CoffeeColors.Success,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            message,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "── ⋆⋅ Menu ⋅⋆ ──",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 50.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    color = CoffeeColors.Dark,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x663B2418),
                            offset = Offset(1.5f, 2.5f),
                            blurRadius = 3f
                        )
                    )
                )
            }

            items(drinks) { drink ->
                CoffeeTile(drink = drink) {
                    scope.launch {
                        val (order, warn) = Api.order(drink.type, false)

                        if (warn != null) {
                            pendingCoffee = drink.type
                            warning = warn
                        } else {
                            message = "Zamówiono: ${order?.coffeeName ?: drink.title}"
                            refresh()
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Dzisiejsze zamówienia",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoffeeColors.Dark
                )
            }

            items(state.orders, span = { GridItemSpan(maxLineSpan) }) { order ->
                OrderCard(order)
            }
        }
    }

    if (confirmMeal) {
        AlertDialog(
            onDismissRequest = { confirmMeal = false },
            containerColor = Color(0xFFFFF7EC),
            titleContentColor = Color(0xFF2A1810),
            textContentColor = Color(0xFF2A1810),
            title = { Text("Potwierdź posiłek") },
            text = { Text("Czy na pewno zjadłeś posiłek? Od tego zacznie się odliczanie 60 minut.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeColors.Brown,
                        contentColor = Color.White
                    ),
                    onClick = {
                        confirmMeal = false
                        scope.launch {
                            Api.meal()
                            message = "Zapisano posiłek"
                            refresh()
                        }
                    }
                ) {
                    Text("Tak, zjadłem")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmMeal = false }) {
                    Text("Anuluj", color = CoffeeColors.Brown)
                }
            }
        )
    }

    if (confirmCancelMeal) {
        AlertDialog(
            onDismissRequest = { confirmCancelMeal = false },
            containerColor = Color(0xFFFFF1DC),
            titleContentColor = CoffeeColors.Danger,
            textContentColor = CoffeeColors.Dark,
            title = { Text("Anulować posiłek?") },
            text = {
                Text(
                    "Licznik zostanie wyzerowany. Użyj tego tylko wtedy, gdy posiłek został zapisany przez pomyłkę."
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeColors.Danger,
                        contentColor = Color.White
                    ),
                    onClick = {
                        confirmCancelMeal = false
                        scope.launch {
                            runCatching { Api.cancelMeal() }
                                .onSuccess {
                                    message = "Anulowano posiłek"
                                    refresh()
                                }
                                .onFailure {
                                    message = "Nie udało się anulować posiłku"
                                    refresh()
                                }
                        }
                    }
                ) {
                    Text("Anuluj posiłek")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelMeal = false }) {
                    Text("Zostaw licznik", color = CoffeeColors.Brown)
                }
            }
        )
    }

    warning?.let { w ->
        AlertDialog(
            onDismissRequest = { warning = null },
            containerColor = Color(0xFFFFF1DC),
            titleContentColor = CoffeeColors.Danger,
            textContentColor = CoffeeColors.Dark,
            title = { Text("To może być za wcześnie") },
            text = {
                Text(
                    "Od posiłku minęło ${w.minutesSinceMeal} min.\n" +
                            "Do zalecanej kawy zostało ${w.minutesLeft} min.\n\n" +
                            "Zamówić mimo to?"
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeColors.Warning,
                        contentColor = Color.White
                    ),
                    onClick = {
                        val type = pendingCoffee
                        warning = null
                        if (type != null) {
                            scope.launch {
                                Api.order(type, true)
                                message = "Zamówiono mimo ostrzeżenia"
                                refresh()
                            }
                        }
                    }
                ) {
                    Text("Zamów mimo to")
                }
            },
            dismissButton = {
                TextButton(onClick = { warning = null }) {
                    Text("Anuluj", color = CoffeeColors.Brown)
                }
            }
        )
    }
}

data class Drink(
    val type: String,
    val title: String,
    val subtitle: String
)

fun tileColor(type: String): Color =
    when (type) {
        "AMERICANO" -> Color(0xFF3B2418)
        "LATTE" -> Color(0xFF8A5A2F)
        "ESPRESSO" -> Color(0xFF24140E)
        "CAPPUCCINO" -> Color(0xFFA86F3B)
        "KAWKAO" -> Color(0xFF6B3A1E)
        "MILK_KAWKAO" -> Color(0xFF9C6B3F)
        "KAWKAO_MINI" -> Color(0xFF7A4728)
        "MILK_KAWKAO_MINI" -> Color(0xFFC07B3C)
        else -> CoffeeColors.Espresso
    }

@Composable
fun CoffeeTile(
    drink: Drink,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(185.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = tileColor(drink.type),
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CoffeeGlyph(drink.type)

            Column {
                Text(
                    drink.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 25.sp
                )

                Spacer(modifier = Modifier.height(0.dp))

                Text(
                    drink.subtitle,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFFFE8C7),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CoffeeGlyph(type: String) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val cream = Color(0xFFF4E1C1)
        val coffee = Color(0xFF1F120C)
        val caramel = Color(0xFFC78A4A)
        val milk = Color(0xFFEAD6B8)

        val isMini = type.contains("MINI") || type == "ESPRESSO" || type == "CAPPUCCINO"
        val hasMilk = type == "LATTE" || type == "CAPPUCCINO" || type.contains("MILK")
        val hasCocoa = type.contains("KAWKAO")

        val cupWidth = if (isMini) size.width * 0.58f else size.width * 0.74f
        val cupHeight = if (isMini) size.height * 0.44f else size.height * 0.52f
        val left = (size.width - cupWidth) / 2f
        val top = size.height * 0.23f

        drawRoundRect(
            color = cream,
            topLeft = Offset(left, top),
            size = Size(cupWidth, cupHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f)
        )

        drawOval(
            color = if (hasMilk) milk else coffee,
            topLeft = Offset(left + 9f, top - 9f),
            size = Size(cupWidth - 18f, cupHeight * 0.46f)
        )

        drawOval(
            color = if (hasMilk) Color(0xFFF8E7CB) else Color(0xFF3A2116),
            topLeft = Offset(left + 18f, top - 3f),
            size = Size(cupWidth - 36f, cupHeight * 0.28f)
        )

        if (hasMilk) {
            drawOval(
                color = caramel,
                topLeft = Offset(left + cupWidth * 0.25f, top + 1f),
                size = Size(cupWidth * 0.50f, cupHeight * 0.22f),
                style = Stroke(width = 5.5f)
            )
        }

        if (hasCocoa) {
            repeat(7) { i ->
                drawCircle(
                    color = Color(0xFF6F3F22),
                    radius = 4.4f,
                    center = Offset(
                        left + cupWidth * (0.20f + i * 0.10f),
                        top + cupHeight * 0.11f
                    )
                )
            }
        }

        if (!isMini) {
            drawCircle(
                color = cream,
                radius = 17f,
                center = Offset(left + cupWidth + 13f, top + cupHeight * 0.43f),
                style = Stroke(width = 8f)
            )
        }

        drawOval(
            color = Color(0x553B2418),
            topLeft = Offset(size.width * 0.17f, top + cupHeight + 13f),
            size = Size(size.width * 0.66f, 13f)
        )
    }
}

@Composable
fun MealTimerCard(
    minutesSinceMeal: Int?,
    onCancelMeal: () -> Unit
) {
    val context = LocalContext.current

    var played by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var showAlarmDialog by remember { mutableStateOf(false) }

    fun stopAlarm() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            player.release()
        }
        mediaPlayer = null
        showAlarmDialog = false
    }

    fun startAlarm() {
        stopAlarm()

        mediaPlayer = MediaPlayer.create(context, R.raw.alarm).apply {
            isLooping = true
            start()
        }
        showAlarmDialog = true
    }

    val minutesLeft = when {
        minutesSinceMeal == null -> null
        minutesSinceMeal < 60 -> 60 - minutesSinceMeal
        else -> 0
    }

    val coffeeTime = if (minutesLeft != null && minutesLeft > 0) {
        LocalTime.now()
            .plusMinutes(minutesLeft.toLong())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } else null

    LaunchedEffect(minutesLeft) {
        if (minutesLeft == 0 && !played) {
            played = true
            startAlarm()
        }

        if (minutesLeft != null && minutesLeft > 0) {
            played = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopAlarm()
        }
    }

    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = {
                stopAlarm()
            },
            title = {
                Text(
                    "Można pić kawę ☕",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Dzielnie wytrwałeś do zakończenia odliczania. Alarm gra — kliknij przycisk poniżej, żeby go wyłączyć.",
                    fontSize = 18.sp
                )
            },
            confirmButton = {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeColors.Danger,
                        contentColor = Color.White
                    ),
                    onClick = {
                        stopAlarm()
                    }
                ) {
                    Text("🔕 Wyłącz alarm", fontSize = 24.sp)
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (minutesSinceMeal != null) {
                    Modifier.clickable { onCancelMeal() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF1DC),
            contentColor = CoffeeColors.Dark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                minutesLeft != null && minutesLeft > 0 -> {
                    Text("Kawa zalecana za", fontSize = 20.sp)
                    Text(
                        "$minutesLeft min",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoffeeColors.Brown
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (minutesSinceMeal ?: 0).coerceIn(0, 60) / 60f },
                            color = CoffeeColors.Success,
                            trackColor = Color(0xFFE8D8C3),
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                        )

                        Text(
                            text = coffeeTime ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoffeeColors.Success
                        )
                    }
                    Text("Od posiłku minęło: $minutesSinceMeal min", fontSize = 17.sp)

                    Text(
                        "Kliknij kartę, aby anulować zapisany posiłek",
                        fontSize = 15.sp,
                        color = CoffeeColors.Danger
                    )
                }

                else -> {
                    Text(
                        "Można pić kawę ☕",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoffeeColors.Success
                    )
                    Text("Od posiłku minęło: $minutesSinceMeal min", fontSize = 17.sp)

                    Text(
                        "Kliknij kartę, aby anulować zapisany posiłek",
                        fontSize = 15.sp,
                        color = CoffeeColors.Danger
                    )

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoffeeColors.Danger,
                            contentColor = Color.White
                        ),
                        onClick = {
                            stopAlarm()
                        }
                    ) {
                        Text("🔕 Wyłącz alarm", fontSize = 24.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun OrderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF7EC),
            contentColor = CoffeeColors.Dark
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "${order.createdAt.substring(11, 16)} — ${order.coffeeName}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                statusText(order.status),
                fontSize = 18.sp
            )
        }
    }
}

fun statusText(status: String): String =
    when (status) {
        "NOWE" -> "Oczekuje ⏳"
        "PRZYJETE" -> "Przyjęte \uD83D\uDCE9"
        "ROBIE" -> "Robię ⚙\uFE0F"
        "GOTOWE" -> "Gotowe \uD83C\uDF89"
        "ZAMKNIETE" -> "Zamknięte \uD83D\uDCE6"
        else -> status
    }
