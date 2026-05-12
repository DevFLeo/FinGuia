package com.finguia.ui.calculadora

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CalcEntry(
    val id: Long,
    val timestamp: Long,
    val tipo: String,
    val titulo: String,
    val detalhes: String
)

class CalcCacheViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("finguia_calc_prefs", android.content.Context.MODE_PRIVATE)

    private val _ocultas = MutableStateFlow(carregarOcultas())
    val ocultas: StateFlow<Set<String>> = _ocultas.asStateFlow()

    private val _entries = MutableStateFlow(carregarEntries())
    val entries: StateFlow<List<CalcEntry>> = _entries.asStateFlow()

    fun toggleAba(aba: String, oculta: Boolean) {
        val novo = if (oculta) _ocultas.value + aba else _ocultas.value - aba
        _ocultas.value = novo
        prefs.edit().putStringSet("ocultas", novo).apply()
    }

    fun salvar(tipo: String, titulo: String, detalhes: String) {
        val novo = CalcEntry(
            id = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            tipo = tipo,
            titulo = titulo,
            detalhes = detalhes
        )
        val lista = listOf(novo) + _entries.value
        _entries.value = lista
        persistirEntries(lista)
    }

    fun remover(id: Long) {
        val lista = _entries.value.filterNot { it.id == id }
        _entries.value = lista
        persistirEntries(lista)
    }

    fun limparTudo() {
        _entries.value = emptyList()
        prefs.edit().remove("entries").apply()
    }

    private fun carregarOcultas(): Set<String> =
        prefs.getStringSet("ocultas", emptySet()) ?: emptySet()

    private fun carregarEntries(): List<CalcEntry> {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CalcEntry(
                    id = o.optLong("id"),
                    timestamp = o.optLong("ts"),
                    tipo = o.optString("tipo"),
                    titulo = o.optString("titulo"),
                    detalhes = o.optString("detalhes")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun persistirEntries(lista: List<CalcEntry>) {
        val arr = JSONArray()
        lista.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("ts", e.timestamp)
                    .put("tipo", e.tipo)
                    .put("titulo", e.titulo)
                    .put("detalhes", e.detalhes)
            )
        }
        prefs.edit().putString("entries", arr.toString()).apply()
    }
}
