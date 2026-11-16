package com.fitnessmarket.myworkerlodger.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fitnessmarket.myworkerlodger.domain.Bed
import com.fitnessmarket.myworkerlodger.domain.BedId
import com.fitnessmarket.myworkerlodger.domain.HousingStore
import com.fitnessmarket.myworkerlodger.domain.Property
import com.fitnessmarket.myworkerlodger.domain.PropertyId
import com.fitnessmarket.myworkerlodger.domain.Room
import com.fitnessmarket.myworkerlodger.domain.RoomId
import com.fitnessmarket.myworkerlodger.domain.Sex

/** The on-device [HousingStore] (ADR-0001): one SQLite database file, included in Android backup. */
class SqliteHousingStore(context: Context) : HousingStore {

    private val helper = object : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_PROPERTY)
            db.execSQL(CREATE_ROOM)
            db.execSQL(CREATE_BED)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL(CREATE_ROOM)
                db.execSQL(CREATE_BED)
            }
        }
    }

    override fun property(id: PropertyId): Property? =
        query("property", "id = ?", id.value) { it.toProperty() }.firstOrNull()

    override fun properties(): List<Property> = query("property") { it.toProperty() }

    override fun saveProperty(property: Property) = replace(
        "property",
        ContentValues().apply {
            put("id", property.id.value)
            put("name", property.name)
        },
    )

    override fun room(id: RoomId): Room? = query("room", "id = ?", id.value) { it.toRoom() }.firstOrNull()

    override fun rooms(propertyId: PropertyId): List<Room> =
        query("room", "property_id = ?", propertyId.value) { it.toRoom() }

    override fun saveRoom(room: Room) = replace(
        "room",
        ContentValues().apply {
            put("id", room.id.value)
            put("property_id", room.propertyId.value)
            put("name", room.name)
            put("sex", room.sex.name)
        },
    )

    override fun bed(id: BedId): Bed? = query("bed", "id = ?", id.value) { it.toBed() }.firstOrNull()

    override fun beds(roomId: RoomId): List<Bed> = query("bed", "room_id = ?", roomId.value) { it.toBed() }

    override fun saveBeds(beds: List<Bed>) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            beds.forEach { bed ->
                replace(
                    "bed",
                    ContentValues().apply {
                        put("id", bed.id.value)
                        put("room_id", bed.roomId.value)
                        put("label", bed.label)
                    },
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun <T> query(table: String, where: String? = null, vararg args: String, read: (Cursor) -> T): List<T> =
        helper.readableDatabase.query(table, null, where, args.takeIf { it.isNotEmpty() }, null, null, null)
            .use { cursor -> buildList { while (cursor.moveToNext()) add(read(cursor)) } }

    private fun replace(table: String, values: ContentValues) {
        helper.writableDatabase.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column))

    private fun Cursor.toProperty() = Property(PropertyId(text("id")), text("name"))

    private fun Cursor.toRoom() =
        Room(RoomId(text("id")), PropertyId(text("property_id")), text("name"), Sex.valueOf(text("sex")))

    private fun Cursor.toBed() = Bed(BedId(text("id")), RoomId(text("room_id")), text("label"))

    companion object {
        const val DATABASE_NAME = "housing.db"
        private const val DATABASE_VERSION = 2

        private const val CREATE_PROPERTY =
            "CREATE TABLE property (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)"
        private const val CREATE_ROOM =
            "CREATE TABLE room (id TEXT PRIMARY KEY NOT NULL, " +
                "property_id TEXT NOT NULL REFERENCES property(id), name TEXT NOT NULL, sex TEXT NOT NULL)"
        private const val CREATE_BED =
            "CREATE TABLE bed (id TEXT PRIMARY KEY NOT NULL, " +
                "room_id TEXT NOT NULL REFERENCES room(id), label TEXT NOT NULL)"
    }
}
