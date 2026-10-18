package app.novafetch.data

import android.content.Context
import androidx.room.Room

object NovaFetchDatabaseFactory {
    fun create(context: Context): NovaFetchDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            NovaFetchDatabase::class.java,
            "novafetch.db",
        ).build()
}
