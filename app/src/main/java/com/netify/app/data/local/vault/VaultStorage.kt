package com.netify.app.data.local.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.netify.app.domain.model.VaultEntry
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Netify's own password vault — separate from, and unrelated to, the
 * Android OS's saved-WiFi-credentials store (which no third-party app can
 * read after Android 10, by design). Entries here are only ones the user
 * has explicitly added or scanned into Netify, and are stored with
 * [EncryptedSharedPreferences] (AES-256-GCM, key held in the Android
 * Keystore) so the raw file on disk is not readable even with a rooted
 * device inspecting app-private storage.
 */
class VaultStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "netify_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAll(): List<VaultEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            VaultEntry(
                id = o.getString("id"),
                ssid = o.getString("ssid"),
                password = o.getString("password"),
                security = o.optString("security", "WPA")
            )
        }
    }

    fun add(ssid: String, password: String, security: String = "WPA"): VaultEntry {
        val entry = VaultEntry(UUID.randomUUID().toString(), ssid, password, security)
        val updated = getAll() + entry
        persist(updated)
        return entry
    }

    fun delete(id: String) {
        persist(getAll().filterNot { it.id == id })
    }

    private fun persist(entries: List<VaultEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("ssid", e.ssid); put("password", e.password); put("security", e.security)
            })
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    companion object {
        private const val KEY_ENTRIES = "entries_json"
    }
}
