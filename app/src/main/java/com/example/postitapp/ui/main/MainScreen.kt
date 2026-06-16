package com.example.postitapp.ui.main

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.postitapp.data.TodoItem
import com.example.postitapp.data.ColorPalette

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toArgb

val PresetPalettes = listOf(
    ColorPalette("Negro", Color(0xFF000000), Color(0xFF2C2C2C), Color(0xFFFFFFFF)),
    ColorPalette("Azul", Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFFFFFFFF)),
    ColorPalette("Verde", Color(0xFF112211), Color(0xFF1A3D2B), Color(0xFFFFFFFF)),
    ColorPalette("Rojo", Color(0xFF240A0A), Color(0xFF4A1A1A), Color(0xFFFFFFFF)),
    ColorPalette("Púrpura", Color(0xFF160A24), Color(0xFF381A4A), Color(0xFFFFFFFF)),
    ColorPalette("Naranja", Color(0xFF24140A), Color(0xFF4A2B1A), Color(0xFFFFFFFF))
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(context.applicationContext as Application)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPalette by viewModel.selectedPalette.collectAsStateWithLifecycle()
    val customPalette by viewModel.customPalette.collectAsStateWithLifecycle()

    val backgroundModifier = if (selectedPalette.name == "Negro") {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF3B0000), Color(0xFF000000))
            )
        )
    } else {
        Modifier.background(selectedPalette.background)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().then(backgroundModifier),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Post It List",
                        style = MaterialTheme.typography.titleLarge,
                        color = selectedPalette.textColor
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = selectedPalette.chip
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                MainScreenUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is MainScreenUiState.Success -> {
                    val items = (state as MainScreenUiState.Success).items
                    MainScreenContent(
                        items = items,
                        selectedPalette = selectedPalette,
                        customPalette = customPalette,
                        onPaletteChange = { viewModel.updatePalette(it) },
                        onCustomPaletteChange = { viewModel.updateCustomPalette(it) },
                        onToggle = { viewModel.toggleItem(it) },
                        onDelete = { viewModel.deleteItem(it) },
                        onAdd = { viewModel.addItem(it) },
                        onEdit = { id, text -> viewModel.editItem(id, text) },
                        onItemClick = onItemClick
                    )
                }
                is MainScreenUiState.Error -> {
                    Text(
                        "Error: ${(state as MainScreenUiState.Error).throwable.message}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    items: List<TodoItem>,
    selectedPalette: ColorPalette,
    customPalette: ColorPalette,
    onPaletteChange: (ColorPalette) -> Unit,
    onCustomPaletteChange: (ColorPalette) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onItemClick: (NavKey) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<TodoItem?>(null) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar tarea", color = Color.White) },
            text = { Text("¿Deseas eliminar la tarea \"${itemToDelete?.text}\"?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDelete(it.id) }
                        itemToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = selectedPalette.chip
        )
    }

    if (showColorPickerDialog) {
        val initialBgHsv = remember(customPalette) { getHSV(customPalette.background) }
        var bgHue by remember { mutableStateOf(initialBgHsv[0]) }
        var bgSat by remember { mutableStateOf(initialBgHsv[1]) }
        var bgVal by remember { mutableStateOf(initialBgHsv[2]) }

        val initialChipHsv = remember(customPalette) { getHSV(customPalette.chip) }
        var chipHue by remember { mutableStateOf(initialChipHsv[0]) }
        var chipSat by remember { mutableStateOf(initialChipHsv[1]) }
        var chipVal by remember { mutableStateOf(initialChipHsv[2]) }

        val initialTextHsv = remember(customPalette) { getHSV(customPalette.textColor) }
        var textHue by remember { mutableStateOf(initialTextHsv[0]) }
        var textSat by remember { mutableStateOf(initialTextHsv[1]) }
        var textVal by remember { mutableStateOf(initialTextHsv[2]) }

        var activeTab by remember { mutableStateOf(0) } // 0: Fondo, 1: Tarjeta, 2: Texto

        val pickerHue = when (activeTab) {
            0 -> bgHue
            1 -> chipHue
            else -> textHue
        }
        val pickerSaturation = when (activeTab) {
            0 -> bgSat
            1 -> chipSat
            else -> textSat
        }
        val pickerValue = when (activeTab) {
            0 -> bgVal
            1 -> chipVal
            else -> textVal
        }

        val onHueChange: (Float) -> Unit = { newVal ->
            when (activeTab) {
                0 -> bgHue = newVal
                1 -> chipHue = newVal
                else -> textHue = newVal
            }
        }
        val onSatChange: (Float) -> Unit = { newVal ->
            when (activeTab) {
                0 -> bgSat = newVal
                1 -> chipSat = newVal
                else -> textSat = newVal
            }
        }
        val onValChange: (Float) -> Unit = { newVal ->
            when (activeTab) {
                0 -> bgVal = newVal
                1 -> chipVal = newVal
                else -> textVal = newVal
            }
        }

        val previewBg = Color.hsv(bgHue, bgSat, bgVal)
        val previewChip = Color.hsv(chipHue, chipSat, chipVal)
        val previewText = Color.hsv(textHue, textSat, textVal)

        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("Personalizar Color", color = Color.White) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Target selection buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Fondo", "Tarjeta", "Texto").forEachIndexed { index, label ->
                            val isSelected = activeTab == index
                            TextButton(
                                onClick = { activeTab = index },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    // Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(previewBg)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(previewChip)
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Text("Ejemplo de Tarea", color = previewText)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hue Spectrum Picker
                    HueSpectrumPicker(
                        hue = pickerHue,
                        onHueChange = onHueChange,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Preset Hues Row
                    val presetHues = listOf(0f, 35f, 60f, 120f, 180f, 220f, 280f, 320f)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White preset circle
                        val isWhiteSelected = pickerSaturation == 0f && pickerValue == 1f
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    onSatChange(0f)
                                    onValChange(1f)
                                }
                                .border(
                                    width = if (isWhiteSelected) 2.dp else 0.dp,
                                    color = if (isWhiteSelected) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                        )

                        presetHues.forEach { presetHue ->
                            val color = Color.hsv(presetHue, pickerSaturation, pickerValue)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onHueChange(presetHue) }
                                    .border(
                                        width = if (pickerHue == presetHue) 2.dp else 0.dp,
                                        color = if (pickerHue == presetHue) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Saturation (Intensidad) Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Intensidad", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Slider(
                            value = pickerSaturation,
                            onValueChange = onSatChange,
                            valueRange = 0f..1f
                        )
                    }

                    // Brightness (Brillo) Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Brillo", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Slider(
                            value = pickerValue,
                            onValueChange = onValChange,
                            valueRange = 0f..1f
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCustomPaletteChange(
                            ColorPalette(
                                name = "Personalizado",
                                background = previewBg,
                                chip = previewChip,
                                textColor = previewText
                            )
                        )
                        showColorPickerDialog = false
                    }
                ) {
                    Text("Guardar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = selectedPalette.chip
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Color Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PresetPalettes.forEach { palette ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.chip)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPaletteChange(palette) }
                        .border(
                            width = if (selectedPalette == palette) 2.dp else 0.dp,
                            color = if (selectedPalette == palette) Color.White else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    if (palette.name == "Negro") {
                                        listOf(Color(0xFF3B0000), Color(0xFF000000))
                                    } else {
                                        listOf(palette.background, palette.background)
                                    }
                                )
                            )
                    )
                }
            }

            // Custom palette circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(customPalette.chip)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPaletteChange(customPalette) }
                    .border(
                        width = if (selectedPalette == customPalette) 2.dp else 0.dp,
                        color = if (selectedPalette == customPalette) Color.White else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(customPalette.background)
                )
            }

            // Customize '+' button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E2E34))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showColorPickerDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Personalizar color",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.7f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay tareas pendientes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    TodoItemRow(
                        item = item,
                        selectedPalette = selectedPalette,
                        onToggle = { onToggle(item.id) },
                        onDelete = { itemToDelete = item },
                        onEdit = { newText -> onEdit(item.id, newText) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(selectedPalette.chip, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Nueva tarea...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = selectedPalette.textColor,
                    unfocusedTextColor = selectedPalette.textColor
                ),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) {
                        onAdd(text)
                        text = ""
                    }
                }),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text)
                        text = ""
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar tarea"
                )
            }
        }
    }
}

@Composable
fun TodoItemRow(
    item: TodoItem,
    selectedPalette: ColorPalette,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(item.text) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar tarea", color = selectedPalette.textColor) },
            text = {
                TextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = selectedPalette.textColor,
                        unfocusedTextColor = selectedPalette.textColor
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank()) {
                            onEdit(editText)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Guardar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = selectedPalette.chip
        )
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(selectedPalette.chip)
                .combinedClickable(
                    onClick = { onToggle() },
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = item.text,
                modifier = Modifier.weight(1f),
                color = if (item.isChecked) Color.Gray else selectedPalette.textColor,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(selectedPalette.chip)
        ) {
            DropdownMenuItem(
                text = { Text("Editar", color = selectedPalette.textColor) },
                onClick = {
                    showMenu = false
                    showEditDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}
