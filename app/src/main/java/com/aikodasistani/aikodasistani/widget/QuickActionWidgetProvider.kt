package com.aikodasistani.aikodasistani.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aikodasistani.aikodasistani.MainActivity
import com.aikodasistani.aikodasistani.R

/**
 * AI Kod Asistanı home screen widget provider.
 * Shows a quick action button to open the app with voice input ready.
 */
class QuickActionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val ACTION_OPEN_VOICE = "com.aikodasistani.aikodasistani.ACTION_OPEN_VOICE"
        const val ACTION_OPEN_PLAYGROUND = "com.aikodasistani.aikodasistani.ACTION_OPEN_PLAYGROUND"
        const val ACTION_OPEN_TOOLS = "com.aikodasistani.aikodasistani.ACTION_OPEN_TOOLS"
        const val ACTION_NEW_CHAT = "com.aikodasistani.aikodasistani.ACTION_NEW_CHAT"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_action)

            // Open app with voice input ready
            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_VOICE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val voicePendingIntent = PendingIntent.getActivity(
                context, 0, voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetVoice, voicePendingIntent)

            // Open Code Playground
            val playgroundIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_PLAYGROUND
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val playgroundPendingIntent = PendingIntent.getActivity(
                context, 1, playgroundIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetPlayground, playgroundPendingIntent)

            // Open Developer Tools
            val toolsIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_TOOLS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val toolsPendingIntent = PendingIntent.getActivity(
                context, 2, toolsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetTools, toolsPendingIntent)

            // New Chat
            val newChatIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_NEW_CHAT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val newChatPendingIntent = PendingIntent.getActivity(
                context, 3, newChatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetNewChat, newChatPendingIntent)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
