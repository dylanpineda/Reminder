package com.example.postitapp.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.postitapp.data.TodoItem
import com.example.postitapp.data.TodoRepository

class ListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ListRemoteViewsFactory(applicationContext)
    }
}

class ListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TodoItem> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        items = TodoRepository.getInstance(context).todoItems.value
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= items.size) return null

        val item = items[position]

        val itemLayoutResId = context.resources.getIdentifier("widget_item_layout", "layout", context.packageName)
        val textResId = context.resources.getIdentifier("widget_item_text", "id", context.packageName)
        val checkboxResId = context.resources.getIdentifier("widget_item_checkbox", "id", context.packageName)
        val rootResId = context.resources.getIdentifier("widget_item_root", "id", context.packageName)
        val deleteResId = context.resources.getIdentifier("widget_item_delete", "id", context.packageName)

        val checkedDrawableResId = context.resources.getIdentifier("ic_checkbox_checked", "drawable", context.packageName)
        val uncheckedDrawableResId = context.resources.getIdentifier("ic_checkbox_unchecked", "drawable", context.packageName)

        val rv = RemoteViews(context.packageName, itemLayoutResId)
        rv.setTextViewText(textResId, item.text)

        if (item.isChecked) {
            rv.setImageViewResource(checkboxResId, checkedDrawableResId)
            rv.setTextColor(textResId, 0xFFA0A0A0.toInt())
        } else {
            rv.setImageViewResource(checkboxResId, uncheckedDrawableResId)
            rv.setTextColor(textResId, 0xFFFFFFFF.toInt())
        }

        // Action 1: Toggle task completion
        val toggleFillInIntent = Intent().apply {
            putExtra(EXTRA_ITEM_ID, item.id)
            putExtra(EXTRA_WIDGET_ACTION, "toggle")
        }
        rv.setOnClickFillInIntent(rootResId, toggleFillInIntent)

        // Action 2: Delete task
        val deleteFillInIntent = Intent().apply {
            putExtra(EXTRA_ITEM_ID, item.id)
            putExtra(EXTRA_WIDGET_ACTION, "delete")
        }
        rv.setOnClickFillInIntent(deleteResId, deleteFillInIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= items.size) return 0L
        return items[position].id.hashCode().toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
