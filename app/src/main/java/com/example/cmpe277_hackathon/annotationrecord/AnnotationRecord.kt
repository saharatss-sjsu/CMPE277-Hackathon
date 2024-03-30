package com.example.cmpe277_hackathon.annotationrecord

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns


class AnnotationRecord(val country:String, val year: String, val indicator: String, var content: String)

object AnnotationRecordContract {
    object EconomicEntry : BaseColumns {
        const val TABLE_NAME = "annotation_economic"
        const val COLUMN_NAME_COUNTRY   = "country"
        const val COLUMN_NAME_YEAR      = "year"
        const val COLUMN_NAME_INDICATOR = "indicator"
        const val COLUMN_NAME_CONTENT   = "content"
    }

    object AgriculturalEntry : BaseColumns {
        const val TABLE_NAME = "annotation_agricultural"
        const val COLUMN_NAME_COUNTRY   = "country"
        const val COLUMN_NAME_YEAR      = "year"
        const val COLUMN_NAME_INDICATOR = "indicator"
        const val COLUMN_NAME_CONTENT   = "content"
    }

    object DebtEntry : BaseColumns {
        const val TABLE_NAME = "annotation_debt"
        const val COLUMN_NAME_COUNTRY   = "country"
        const val COLUMN_NAME_YEAR      = "year"
        const val COLUMN_NAME_INDICATOR = "indicator"
        const val COLUMN_NAME_CONTENT   = "content"
    }
}
