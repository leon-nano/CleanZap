package com.limpamidia.app

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.limpamidia.app.data.AudioPlayer
import com.limpamidia.app.data.DateFilter
import com.limpamidia.app.data.MediaCategory
import com.limpamidia.app.data.MediaItem
import com.limpamidia.app.data.WhatsAppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class UiState(
    val treeUri: Uri? = null,
    val loading: Boolean = false,
    val deleting: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val filter: DateFilter = DateFilter.TODAY,
    val available: List<MediaCategory> = emptyList(),
    val items: Map<MediaCategory, List<MediaItem>> = emptyMap(),
    val openCategory: MediaCategory? = null,
    val selected: Set<String> = emptySet(),
) {
    /** Itens da categoria que passam no filtro de data atual. */
    fun visible(category: MediaCategory, today: LocalDate = LocalDate.now()): List<MediaItem> =
        items[category].orEmpty().filter { filter.matches(it.date, today) }
}

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repo = WhatsAppRepository(app)
    val player = AudioPlayer(app)

    private val _state = MutableStateFlow(UiState(treeUri = repo.savedTreeUri()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var lastRefresh = 0L

    init {
        if (_state.value.treeUri != null) refresh()
    }

    // ------------------------------------------------------------ pasta

    fun onFolderPicked(uri: Uri) {
        refreshJob?.cancel()
        repo.saveTreeUri(uri)
        _state.update { UiState(treeUri = uri, filter = it.filter) }
        refresh()
    }

    fun forgetFolder() {
        refreshJob?.cancel()
        player.stop()
        repo.forgetTree()
        _state.update { UiState(filter = it.filter) }
    }

    // ------------------------------------------------------------ varredura

    fun refresh() {
        val tree = _state.value.treeUri ?: return
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val root = repo.findMediaRoot(tree)
                if (root == null) {
                    repo.forgetTree()
                    _state.update {
                        UiState(
                            filter = it.filter,
                            error = "Não encontrei as pastas \"WhatsApp Images\", \"WhatsApp Voice Notes\" etc. " +
                                "dentro da pasta escolhida. Escolha a pasta Media do WhatsApp " +
                                "(Android › media › com.whatsapp › WhatsApp › Media).",
                        )
                    }
                    return@launch
                }
                val folders = repo.categoryFolders(tree, root)
                _state.update { it.copy(available = folders.keys.toList()) }
                for ((category, folderId) in folders) {
                    val list = repo.scan(tree, category, folderId)
                    _state.update { it.copy(items = it.items + (category to list)) }
                }
                lastRefresh = SystemClock.elapsedRealtime()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro ao ler a pasta: ${e.message}") }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    /** Ao voltar para o app, atualiza a lista (chegam mídias novas o tempo todo). */
    fun onResume() {
        if (_state.value.treeUri != null &&
            SystemClock.elapsedRealtime() - lastRefresh > RESUME_REFRESH_MS
        ) {
            refresh()
        }
    }

    // ------------------------------------------------------------ navegação e seleção

    fun setFilter(filter: DateFilter) {
        player.stop()
        _state.update { it.copy(filter = filter, selected = emptySet()) }
    }

    fun openCategory(category: MediaCategory) =
        _state.update { it.copy(openCategory = category, selected = emptySet()) }

    fun closeCategory() {
        player.stop()
        _state.update { it.copy(openCategory = null, selected = emptySet()) }
    }

    fun toggle(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun selectAll(category: MediaCategory) = _state.update { s ->
        s.copy(selected = s.visible(category).map { it.id }.toSet())
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    fun messageShown() = _state.update { it.copy(message = null) }

    // ------------------------------------------------------------ apagar

    fun deleteSelected() {
        val s = _state.value
        val category = s.openCategory ?: return
        if (s.deleting) return
        val targets = s.visible(category).filter { it.id in s.selected }
        if (targets.isEmpty()) return

        player.stop()
        viewModelScope.launch {
            _state.update { it.copy(deleting = true) }
            val result = withContext(Dispatchers.IO) { repo.delete(targets) }
            val freed = Formatter.formatShortFileSize(app, result.bytes)
            val msg = buildString {
                append("${result.deletedIds.size} arquivo(s) apagado(s) · $freed liberados")
                if (result.failed > 0) append(" · ${result.failed} não foi possível apagar")
            }
            _state.update { st ->
                st.copy(
                    items = st.items + (category to st.items[category].orEmpty().filterNot { it.id in result.deletedIds }),
                    selected = st.selected - result.deletedIds,
                    deleting = false,
                    message = msg,
                )
            }
        }
    }

    override fun onCleared() {
        player.stop()
        super.onCleared()
    }

    private companion object {
        const val RESUME_REFRESH_MS = 15_000L
    }
}
