package com.nononsenseapps.feeder.model.opml

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nononsenseapps.feeder.db.room.AppDatabase
import com.nononsenseapps.feeder.db.room.Feed
import com.nononsenseapps.feeder.model.requestFeedSync
import com.nononsenseapps.feeder.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * Exports OPML on a background thread
 */
suspend fun exportOpml(appContext: Context, uri: Uri) {
    try {
        val time = measureTimeMillis {
            appContext.contentResolver.openOutputStream(uri)?.let {
                writeOutputStream(it,
                        tags(appContext),
                        feedsWithTags(appContext))
            }
        }
        Log.d("OPML", "Exported OPML in $time ms on ${Thread.currentThread().name}")
    } catch (e: Throwable) {
        e.printStackTrace()
        Log.e("OMPL", "Failed to export OMPL: $e")
        withContext(Dispatchers.Main) {
            appContext.makeToast("Failed to export OMPL")
        }
    }
}

/**
 * Imports OPML on a background thread
 */
suspend fun importOpml(context: Context, uri: Uri) {
    val appContext = context.applicationContext
    val db = AppDatabase.getInstance(context)
    try {
        val time = measureTimeMillis {
            val parser = OpmlParser(OPMLToRoom(db))
            appContext.contentResolver.openInputStream(uri).use {
                it?.let { stream ->
                    parser.parseInputStream(stream)
                }
            }
            requestFeedSync(parallell = true, ignoreConnectivitySettings = false)
        }
        Log.d("OPML", "Imported OPML in $time ms on ${Thread.currentThread().name}")
    } catch (e: Throwable) {
        Log.e("OMPL", "Failed to import OMPL: $e")
        withContext(Dispatchers.Main) {
            appContext.makeToast("Failed to import OMPL")
        }
    }
}

private fun tags(context: Context): Iterable<String> =
        AppDatabase.getInstance(context).feedDao().loadTags()

private fun feedsWithTags(context: Context): (String) -> Iterable<Feed> {
    val dao = AppDatabase.getInstance(context).feedDao()
    return { tag ->
        dao.loadFeeds(tag = tag)
    }
}
