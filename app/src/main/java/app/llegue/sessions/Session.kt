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

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(

        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,

        val name: String,

        val contactName: String,

        val contactPhone: String,

        val codeWord: String,

        /** Minutos entre envios automaticos, o null si la sesion no tiene intervalo. */
        val intervalMinutes: Int?,

        val startedAt: Long,

        /** Momento en que la sesion termina sola, o null si queda abierta. */
        val endsAt: Long?,

        val active: Boolean = true,

        val lastLatitude: Double? = null,

        val lastLongitude: Double? = null,

        val lastLocationAt: Long? = null,

        /** Proximo envio automatico, o null si la sesion no tiene intervalo. */
        val nextSendAt: Long? = null,

        /** Fallos seguidos de GPS al momento de enviar. A 3 se cierra la sesion. */
        val consecutiveGpsMisses: Int = 0,

        /** Ya se aviso al contacto que se reutilizo la ultima ubicacion. */
        val reusedLocation: Boolean = false
)
