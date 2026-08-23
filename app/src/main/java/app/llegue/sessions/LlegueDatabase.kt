/**
 * Copyright (C) 2026 Llegue
 *
 * This file is part of Llegue, derived from Open SMS Locator
 *
 * Llegue is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Llegue is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Llegue. If not, see <https://www.gnu.org/licenses/>.
 */

package app.llegue.sessions

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Session::class], version = 2, exportSchema = false)
abstract class LlegueDatabase : RoomDatabase() {

    abstract fun sessions(): SessionDao

    companion object {

        @Volatile
        private var instance: LlegueDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN lastLatitude REAL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN lastLongitude REAL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN lastLocationAt INTEGER")
                db.execSQL("ALTER TABLE sessions ADD COLUMN nextSendAt INTEGER")
                db.execSQL("ALTER TABLE sessions ADD COLUMN consecutiveGpsMisses INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN reusedLocation INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): LlegueDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LlegueDatabase::class.java,
                    "llegue.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
