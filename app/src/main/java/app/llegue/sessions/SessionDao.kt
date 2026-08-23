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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {

    @Insert
    fun insert(session: Session): Long

    @Update
    fun update(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    fun delete(id: Long)

    @Query("SELECT * FROM sessions WHERE active = 1 ORDER BY startedAt DESC")
    fun active(): List<Session>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun all(): List<Session>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun byId(id: Long): Session?

    @Query("UPDATE sessions SET active = 0 WHERE id = :id")
    fun close(id: Long)

    @Query("UPDATE sessions SET active = :active WHERE id = :id")
    fun setActive(id: Long, active: Boolean)

    @Query("SELECT * FROM sessions WHERE active = 1 AND intervalMinutes IS NOT NULL")
    fun activeWithInterval(): List<Session>

    @Query("UPDATE sessions SET lastLatitude = :lat, lastLongitude = :lng, lastLocationAt = :at WHERE id = :id")
    fun saveLocation(id: Long, lat: Double, lng: Double, at: Long)

    @Query("UPDATE sessions SET nextSendAt = :at WHERE id = :id")
    fun setNextSendAt(id: Long, at: Long?)

    @Query("UPDATE sessions SET consecutiveGpsMisses = :misses, reusedLocation = :reused WHERE id = :id")
    fun setGpsStatus(id: Long, misses: Int, reused: Boolean)
}
