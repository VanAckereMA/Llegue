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
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import app.llegue.contacts.ContactsController
import app.llegue.data.Sms
import app.llegue.settings.RSTPreferences
import app.llegue.timer.model.ForceSmsModel

class ControlSmsReceiver : BroadcastReceiver() {
    private val logTag = "ControlSmsReceiver"
    private val actionSmsReceived: String = "android.provider.Telephony.SMS_RECEIVED"

    override fun onReceive(context: Context, intent: Intent?) {
        if (!RSTForegroundService.started) {
            Log.i(logTag, "SMS received, but app is turned off")
            return
        }
        intent?.action?.let { action ->
            if (actionSmsReceived.compareTo(action, true) == 0) {
                val whoCanRequestLocation = RSTPreferences.whoCanRequestLocation(context)
                val messages = getMessagesFromIntent(intent)
                Log.i(logTag, "Received sms messages: " + messages.map { message ->
                    "[adr:${message?.originatingAddress}, msg:${message?.messageBody}]"
                })

                val destinationContacts = whoCanRequestLocation.validator.getAllSmsDestinations(
                        context = context,
                        smsMessages = messages,
                        trustedContacts = ContactsController.loadAllContacts()
                )
                if (destinationContacts.isNotEmpty()) {
                    ForceSmsModel.forceSendLocation(
                            context = context,
                            smsType = Sms.Type.LOCATION_FOR_CODE_WORD,
                            contacts = destinationContacts,
                            listener = null
                    )
                }
            }
        }
    }

    @SuppressLint("DeprecatedApi")
    private fun getMessagesFromIntent(intent: Intent): Array<SmsMessage?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } else {
            val pdus = intent.getSerializableExtra("pdus") as Array<*>
            val messages = arrayOfNulls<SmsMessage>(pdus.size)
            for (index in messages.indices) {
                val pdu = pdus[index] as? ByteArray?
                messages[index] = SmsMessage.createFromPdu(pdu)
            }
            messages
        }
    }
}