package com.netify.app.domain

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Builds and parses standard "WIFI:" QR payloads
 * (WIFI:S:<ssid>;T:<WPA|WEP|nopass>;P:<password>;;), the same format used
 * by Android's own Settings > WiFi > Share QR feature and read by every
 * stock camera / QR app on the market.
 */
object QrHelper {

    data class ParsedWifiQr(val ssid: String, val password: String, val security: String)

    fun buildWifiQrContent(ssid: String, password: String, security: String = "WPA"): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:")
        val type = if (password.isBlank()) "nopass" else security
        return "WIFI:S:${esc(ssid)};T:$type;P:${esc(password)};;"
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bmp
    }

    fun parseWifiQr(raw: String): ParsedWifiQr? {
        if (!raw.startsWith("WIFI:")) return null
        val body = raw.removePrefix("WIFI:").removeSuffix(";;")
        val fields = splitUnescaped(body)
        var ssid = ""; var pass = ""; var type = "WPA"
        for (f in fields) {
            val idx = f.indexOf(':')
            if (idx == -1) continue
            val key = f.substring(0, idx)
            val value = unescape(f.substring(idx + 1))
            when (key) {
                "S" -> ssid = value
                "P" -> pass = value
                "T" -> type = value
            }
        }
        if (ssid.isBlank()) return null
        return ParsedWifiQr(ssid, pass, type)
    }

    private fun splitUnescaped(s: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var escaped = false
        for (c in s) {
            when {
                escaped -> { cur.append(c); escaped = false }
                c == '\\' -> escaped = true
                c == ';' -> { out.add(cur.toString()); cur.clear() }
                else -> cur.append(c)
            }
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out
    }

    private fun unescape(s: String): String =
        s.replace("\\;", ";").replace("\\,", ",").replace("\\:", ":").replace("\\\\", "\\")
}
