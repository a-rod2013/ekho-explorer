package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [JournalDraftEntity::class],
    version = 1,
    exportSchema = true,
)

abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDraftDao(): JournalDraftDao

    companion object {
        fun create(context: Context): JournalDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                JournalDatabase::class.java,
                "journal.db",
            ).build()
    }
}
