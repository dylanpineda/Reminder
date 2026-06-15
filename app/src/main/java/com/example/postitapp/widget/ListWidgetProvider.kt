package com.example.postitapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.example.postitapp.data.TodoRepository
import com.example.postitapp.ConfirmationActivity

const val ACTION_WIDGET_CLICK = "com.example.postitapp.widget.ACTION_WIDGET_CLICK"
const val EXTRA_ITEM_ID = "com.example.postitapp.widget.EXTRA_ITEM_ID"
const val EXTRA_WIDGET_ACTION = "com.example.postitapp.widget.EXTRA_WIDGET_ACTION"

class ListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_CLICK) {
            val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
            val actionType = intent.getStringExtra(EXTRA_WIDGET_ACTION)
            Log.d("ListWidgetProvider", "Received action $actionType for item: $itemId")
            if (itemId != null) {
                val repository = TodoRepository.getInstance(context)
                if (actionType == "delete") {
                    val activityIntent = Intent(context, ConfirmationActivity::class.java).apply {
                        putExtra(EXTRA_ITEM_ID, itemId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(activityIntent)
                } else {
                    repository.toggleItem(itemId)
                }
            }
        }
        super.onReceive(context, intent)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val layoutResId = context.resources.getIdentifier("widget_layout", "layout", context.packageName)
        val listResId = context.resources.getIdentifier("widget_list", "id", context.packageName)
        val emptyResId = context.resources.getIdentifier("widget_empty", "id", context.packageName)

        val views = RemoteViews(context.packageName, layoutResId)

        val serviceIntent = Intent(context, ListWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(listResId, serviceIntent)
        views.setEmptyView(listResId, emptyResId)

        val clickIntent = Intent(context, ListWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_CLICK
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val clickPendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent, flags)
        views.setPendingIntentTemplate(listResId, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
