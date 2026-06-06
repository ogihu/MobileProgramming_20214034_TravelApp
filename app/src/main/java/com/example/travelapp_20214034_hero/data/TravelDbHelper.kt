package com.example.travelapp_20214034_hero.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TravelDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRAVEL (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PLACE TEXT NOT NULL,
                $COL_VISIT_DATE TEXT NOT NULL,
                $COL_MEMO TEXT,
                $COL_PHOTO_URI TEXT,
                $COL_LATITUDE REAL,
                $COL_LONGITUDE REAL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            onCreate(db)
        }
    }

    fun insertTravel(item: TravelItem): Long {
        val values = contentValuesFrom(item)
        return writableDatabase.insert(TABLE_TRAVEL, null, values)
    }

    fun getAllTravels(sortDescending: Boolean = true): List<TravelItem> {
        val order = if (sortDescending) "DESC" else "ASC"
        val sql = "SELECT * FROM $TABLE_TRAVEL ORDER BY $COL_VISIT_DATE $order, $COL_ID $order"
        return queryList(sql, null)
    }

    fun getTravelsWithLocation(): List<TravelItem> {
        val sql =
            "SELECT * FROM $TABLE_TRAVEL WHERE $COL_LATITUDE IS NOT NULL AND $COL_LONGITUDE IS NOT NULL"
        return queryList(sql, null)
    }

    fun getTravelById(id: Long): TravelItem? {
        val sql = "SELECT * FROM $TABLE_TRAVEL WHERE $COL_ID = ?"
        return readableDatabase.rawQuery(sql, arrayOf(id.toString())).use { cursor ->
            if (cursor.moveToFirst()) cursorToTravel(cursor) else null
        }
    }

    fun updateTravel(item: TravelItem): Int {
        val values = contentValuesFrom(item)
        return writableDatabase.update(
            TABLE_TRAVEL,
            values,
            "$COL_ID = ?",
            arrayOf(item.id.toString())
        )
    }

    fun deleteTravel(id: Long): Int {
        return writableDatabase.delete(
            TABLE_TRAVEL,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteAllTravels(): Int {
        return writableDatabase.delete(TABLE_TRAVEL, null, null)
    }

    private fun queryList(sql: String, args: Array<String>?): List<TravelItem> {
        return readableDatabase.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursorToTravel(cursor))
                }
            }
        }
    }

    private fun contentValuesFrom(item: TravelItem): ContentValues {
        return ContentValues().apply {
            put(COL_PLACE, item.place)
            put(COL_VISIT_DATE, item.visitDate)
            put(COL_MEMO, item.memo)
            put(COL_PHOTO_URI, item.photoUri)
            if (item.latitude != null && item.longitude != null) {
                put(COL_LATITUDE, item.latitude)
                put(COL_LONGITUDE, item.longitude)
            } else {
                putNull(COL_LATITUDE)
                putNull(COL_LONGITUDE)
            }
        }
    }

    private fun cursorToTravel(cursor: android.database.Cursor): TravelItem {
        val latIndex = cursor.getColumnIndexOrThrow(COL_LATITUDE)
        val lngIndex = cursor.getColumnIndexOrThrow(COL_LONGITUDE)
        val lat = if (cursor.isNull(latIndex)) null else cursor.getDouble(latIndex)
        val lng = if (cursor.isNull(lngIndex)) null else cursor.getDouble(lngIndex)
        val photoIndex = cursor.getColumnIndexOrThrow(COL_PHOTO_URI)
        val photo = if (cursor.isNull(photoIndex)) null else cursor.getString(photoIndex)

        return TravelItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            place = cursor.getString(cursor.getColumnIndexOrThrow(COL_PLACE)),
            visitDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_VISIT_DATE)),
            memo = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEMO)) ?: "",
            photoUri = photo,
            latitude = lat,
            longitude = lng
        )
    }

    companion object {
        @Volatile
        private var instance: TravelDbHelper? = null

        fun getInstance(context: Context): TravelDbHelper {
            return instance ?: synchronized(this) {
                instance ?: TravelDbHelper(context).also { instance = it }
            }
        }

        const val DATABASE_NAME = "travel_app.db"
        const val DATABASE_VERSION = 1
        const val TABLE_TRAVEL = "travel"
        const val COL_ID = "id"
        const val COL_PLACE = "place"
        const val COL_VISIT_DATE = "visit_date"
        const val COL_MEMO = "memo"
        const val COL_PHOTO_URI = "photo_uri"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
    }
}
