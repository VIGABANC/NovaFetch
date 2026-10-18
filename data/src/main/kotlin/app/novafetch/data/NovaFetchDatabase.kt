package app.novafetch.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadJobEntity::class, DownloadPartEntity::class, MediaItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NovaFetchDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun mediaLibraryDao(): MediaLibraryDao
}
