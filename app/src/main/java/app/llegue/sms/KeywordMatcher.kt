/**
 * Copyright (C) 2026 Llegue
 *
 * This file is part of Llegue, derived from Open SMS Locator
 *
 * Llegue is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Llegue is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Llegue. If not, see <https://www.gnu.org/licenses/>.
 */

package app.llegue.sms

import app.llegue.sessions.Session

object KeywordMatcher {

    /**
     * Compara la palabra clave de la sesion con el SMS recibido.
     * Ignora mayusculas, minusculas y espacios alrededor: "Dado", "dado" y " DADO "
     * coinciden, porque el teclado del contacto puede capitalizar solo.
     */
    fun isKeyword(configured: String, incoming: String): Boolean {
        val expected = configured.trim()
        if (expected.isEmpty()) return false
        return expected.equals(incoming.trim(), ignoreCase = true)
    }

    fun findSession(
            sessions: List<Session>,
            fromPhone: String?,
            body: String?,
            samePhone: (String, String) -> Boolean
    ): Session? {
        if (fromPhone.isNullOrBlank() || body == null) return null
        return sessions.firstOrNull { session ->
            session.active &&
                    isKeyword(session.codeWord, body) &&
                    samePhone(session.contactPhone, fromPhone)
        }
    }
}
