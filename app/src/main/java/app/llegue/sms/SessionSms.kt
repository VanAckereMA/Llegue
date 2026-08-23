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

package app.llegue.sms

import android.location.Location
import app.llegue.sessions.Session
import java.util.Locale

object SessionSms {

    const val NO_LOCATION = "No se pudo obtener la ubicacion"
    const val REUSED_LOCATION = "ubicacion reutilizada (sin senal GPS)"

    /**
     * Arma el mensaje del frame: clave, link al mapa, proximo envio y bateria.
     * La linea del proximo envio se omite en las sesiones sin intervalo.
     */
    fun compose(
            session: Session,
            location: Location?,
            batteryLevel: Int,
            reusedLocation: Boolean = false
    ): String {
        val lines = mutableListOf<String>()
        lines += "Clave: ${session.codeWord}"
        lines += location?.let { mapsLink(it) } ?: NO_LOCATION
        if (reusedLocation) lines += REUSED_LOCATION
        nextSendMinutes(session)?.let { lines += "siguiente envio en $it min" }
        lines += "batería $batteryLevel%"
        return lines.joinToString("\n")
    }

    private fun mapsLink(location: Location): String = String.format(
            Locale.US,
            "maps.google.com/?q=%.6f,%.6f",
            location.latitude,
            location.longitude)

    private fun nextSendMinutes(session: Session): Int? {
        val interval = session.intervalMinutes ?: return null
        val sendAt = session.nextSendAt ?: return interval
        val remainingMs = sendAt - System.currentTimeMillis()
        if (remainingMs <= 30_000L) return interval
        return ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    }
}
