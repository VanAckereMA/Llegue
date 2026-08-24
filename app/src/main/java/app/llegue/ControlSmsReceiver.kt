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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.util.Log
import app.llegue.sessions.LlegueDatabase
import app.llegue.sessions.SessionCycle
import app.llegue.sessions.SessionScheduler
import app.llegue.sms.KeywordMatcher

class ControlSmsReceiver : BroadcastReceiver() {
    private val logTag = "ControlSmsReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val app = context.applicationContext
        val wakeLock = (app.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "llegue:sms-received")
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire(90_000L)
        try {
            LlegueForegroundService.start(app)
        } catch (e: Exception) {
            Log.w(logTag, "No se pudo iniciar el servicio al recibir SMS", e)
        }
        Background.execute {
            try {
                replyIfKeyword(app, messages)
            } catch (e: Exception) {
                Log.e(logTag, "Error al responder un SMS con palabra clave", e)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                SessionScheduler.syncForegroundService(app)
            }
        }
    }

    private fun replyIfKeyword(context: Context, messages: Array<SmsMessage?>) {
        val sessions = LlegueDatabase.get(context).sessions().active()
        if (sessions.isEmpty()) return

        groupedBodies(messages).forEach { (phone, body) ->
            val session = KeywordMatcher.findSession(sessions, phone, body) { left, right ->
                PhoneNumberUtils.compare(context, left, right)
            }
            if (session == null) {
                Log.i(logTag, "SMS de $phone ignorado: no coincide con una sesion activa")
                return@forEach
            }
            Log.i(logTag, "Palabra clave de sesion ${session.id} recibida de $phone")
            SessionCycle.sendOnDemand(context, session.id)
        }
    }

    private fun groupedBodies(messages: Array<SmsMessage?>): List<Pair<String, String>> =
            messages.filterNotNull()
                    .groupBy { it.originatingAddress.orEmpty() }
                    .filterKeys { it.isNotBlank() }
                    .map { (phone, parts) ->
                        phone to parts.joinToString("") { it.messageBody.orEmpty() }
                    }
}
