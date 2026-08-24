/**
 * Copyright (C) 2020 Safety Tracker
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

package app.llegue

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.llegue.sessions.SessionScheduler

class BootReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent?) {
        if (LlegueForegroundService.ping(context)) {
            Log.d(TAG, "Service started after device boot up")
        } else {
            Log.d(TAG, "Service NOT started after device boot up")
        }

        val pending = goAsync()
        Background.execute {
            try {
                SessionScheduler.rescheduleAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

private const val TAG = "BootReceiver"