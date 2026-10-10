package pe.edu.gowayki.prototype.reportcapture

import androidx.compose.material.icons.outlined.Build
import androidx.core.content.ContextCompat
import android.os.Looper
import android.location.LocationManager
import android.location.LocationListener
import android.location.Location
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Terracotta = Color(0xFFBC5433)
private val BlueGray = Color(0xFF8E9DB0)
private val Charcoal = Color(0xFF282828)
private val Background = Color(0xFFF7F6F3)
private val CardWhite = Color(0xFFFFFFFF)
private val Muted = Color(0xFF777773)
private val Border = Color(0xFFE8E4DE)
private val SoftTerracotta = Color(0xFFF8EAE5)
private val SoftBlueGray = Color(0xFFEDF0F4)
private val SuccessGreen = Color(0xFF36765A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Terracotta,
                    onPrimary = Color.White,
                    background = Background,
                    surface = CardWhite,
                    onSurface = Charcoal,
                    secondary = BlueGray
                )
            ) {
                GoWaykiReportScreen()
            }
        }
    }
}

@Composable
private fun GoWaykiReportScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var category by remember { mutableStateOf("Vía dañada") }
    var description by remember { mutableStateOf("") }

    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var hasLocation by remember { mutableStateOf(false) }
    var locationStatus by remember {
        mutableStateOf("Aún no has compartido tu ubicación.")
    }
    var latitudeInput by remember { mutableStateOf("") }
    var longitudeInput by remember { mutableStateOf("") }
    var showEditLocationDialog by remember { mutableStateOf(false) }

    fun updateLocation(status: String, found: Boolean) {
        locationStatus = status
        hasLocation = found
        submitted = false

        if (found && status.startsWith("Ubicación obtenida: ")) {
            val parts = status.substringAfter("Ubicación obtenida: ").split(",")
            if (parts.size == 2) {
                latitudeInput = parts[0].trim()
                longitudeInput = parts[1].trim()
            }
        }
    }

    val categories = listOf(
        "Vía dañada",
        "Accidente",
        "Obras en la vía",
        "Seguridad",
        "Transporte",
        "Otro"
    )


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            requestCurrentLocation(context) { status, found ->
                updateLocation(status, found)
            }
        } else {
            locationStatus = "Permiso de ubicación denegado. Puedes habilitarlo en Ajustes."
            hasLocation = false
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            photo = bitmap
            submitted = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    photo = bitmap
                    submitted = false
                }
            }
        }
    }

    if (showEditLocationDialog) {
        AlertDialog(
            onDismissRequest = { showEditLocationDialog = false },
            title = { Text("Editar ubicación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Introduce las coordenadas del incidente para esta demostración.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = latitudeInput,
                        onValueChange = { latitudeInput = it },
                        label = { Text("Latitud") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longitudeInput,
                        onValueChange = { longitudeInput = it },
                        label = { Text("Longitud") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val lat = latitudeInput.trim().toDoubleOrNull()
                        val lon = longitudeInput.trim().toDoubleOrNull()

                        if (lat == null || lon == null ||
                            lat !in -90.0..90.0 || lon !in -180.0..180.0
                        ) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Introduce una latitud y longitud válidas."
                                )
                            }
                        } else {
                            hasLocation = true
                            locationStatus = "Ubicación editada: $lat, $lon"
                            submitted = false
                            showEditLocationDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLocationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Background,
        bottomBar = {
            Surface(
                color = CardWhite,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            when {
                                !hasLocation -> scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Primero ubica tu posición actual."
                                    )
                                }
                                category in listOf("Vía dañada", "Accidente", "Obras en la vía") && photo == null ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Adjunta una fotografía para esta categoría."
                                        )
                                    }
                                else -> {
                                    submitted = true
                                    scope.launch {
                                        snackbarHostState.showSnackbar("¡Reporte enviado!")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Terracotta
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Registrar reporte",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Tu voz contribuye a mejorar la ciudad",
                        modifier = Modifier.fillMaxWidth(),
                        color = Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(5.dp))

            // Identidad de marca
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Terracotta),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "G",
                        color = Color.White,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "GoWayki",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Charcoal
                    )
                    Text(
                        "Participación ciudadana",
                        fontSize = 12.sp,
                        color = Muted
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SoftTerracotta
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Terracotta,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "AREQUIPA",
                            color = Terracotta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Encabezado principal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "HAGAMOS UNA MEJOR CIUDAD",
                    color = Terracotta,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "¿Qué necesita\nmejorar cerca de ti?",
                    color = Charcoal,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    lineHeight = 36.sp
                )
                Text(
                    "Comparte la ubicación del problema y añade una fotografía cuando la categoría lo requiera.",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }

            // Ubicación aproximada
            SectionHeader(
                number = "01",
                title = "Ubicación",
                subtitle = "Comparte tu posición actual"
            )

            CardSurface {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    MapIllustration("Centro Histórico")
                    Text(
                        text = "Mapa ilustrativo; tu ubicación se obtiene del dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Text(
                        locationStatus,
                        color = if (hasLocation) SuccessGreen else Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!fineGranted && !coarseGranted) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                requestCurrentLocation(context) { status, found ->
                                    updateLocation(status, found)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Terracotta
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Mi ubicación",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showEditLocationDialog = true
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Text(
                            "Editar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    }


                }
            }

            // Categorías
            SectionHeader(
                number = "02",
                title = "Etiqueta y comentario",
                subtitle = "Selecciona una categoría y describe lo ocurrido"
            )

            CardSurface {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    categories.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            rowItems.forEach { catName ->
                                val selected = category == catName
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) SoftTerracotta else Background
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) Terracotta else Border,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            category = catName
                                            submitted = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 13.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) Terracotta else SoftBlueGray
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategoryIcon(
                                            category = catName,
                                            selected = selected
                                        )
                                    }
                                    Text(
                                        catName,
                                        color = if (selected) Terracotta else Charcoal,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Comentario
            Text(
                "Comentario (opcional)",
                color = Charcoal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    submitted = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Ej. Hay un bache en la vía que dificulta el tránsito...",
                        fontSize = 13.sp,
                        color = Muted
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracotta,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedTextColor = Charcoal,
                    unfocusedTextColor = Charcoal
                )
            )

            if (category in listOf("Vía dañada", "Accidente", "Obras en la vía")) {
            // Evidencia fotográfica
            SectionHeader(
                number = "03",
                title = "Evidencia fotográfica",
                subtitle = if (category in listOf("Vía dañada", "Accidente", "Obras en la vía")) "Fotografía obligatoria para esta categoría" else "Fotografía opcional para esta categoría"
            )

            CardSurface {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (photo != null) {
                        Box {
                            Image(
                                bitmap = photo!!.asImageBitmap(),
                                contentDescription = "Fotografía del reporte",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                                color = Charcoal.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Foto adjunta",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { photo = null; submitted = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = Charcoal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Quitar fotografía",
                                    color = Charcoal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Background)
                                .border(
                                    width = 1.dp,
                                    color = Border,
                                    shape = RoundedCornerShape(15.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(SoftTerracotta),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddAPhoto,
                                        contentDescription = null,
                                        tint = Terracotta,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    "Añade una fotografía",
                                    color = Charcoal,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Captura el problema o elige una imagen",
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Terracotta)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = null,
                                        tint = Terracotta,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Cámara",
                                        color = Terracotta,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Charcoal
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Galería",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            }

            // Confirmación local
            if (submitted) {
                CardSurface {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5F1E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "¡Reporte enviado!",
                                color = Charcoal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tu participación ayuda a visibilizar las necesidades de nuestra ciudad.",
                                color = Muted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SoftTerracotta
                                ) {
                                    Text(
                                        category,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Terracotta,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun requestCurrentLocation(
    context: android.content.Context,
    onResult: (String, Boolean) -> Unit
) {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!fineGranted && !coarseGranted) {
        onResult("Falta conceder el permiso de ubicación.", false)
        return
    }

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (manager == null) {
        onResult("No se pudo acceder al servicio de ubicación.", false)
        return
    }

    try {
        val provider = when {
            fineGranted && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            else -> null
        }

        if (provider == null) {
            onResult("Activa la ubicación del dispositivo e inténtalo de nuevo.", false)
            return
        }

        onResult("Buscando ubicación actual…", false)

        manager.requestSingleUpdate(
            provider,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onResult(
                        "Ubicación obtenida: ${location.latitude}, ${location.longitude}",
                        true
                    )
                }

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) {
                    onResult("La ubicación se desactivó. Inténtalo nuevamente.", false)
                }

                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?
                ) = Unit
            },
            Looper.getMainLooper()
        )
    } catch (_: SecurityException) {
        onResult("Android no permitió acceder a la ubicación.", false)
    } catch (_: Exception) {
        onResult("No se pudo obtener la ubicación. Inténtalo nuevamente.", false)
    }
}

@Composable
private fun CategoryIcon(category: String, selected: Boolean) {
    val image = when (category) {
        "Vía dañada" -> Icons.Outlined.Warning
        "Accidente" -> Icons.Outlined.Warning
        "Obras en la vía" -> Icons.Outlined.Build
        "Seguridad" -> Icons.Outlined.Security
        "Transporte" -> Icons.Outlined.DirectionsBus
        else -> Icons.Outlined.MoreHoriz
    }

    Icon(
        imageVector = image,
        contentDescription = category,
        tint = if (selected) Color.White else Charcoal,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
private fun SectionHeader(
    number: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(35.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(SoftTerracotta),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = Terracotta,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }

        Column(
            modifier = Modifier.padding(top = 1.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                title,
                color = Charcoal,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun CardSurface(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun MapIllustration(zone: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFE9EDE8))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Manzanas urbanas
            val blocks = listOf(
                listOf(0.04f, 0.08f, 0.27f, 0.27f),
                listOf(0.36f, 0.04f, 0.60f, 0.25f),
                listOf(0.69f, 0.07f, 0.94f, 0.27f),
                listOf(0.05f, 0.41f, 0.24f, 0.66f),
                listOf(0.36f, 0.38f, 0.59f, 0.62f),
                listOf(0.71f, 0.42f, 0.95f, 0.65f),
                listOf(0.05f, 0.76f, 0.28f, 0.96f),
                listOf(0.39f, 0.75f, 0.60f, 0.95f),
                listOf(0.72f, 0.76f, 0.94f, 0.96f)
            )

            blocks.forEachIndexed { index, b ->
                drawRoundRect(
                    color = if (index == 4) Color(0xFFD4E1D0) else Color(0xFFF8F7F1),
                    topLeft = androidx.compose.ui.geometry.Offset(w * b[0], h * b[1]),
                    size = androidx.compose.ui.geometry.Size(
                        w * (b[2] - b[0]),
                        h * (b[3] - b[1])
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }

            // Vías principales
            drawLine(
                color = Color(0xFFCFD5D1),
                start = androidx.compose.ui.geometry.Offset(0f, h * 0.34f),
                end = androidx.compose.ui.geometry.Offset(w, h * 0.34f),
                strokeWidth = 12f
            )
            drawLine(
                color = Color(0xFFCFD5D1),
                start = androidx.compose.ui.geometry.Offset(w * 0.31f, 0f),
                end = androidx.compose.ui.geometry.Offset(w * 0.31f, h),
                strokeWidth = 11f
            )
            drawLine(
                color = Color(0xFFCFD5D1),
                start = androidx.compose.ui.geometry.Offset(w * 0.65f, 0f),
                end = androidx.compose.ui.geometry.Offset(w * 0.65f, h),
                strokeWidth = 9f
            )
            drawLine(
                color = Color(0xFFCFD5D1),
                start = androidx.compose.ui.geometry.Offset(0f, h * 0.70f),
                end = androidx.compose.ui.geometry.Offset(w, h * 0.70f),
                strokeWidth = 8f
            )

            // Ruta destacada
            val route = Path().apply {
                moveTo(w * 0.14f, h * 0.86f)
                lineTo(w * 0.31f, h * 0.70f)
                lineTo(w * 0.31f, h * 0.34f)
                lineTo(w * 0.49f, h * 0.34f)
                lineTo(w * 0.65f, h * 0.17f)
            }
            drawPath(
                path = route,
                color = BlueGray.copy(alpha = 0.8f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }

        // Pin del mapa
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 13.dp, y = (-7).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Terracotta)
                    .border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(8.dp)
                    .background(Terracotta)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            color = Color.White.copy(alpha = 0.95f),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Terracotta,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    zone,
                    color = Charcoal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "UBICACIÓN",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            color = Charcoal.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
