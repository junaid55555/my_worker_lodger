package com.fitnessmarket.myworkerlodger.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fitnessmarket.myworkerlodger.domain.HousingStore
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId

/** The on-device [HousingStore] (ADR-0001): one SQLite database file, included in Android backup. */
class SqliteHousingStore(context: Context) : HousingStore {

    private val helper = object : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE property (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    override fun properties(): List<Property> =
        helper.readableDatabase.query("property", arrayOf("id", "name"), null, null, null, null, null)
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(Property(PropertyId(cursor.getString(0)), cursor.getString(1)))
                    }
                }
            }

    override fun saveProperty(property: Property) {
        val values = ContentValues().apply {
            put("id", property.id.value)
            put("name", property.name)
        }
        helper.writableDatabase.insertWithOnConflict("property", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    companion object {
        const val DATABASE_NAME = "housing.db"
    }
}
