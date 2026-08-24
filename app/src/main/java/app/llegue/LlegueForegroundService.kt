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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.llegue.ActiveNotificationController.FOREGROUND_NOTIFICATION_ID
import app.llegue.ActiveNotificationController.createForegroundNotification

class LlegueForegroundService : Service() {

    companion object {

        var started: Boolean = false
            private set

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(buildIntent(context))
            } else {
                context.startService(buildIntent(context))
            }
            started = true
            context.servicePreferences.started = true
        }

        fun stop(context: Context) {
            context.stopService(buildIntent(context))
            started = false
            context.servicePreferences.started = false
        }

        fun ping(context: Context): Boolean {
            if (context.servicePreferences.started) {
                start(context)
                return true
            }
            return false
        }

        private fun buildIntent(context: Context) = Intent(context, LlegueForegroundService::class.java)

        private val Context.servicePreferences: ServicePreferences
            get() {
                val current = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                migrateLegacyPrefs(this, current)
                return ServicePreferences(current)
            }

        private fun migrateLegacyPrefs(context: Context, current: SharedPreferences) {
            if (current.contains(KEY_STARTED)) return
            val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            if (!legacy.contains(KEY_STARTED)) return
            current.edit().putBoolean(KEY_STARTED, legacy.getBoolean(KEY_STARTED, false)).apply()
            legacy.edit().clear().apply()
        }

        private class ServicePreferences(val prefs: SharedPreferences) {

            var started: Boolean
                get() = prefs.getBoolean(KEY_STARTED, false)
                set(value) = prefs.edit().putBoolean(KEY_STARTED, value).apply()

        }
    }

    override fun onBind(intent: Intent?): IBinder {
        throw UnsupportedOperationException()
    }

    override fun onCreate() {
        super.onCreate()
        started = true
    }

    override fun onDestroy() {
        super.onDestroy()
        started = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

}

private const val PREFS_NAME = "LlegueForegroundService"
private const val LEGACY_PREFS_NAME = "RSTForegroundService"
private const val KEY_STARTED = "started"
