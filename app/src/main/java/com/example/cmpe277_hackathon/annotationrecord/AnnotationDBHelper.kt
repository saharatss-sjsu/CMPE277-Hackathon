package com.example.cmpe277_hackathon.annotationrecord

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log

import com.example.cmpe277_hackathon.annotationrecord.AnnotationRecordContract.EconomicEntry as DBEntry


class AnnotationDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "AnnotationDb.db"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${DBEntry.TABLE_NAME}"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE ${AnnotationRecordContract.EconomicEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${AnnotationRecordContract.EconomicEntry.COLUMN_NAME_COUNTRY} TEXT," +
                "${AnnotationRecordContract.EconomicEntry.COLUMN_NAME_YEAR} TEXT," +
                "${AnnotationRecordContract.EconomicEntry.COLUMN_NAME_INDICATOR} TEXT," +
                "${AnnotationRecordContract.EconomicEntry.COLUMN_NAME_CONTENT} TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun writeRecord(record: AnnotationRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(DBEntry.COLUMN_NAME_COUNTRY, record.country)
            put(DBEntry.COLUMN_NAME_YEAR, record.year)
            put(DBEntry.COLUMN_NAME_INDICATOR, record.indicator)
            put(DBEntry.COLUMN_NAME_CONTENT, record.content)
        }
        return db.insert(DBEntry.TABLE_NAME, null, values)
    }

    fun readAllRecords(): MutableList<AnnotationRecord> {
        val db = readableDatabase
        val projection = arrayOf(
            BaseColumns._ID,
            DBEntry.COLUMN_NAME_COUNTRY,
            DBEntry.COLUMN_NAME_YEAR,
            DBEntry.COLUMN_NAME_INDICATOR,
            DBEntry.COLUMN_NAME_CONTENT
        )

        val cursor = db.query(
            DBEntry.TABLE_NAME, // The table to query
            projection,    // The array of columns to return (pass null to get all)
            null,  // The columns for the WHERE clause (null means no WHERE clause, so all rows are returned)
            null,  // The values for the WHERE clause
            null,  // don't group the rows
            null,  // don't filter by row groups
            null   // The sort order (ascending or descending)
        )


        val items = mutableListOf<AnnotationRecord>()
        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(BaseColumns._ID))
                items.add(AnnotationRecord(
                    getString(getColumnIndexOrThrow(DBEntry.COLUMN_NAME_COUNTRY)),
                    getString(getColumnIndexOrThrow(DBEntry.COLUMN_NAME_YEAR)),
                    getString(getColumnIndexOrThrow(DBEntry.COLUMN_NAME_INDICATOR)),
                    getString(getColumnIndexOrThrow(DBEntry.COLUMN_NAME_CONTENT))
                ))
            }
        }
        cursor.close()

        return items
    }

    fun deleteRecord(annotation: AnnotationRecord) {
        val db = writableDatabase
        val selection = "${DBEntry.COLUMN_NAME_COUNTRY} = ? AND ${DBEntry.COLUMN_NAME_YEAR} = ? AND ${DBEntry.COLUMN_NAME_INDICATOR} = ?"
        val selectionArgs = arrayOf(annotation.country, annotation.year, annotation.indicator)
        val deletedRows = db.delete(DBEntry.TABLE_NAME, selection, selectionArgs)

        if (deletedRows > 0) {
            Log.d("main", "SQLite delete: Successfully $deletedRows records")
        } else {
            Log.d("main", "SQLite delete: No records deleted")
        }
    }

}
