package com.example.postitapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.app.RemoteInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.input.RemoteInputIntentHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import com.example.postitapp.data.TodoItem
import com.example.postitapp.data.TodoRepository

data class ColorPalette(
    val name: String,
    val background: Color,
    val chip: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TodoRepository.getInstance(this)

        setContent {
            val palettes = remember {
                listOf(
                    ColorPalette("Negro", Color(0xFF000000), Color(0xFF2C2C2C)),
                    ColorPalette("Azul", Color(0xFF0D1B2A), Color(0xFF1B263B)),
                    ColorPalette("Verde", Color(0xFF112211), Color(0xFF1A3D2B)),
                    ColorPalette("Rojo", Color(0xFF240A0A), Color(0xFF4A1A1A)),
                    ColorPalette("Púrpura", Color(0xFF160A24), Color(0xFF381A4A)),
                    ColorPalette("Naranja", Color(0xFF24140A), Color(0xFF4A2B1A))
                )
            }
            var customPalette by remember {
                mutableStateOf(ColorPalette("Personalizado", Color(0xFF0F172A), Color(0xFF1E293B)))
            }
            var selectedPalette by remember { mutableStateOf(palettes[0]) }

            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    background = selectedPalette.background,
                    surface = selectedPalette.chip
                )
            ) {
                Scaffold(
                    timeText = { TimeText() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colors.background)
                    ) {
                        val items by repository.todoItems.collectAsState()
                        WatchScreen(
                            items = items,
                            onToggle = { repository.toggleItem(it) },
                            onAdd = { repository.addItem(it) },
                            onDelete = { repository.deleteItem(it) },
                            onEdit = { id, text -> repository.editItem(id, text) },
                            palettes = palettes,
                            customPalette = customPalette,
                            currentPalette = selectedPalette,
                            onPaletteChange = { selectedPalette = it },
                            onCustomPaletteChange = { newPalette ->
                                customPalette = newPalette
                                selectedPalette = newPalette
                            }
                        )
                    }
                }
            }
        }
    }
}

fun getHSV(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv
}

@Composable
fun HueSpectrumPicker(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF0000), // Red
                        Color(0xFFFFFF00), // Yellow
                        Color(0xFF00FF00), // Green
                        Color(0xFF00FFFF), // Cyan
                        Color(0xFF0000FF), // Blue
                        Color(0xFFFF00FF), // Magenta
                        Color(0xFFFF0000)  // Red
                    )
                )
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val widthDp = maxWidth
        val density = LocalDensity.current
        val thumbSize = 16.dp
        val thumbSizePx = with(density) { thumbSize.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val activeWidth = widthPx - thumbSizePx
                        val relativeX = offset.x - (thumbSizePx / 2f)
                        val fraction = (relativeX / activeWidth).coerceIn(0f, 1f)
                        onHueChange(fraction * 360f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val activeWidth = widthPx - thumbSizePx
                        val relativeX = change.position.x - (thumbSizePx / 2f)
                        val fraction = (relativeX / activeWidth).coerceIn(0f, 1f)
                        onHueChange(fraction * 360f)
                    }
                }
        )

        val thumbOffset = (widthDp - thumbSize) * (hue / 360f)
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 5.dp)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color.Black, CircleShape)
        )
    }
}

@Composable
fun WatchScreen(
    items: List<TodoItem>,
    onToggle: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    palettes: List<ColorPalette>,
    customPalette: ColorPalette,
    currentPalette: ColorPalette,
    onPaletteChange: (ColorPalette) -> Unit,
    onCustomPaletteChange: (ColorPalette) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<TodoItem?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }

    val initialHsv = remember(customPalette) { getHSV(customPalette.chip) }
    var pickerHue by remember(showColorPicker) { mutableStateOf(initialHsv[0]) }
    var pickerSaturation by remember(showColorPicker) { mutableStateOf(initialHsv[1]) }
    var pickerValue by remember(showColorPicker) { mutableStateOf((initialHsv[2] / 0.55f).coerceIn(0f, 1f)) }

    var itemForMenu by remember { mutableStateOf<TodoItem?>(null) }
    var itemToEdit by remember { mutableStateOf<TodoItem?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            val input = results?.getCharSequence("input_key")?.toString()
            if (!input.isNullOrBlank()) {
                onAdd(input)
            }
        }
    }

    val editLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            val input = results?.getCharSequence("edit_input_key")?.toString()
            if (!input.isNullOrBlank()) {
                itemToEdit?.let { item ->
                    onEdit(item.id, input)
                }
            }
            itemToEdit = null
        }
    }

    // Dialog for Long-press options menu
    Dialog(
        showDialog = (itemForMenu != null),
        onDismissRequest = { itemForMenu = null }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = itemForMenu?.text ?: "Opciones",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Chip(
                onClick = {
                    val item = itemForMenu
                    itemForMenu = null
                    if (item != null) {
                        itemToEdit = item
                        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        val remoteInputs = listOf(
                            RemoteInput.Builder("edit_input_key")
                                .setLabel(item.text)
                                .build()
                        )
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        editLauncher.launch(intent)
                    }
                },
                label = { Text("Editar texto") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Editar") },
                colors = ChipDefaults.chipColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Chip(
                onClick = {
                    val item = itemForMenu
                    itemForMenu = null
                    if (item != null) {
                        itemToDelete = item
                    }
                },
                label = { Text("Eliminar", color = MaterialTheme.colors.error) },
                icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colors.error) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = MaterialTheme.colors.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Dialog(
        showDialog = showColorPicker,
        onDismissRequest = { showColorPicker = false }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Personalizar",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center
            )

            // Preview card
            val previewBg = Color.hsv(pickerHue, pickerSaturation, (pickerValue * 0.25f).coerceAtLeast(0.02f))
            val previewChip = Color.hsv(pickerHue, pickerSaturation * 0.8f, (pickerValue * 0.55f).coerceAtLeast(0.12f))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewBg)
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(previewChip)
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ejemplo",
                            style = MaterialTheme.typography.caption2,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Custom Hue Spectrum Picker
            HueSpectrumPicker(
                hue = pickerHue,
                onHueChange = { pickerHue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )

            // Preset hues grid for quick tap selection
            val presetHues = listOf(0f, 35f, 60f, 120f, 180f, 220f, 280f, 320f)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                presetHues.forEach { presetHue ->
                    val color = Color.hsv(presetHue, pickerSaturation, pickerValue)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                pickerHue = presetHue
                            }
                            .then(
                                if (pickerHue == presetHue) {
                                    Modifier.border(1.5.dp, Color.White, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Intensidad",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            InlineSlider(
                value = pickerSaturation,
                onValueChange = { pickerSaturation = it },
                valueRange = 0f..1f,
                steps = 10,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, contentDescription = "Aumentar") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, contentDescription = "Disminuir") },
                modifier = Modifier.fillMaxWidth().height(36.dp)
            )

            Text(
                text = "Brillo",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            InlineSlider(
                value = pickerValue,
                onValueChange = { pickerValue = it },
                valueRange = 0f..1f,
                steps = 10,
                increaseIcon = { Icon(InlineSliderDefaults.Increase, contentDescription = "Aumentar") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, contentDescription = "Disminuir") },
                modifier = Modifier.fillMaxWidth().height(36.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons: Save and Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showColorPicker = false },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF333333)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.caption2)
                }

                Button(
                    onClick = {
                        val newBg = Color.hsv(pickerHue, pickerSaturation, (pickerValue * 0.25f).coerceAtLeast(0.02f))
                        val newChip = Color.hsv(pickerHue, pickerSaturation * 0.8f, (pickerValue * 0.55f).coerceAtLeast(0.12f))
                        onCustomPaletteChange(
                            ColorPalette(
                                name = "Personalizado",
                                background = newBg,
                                chip = newChip
                            )
                        )
                        showColorPicker = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text("Guardar", style = MaterialTheme.typography.caption2)
                }
            }
        }
    }

    Dialog(
        showDialog = (itemToDelete != null),
        onDismissRequest = { itemToDelete = null }
    ) {
        Alert(
            title = { Text("Eliminar", textAlign = TextAlign.Center) },
            negativeButton = {
                Button(onClick = { itemToDelete = null }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cancelar"
                    )
                }
            },
            positiveButton = {
                Button(onClick = {
                    itemToDelete?.let { onDelete(it.id) }
                    itemToDelete = null
                }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmar"
                    )
                }
            }
        ) {
            Text(
                text = itemToDelete?.text ?: "",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2
            )
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader {
                Text(
                    text = "Post It Watch",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                palettes.forEach { palette ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.chip)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPaletteChange(palette) }
                            .then(
                                if (currentPalette == palette) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(palette.background)
                        )
                    }
                }

                // Círculo para la paleta Personalizada
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(customPalette.chip)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPaletteChange(customPalette) }
                        .then(
                            if (currentPalette == customPalette) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(customPalette.background)
                    )
                }

                // Botón '+' para personalizar
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showColorPicker = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Chip(
                onClick = {
                    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    val remoteInputs = listOf(
                        RemoteInput.Builder("input_key")
                            .setLabel("Nueva tarea")
                            .build()
                      )
                      RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                      launcher.launch(intent)
                },
                label = { Text("Nueva tarea") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                colors = ChipDefaults.chipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        if (items.isEmpty()) {
            item {
                Text(
                    text = "No hay tareas\nAgregalas aqui o en el telefono",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp)
                )
            }
        } else {
            items(items, key = { it.id }) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    ToggleChip(
                        checked = item.isChecked,
                        onCheckedChange = { /* No-op: handled by overlay */ },
                        label = {
                            Text(
                                text = item.text,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.body1.copy(
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                        },
                        toggleControl = {
                            ToggleChipDefaults.checkboxIcon(checked = item.isChecked)
                        },
                        colors = ToggleChipDefaults.toggleChipColors(
                            checkedStartBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                            checkedEndBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                            uncheckedStartBackgroundColor = MaterialTheme.colors.surface,
                            uncheckedEndBackgroundColor = MaterialTheme.colors.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        contentPadding = PaddingValues(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.medium)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = { onToggle(item.id) },
                                onLongClick = { itemForMenu = item }
                            )
                    )
                }
            }
        }
    }
}
