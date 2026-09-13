package com.netify.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.netify.app.MainActivity
import com.netify.app.NetifyApplication
import com.netify.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home screen widget showing the last speed test result. Uses classic
 * RemoteViews (not Glance) for maximum compatibility with older launchers.
 * Tapping the widget opens the app to run a fresh test.
 */
class NetifyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_netify)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        views.setTextViewText(R.id.widget_value, "--")
        views.setTextViewText(R.id.widget_label, "Tap to test")
        manager.updateAppWidget(widgetId, views)

        val app = context.applicationContext as NetifyApplication
        CoroutineScope(Dispatchers.IO).launch {
            val latest = app.container.database.speedTestDao().getLatest()
            val refreshed = RemoteViews(context.packageName, R.layout.widget_netify)
            refreshed.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            if (latest != null) {
                refreshed.setTextViewText(R.id.widget_value, "%.1f".format(latest.downloadMbps))
                refreshed.setTextViewText(R.id.widget_label, "Mbps · last test")
            } else {
                refreshed.setTextViewText(R.id.widget_value, "--")
                refreshed.setTextViewText(R.id.widget_label, "No tests yet")
            }
            manager.updateAppWidget(widgetId, refreshed)
        }
    }
}
