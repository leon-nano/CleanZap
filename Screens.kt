@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.limpamidia.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.limpamidia.app.MainViewModel
import com.limpamidia.app.UiState
import com.limpamidia.app.data.DateFilter
import com.limpamidia.app.data.MediaCategory
import com.limpamidia.app.data.MediaItem
import com.limpamidia.app.data.whenLabel

// ====================================================================== raiz

@Composable
fun AppRoot(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Carrega miniaturas de fotos e também quadros de vídeos.
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
    }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.messageShown()
        }
    }

    val category = state.openCategory
    when {
        state.treeUri == null -> SetupScreen(state.error, snackbar, vm::onFolderPicked)
        category != null -> {
            BackHandler { vm.closeCategory() }
            CategoryScreen(state, category, snackbar, imageLoader, vm)
        }
        else -> HomeScreen(state, snackbar, vm)
    }
}

// ====================================================================== configuração inicial

@Composable
private fun SetupScreen(error: String?, snackbar: SnackbarHostState, onPicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onPicked(uri)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🧹", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Limpa Mídia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Apague fotos, vídeos e áudios do WhatsApp direto da pasta do celular. " +
                    "A conversa continua intacta: só a mídia some.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(20.dp))
            Text("Como liberar o acesso (só uma vez):", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("1. Toque em “Escolher pasta do WhatsApp”.")
            Text("2. Vá em Android › media › com.whatsapp › WhatsApp › Media.")
            Text("3. Toque em “Usar esta pasta” e depois em “Permitir”.")
            Spacer(Modifier.height(6.dp))
            Text(
                "Em Android 10 ou anterior, a pasta fica na raiz: WhatsApp › Media.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "O app não usa internet e não envia nada para fora do seu celular.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launcher.launch(initialFolderUri(business = false)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Escolher pasta do WhatsApp") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { launcher.launch(initialFolderUri(business = true)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Escolher pasta do WhatsApp Business") }
        }
    }
}

/** Faz o seletor de pastas já abrir perto de onde o WhatsApp guarda a mídia. */
private fun initialFolderUri(business: Boolean): Uri {
    val path = if (Build.VERSION.SDK_INT >= 30) {
        if (business) "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"
        else "Android/media/com.whatsapp/WhatsApp/Media"
    } else {
        if (business) "WhatsApp Business/Media" else "WhatsApp/Media"
    }
    return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:$path")
}

// ====================================================================== início

@Composable
private fun HomeScreen(state: UiState, snackbar: SnackbarHostState, vm: MainViewModel) {
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Limpa Mídia") },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = vm::forgetFolder) {
                        Icon(Icons.Default.Settings, contentDescription = "Trocar pasta do WhatsApp")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
            }
            FilterRow(state.filter, vm::setFilter)

            val error = state.error
            if (error != null) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }

            if (state.available.isEmpty() && !state.loading && error == null) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhuma pasta de mídia encontrada.")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.available, key = { it.name }) { category ->
                    val visible = remember(state.items, state.filter) { state.visible(category) }
                    val summary = if (visible.isEmpty()) {
                        "Nada neste período"
                    } else {
                        val total = Formatter.formatShortFileSize(context, visible.sumOf { it.size })
                        "${visible.size} arquivo(s) · $total"
                    }
                    Card(onClick = { vm.openCategory(category) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(category.emoji, fontSize = 32.sp)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(category.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================== lista de uma categoria

@Composable
private fun CategoryScreen(
    state: UiState,
    category: MediaCategory,
    snackbar: SnackbarHostState,
    imageLoader: ImageLoader,
    vm: MainViewModel,
) {
    val context = LocalContext.current
    val visible = remember(state.items, state.filter, category) { state.visible(category) }
    val selectedItems = remember(visible, state.selected) { visible.filter { it.id in state.selected } }
    val selectedBytes = remember(selectedItems) { selectedItems.sumOf { it.size } }
    val allSelected = visible.isNotEmpty() && selectedItems.size == visible.size
    var confirm by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(category.label) },
                navigationIcon = {
                    IconButton(onClick = vm::closeCategory) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (visible.isNotEmpty()) {
                        TextButton(onClick = { if (allSelected) vm.clearSelection() else vm.selectAll(category) }) {
                            Text(if (allSelected) "Limpar" else "Selecionar tudo")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                    Button(
                        onClick = { confirm = true },
                        enabled = selectedItems.isNotEmpty() && !state.deleting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (selectedItems.isEmpty()) {
                                "Selecione para apagar"
                            } else {
                                "Apagar ${selectedItems.size} (${Formatter.formatShortFileSize(context, selectedBytes)})"
                            }
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading || state.deleting) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
            }
            FilterRow(state.filter, vm::setFilter)
            Spacer(Modifier.height(8.dp))
            Text(
                "${visible.size} arquivo(s) · ${Formatter.formatShortFileSize(context, visible.sumOf { it.size })}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (state.loading) "Procurando…" else "Nada encontrado neste período.")
                }
            } else if (category.visual) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    gridItems(visible, key = { it.id }) { item ->
                        MediaThumb(
                            item = item,
                            selected = item.id in state.selected,
                            imageLoader = imageLoader,
                            onToggle = { vm.toggle(item.id) },
                            onOpen = { openExternally(context, item) },
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(visible, key = { it.id }) { item ->
                        FileRow(
                            item = item,
                            selected = item.id in state.selected,
                            playing = vm.player.playingId == item.id,
                            onToggle = { vm.toggle(item.id) },
                            onPlay = if (category.audio) ({ vm.player.toggle(item) }) else null,
                        )
                    }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Apagar ${selectedItems.size} arquivo(s)?") },
            text = {
                Text(
                    "Serão apagados do celular (${Formatter.formatShortFileSize(context, selectedBytes)}). " +
                        "A conversa e o contexto continuam no WhatsApp. Isso não pode ser desfeito."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    vm.deleteSelected()
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancelar") }
            },
        )
    }
}

// ====================================================================== componentes

@Composable
private fun FilterRow(selected: DateFilter, onSelect: (DateFilter) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

/** Fotos, vídeos e GIFs: miniatura em grade. Toque seleciona; segurar abre no visualizador. */
@Composable
private fun MediaThumb(
    item: MediaItem,
    selected: Boolean,
    imageLoader: ImageLoader,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val sizeText = remember(item.size) { Formatter.formatShortFileSize(context, item.size) }

    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onToggle, onLongClick = onOpen)
    ) {
        AsyncImage(
            model = item.uri,
            imageLoader = imageLoader,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (item.category != MediaCategory.IMAGES) {
            Text(
                "▶",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Text(
            sizeText,
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/** Áudios, mensagens de voz e documentos: linha com checkbox (e botão de tocar, quando for áudio). */
@Composable
private fun FileRow(
    item: MediaItem,
    selected: Boolean,
    playing: Boolean,
    onToggle: () -> Unit,
    onPlay: (() -> Unit)?,
) {
    val context = LocalContext.current
    val sizeText = remember(item.size) { Formatter.formatShortFileSize(context, item.size) }
    val details = "${item.whenLabel()} · $sizeText" + (if (item.sent) " · enviado" else "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        if (onPlay != null) {
            FilledTonalIconButton(onClick = onPlay) {
                Text(if (playing) "⏸" else "▶", fontSize = 18.sp)
            }
        } else {
            Text(
                item.category.emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun openExternally(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(item.uri, item.mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    } catch (_: SecurityException) {
    }
}
