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

package app.llegue.sessions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.llegue.Background

class SessionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val sessionId = intent?.getLongExtra(EXTRA_SESSION_ID, -1L) ?: return
        if (sessionId < 0) return
        val app = context.applicationContext
        when (intent.action) {
            ACTION_PREPARE -> Background.execute { SessionCycle.prepare(app, sessionId) }
            ACTION_SEND -> Background.execute { SessionCycle.send(app, sessionId) }
        }
    }

    companion object {
        const val ACTION_PREPARE = "app.llegue.sessions.PREPARE"
        const val ACTION_SEND = "app.llegue.sessions.SEND"
        const val EXTRA_SESSION_ID = "session_id"
    }
}
