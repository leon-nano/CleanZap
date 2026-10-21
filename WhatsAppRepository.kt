package com.limpamidia.app.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Acessa a pasta de mídia do WhatsApp usando o Storage Access Framework (SAF):
 * o usuário escolhe a pasta uma única vez e o Android guarda a permissão.
 */
class WhatsAppRepository(context: Context) {

    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val prefs = context.applicationContext.getSharedPreferences("limpa_midia", Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- permissão da pasta

    fun savedTreeUri(): Uri? {
        val saved = prefs.getString(KEY_TREE, null) ?: return null
        val uri = Uri.parse(saved)
        val stillGranted = resolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
        return if (stillGranted) uri else null
    }

    fun saveTreeUri(uri: Uri) {
        try {
            resolver.takePersistableUriPermission(uri, GRANT_FLAGS)
        } catch (_: SecurityException) {
            // segue sem persistir; o app pedirá a pasta de novo na próxima abertura
        }
        prefs.edit().putString(KEY_TREE, uri.toString()).apply()
    }

    fun forgetTree() {
        val saved = prefs.getString(KEY_TREE, null)
        if (saved != null) {
            try {
                resolver.releasePersistableUriPermission(Uri.parse(saved), GRANT_FLAGS)
            } catch (_: Exception) {
            }
        }
        prefs.edit().remove(KEY_TREE).apply()
    }

    // ---------------------------------------------------------------- navegação nas pastas

    private class Child(
        val docId: String,
        val name: String,
        val mime: String?,
        val size: Long,
        val modified: Long,
    ) {
        val isDir: Boolean get() = mime == Document.MIME_TYPE_DIR
    }

    private fun children(tree: Uri, parentDocId: String): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocId)
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
        )
        val out = ArrayList<Child>()
        try {
            resolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    out.add(
                        Child(
                            docId = c.getString(0),
                            name = c.getString(1).orEmpty(),
                            mime = c.getString(2),
                            size = c.getLong(3),
                            modified = c.getLong(4),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // pasta inacessível: devolve o que tiver
        }
        return out
    }

    /**
     * Procura (em largura, até alguns níveis) a pasta que contém "WhatsApp Images",
     * "WhatsApp Voice Notes" etc. Assim funciona mesmo que o usuário escolha a pasta
     * "WhatsApp" ou "com.whatsapp" em vez da "Media".
     */
    fun findMediaRoot(tree: Uri): String? {
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(DocumentsContract.getTreeDocumentId(tree) to 0)
        while (queue.isNotEmpty()) {
            val (docId, depth) = queue.removeFirst()
            val kids = children(tree, docId)
            if (kids.any { it.isDir && isCategoryFolder(it.name) }) return docId
            if (depth < MAX_SEARCH_DEPTH) {
                kids.filter { it.isDir && !it.name.startsWith(".") }
                    .forEach { queue.add(it.docId to depth + 1) }
            }
        }
        return null
    }

    private fun isCategoryFolder(name: String): Boolean =
        name.startsWith("WhatsApp ", ignoreCase = true) && MediaCategory.fromFolderName(name) != null

    fun categoryFolders(tree: Uri, mediaRootId: String): Map<MediaCategory, String> {
        val found = HashMap<MediaCategory, String>()
        for (dir in children(tree, mediaRootId)) {
            if (!dir.isDir || !isCategoryFolder(dir.name)) continue
            val category = MediaCategory.fromFolderName(dir.name) ?: continue
            if (category !in found) found[category] = dir.docId
        }
        return MediaCategory.entries
            .mapNotNull { c -> found[c]?.let { c to it } }
            .toMap()
    }

    // ---------------------------------------------------------------- varredura

    fun scan(tree: Uri, category: MediaCategory, folderDocId: String): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        collect(tree, category, folderDocId, "", 0, out)
        out.sortWith(compareByDescending<MediaItem> { it.sortKey }.thenBy { it.name })
        return out
    }

    private fun collect(
        tree: Uri,
        category: MediaCategory,
        dirDocId: String,
        dirName: String,
        depth: Int,
        out: MutableList<MediaItem>,
    ) {
        for (child in children(tree, dirDocId)) {
            if (child.name.startsWith(".")) continue // .nomedia e afins
            if (child.isDir) {
                if (depth < MAX_SCAN_DEPTH) {
                    collect(tree, category, child.docId, child.name, depth + 1, out)
                }
            } else {
                out.add(
                    MediaItem(
                        id = child.docId,
                        uri = DocumentsContract.buildDocumentUriUsingTree(tree, child.docId),
                        name = child.name,
                        mime = child.mime ?: "application/octet-stream",
                        size = child.size,
                        modified = child.modified,
                        date = dateOf(child.name, child.modified),
                        folder = dirName,
                        category = category,
                    )
                )
            }
        }
    }

    /** Nomes do WhatsApp trazem a data: PTT-20261021-WA0001.opus, IMG-20261021-WA0003.jpg... */
    private fun dateOf(name: String, modified: Long): LocalDate {
        val m = NAME_DATE.find(name)
        if (m != null) {
            try {
                return LocalDate.of(
                    m.groupValues[1].toInt(),
                    m.groupValues[2].toInt(),
                    m.groupValues[3].toInt(),
                )
            } catch (_: Exception) {
                // data inválida no nome: usa a data de modificação
            }
        }
        return Instant.ofEpochMilli(modified).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // ---------------------------------------------------------------- apagar

    class DeleteResult(val deletedIds: Set<String>, val failed: Int, val bytes: Long)

    fun delete(items: List<MediaItem>): DeleteResult {
        val deleted = HashSet<String>()
        var failed = 0
        var bytes = 0L
        for (item in items) {
            val ok = try {
                DocumentsContract.deleteDocument(resolver, item.uri)
            } catch (_: FileNotFoundException) {
                true // já não existe: some da lista
            } catch (_: Exception) {
                false
            }
            if (ok) {
                deleted.add(item.id)
                bytes += item.size
            } else {
                failed++
            }
        }
        return DeleteResult(deleted, failed, bytes)
    }

    private companion object {
        const val KEY_TREE = "tree_uri"
        const val GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        const val MAX_SEARCH_DEPTH = 4
        const val MAX_SCAN_DEPTH = 3
        val NAME_DATE = Regex("""(\d{4})(\d{2})(\d{2})-WA""")
    }
}
