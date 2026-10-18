package androidx.room

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Entity(val tableName: String = "")
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class PrimaryKey(val autoGenerate: Boolean = false)
@Target(AnnotationTarget.CLASS)
annotation class Dao
@Target(AnnotationTarget.FUNCTION)
annotation class Query(val value: String)
@Target(AnnotationTarget.FUNCTION)
annotation class Insert(val onConflict: Int = 0)
object OnConflictStrategy { const val REPLACE: Int = 1 }
@Target(AnnotationTarget.CLASS)
annotation class Database(val entities: Array<KClass<*>>, val version: Int, val exportSchema: Boolean = true)
abstract class RoomDatabase
