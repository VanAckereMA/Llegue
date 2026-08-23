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

package app.llegue.settings

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import app.llegue.R

object AppTheme {

    private const val PREFS = "llegue_theme"
    private const val KEY_DARK = "dark"

    fun isDark(context: Context): Boolean =
            prefs(context).getBoolean(KEY_DARK, true)

    fun setDark(context: Context, dark: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK, dark).apply()
    }

    fun apply(activity: Activity) {
        activity.setTheme(if (isDark(activity)) R.style.LlegueTheme else R.style.LlegueTheme_Light)
    }

    fun color(context: Context, @AttrRes attr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return value.data
    }

    private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
