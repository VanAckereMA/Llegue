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

package app.llegue.utils

import android.content.Context
import android.content.SharedPreferences

object AppUpdateManager {

    interface AppVersionListener {
        fun onNew()
        fun onUpdate(prevVersionCode: Int, currentVersionCode: Int)
        fun onSame()
    }

    private const val versionCodeKey = "app.llegue.utils.AppUpdateManager.versionCodeKey"

    fun check(context: Context, listener: AppVersionListener?) {
        val prefs = obtainPrefs(context)
        val prevVersion = prefs.getInt(versionCodeKey, -1)
        val currentVersionCode = context.packageManager
                .getPackageInfo(context.packageName, 0).versionCode
        if (prevVersion == -1) {
            listener?.onNew()
        } else {
            if (currentVersionCode == prevVersion) {
                listener?.onSame()
            } else if (currentVersionCode > prevVersion) {
                listener?.onUpdate(prevVersion, currentVersionCode)
            }
        }
        prefs.edit().putInt(versionCodeKey, currentVersionCode).apply()
    }

    private fun obtainPrefs(context: Context): SharedPreferences
            = context.getSharedPreferences("AppUpdateManagerPrefs", Context.MODE_PRIVATE)
}