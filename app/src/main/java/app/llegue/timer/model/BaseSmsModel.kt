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

package app.llegue.timer.model

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.llegue.RSTSmsManager
import app.llegue.data.Sms
import app.llegue.timer.SmsDeliveredReceiver

open class BaseSmsModel {

    companion object {
        /**
         * Creates an sms status intent that will be received by [SmsDeliveredReceiver].
         * It contains an id of sms to retrieve it from the storage. And also sends the result sms.
         *
         * @param sms should be managed by realm. This is used to later get it from the storage in [SmsDeliveredReceiver]
         * @param taskId is used for force sms sending to notify the listener
         */
        fun performLocationSmsSending(context: Context, sms: Sms, taskId: String? = null) {
            if (!sms.isManaged) {
                throw IllegalArgumentException("Sms should be managed by realm")
            }
            val intent = Intent(context, SmsDeliveredReceiver::class.java)
            intent.action = SmsDeliveredReceiver.actionDeliverySms
            intent.putExtra(SmsDeliveredReceiver.smsIdKey, sms.id)
            if (!taskId.isNullOrEmpty()) {
                intent.putExtra(SmsDeliveredReceiver.taskIdKey, taskId)
            }

            val contact = sms.destinationContact
            if (contact != null) {
                RSTSmsManager.get().sendTextMessage(context, contact.phone, null, sms.text, PendingIntent.getBroadcast(
                        context.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT), null)
            }
        }
    }
}