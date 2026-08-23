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

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsGateway {

    private const val logTag = "SmsGateway"

    fun send(context: Context, phoneNumber: String, text: String): Boolean = try {
        val manager = smsManager(context)
        val parts = manager.divideMessage(text)
        if (parts.size > 1) {
            manager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        } else {
            manager.sendTextMessage(phoneNumber, null, text, null, null)
        }
        true
    } catch (e: Exception) {
        Log.e(logTag, "No se pudo enviar el SMS a $phoneNumber", e)
        false
    }

    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
}
