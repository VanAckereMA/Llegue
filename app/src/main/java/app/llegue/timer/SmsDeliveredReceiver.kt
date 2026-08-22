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

package app.llegue.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import io.realm.Realm
import app.llegue.RSTSystem
import app.llegue.data.Sms
import app.llegue.timer.model.ForceSmsModel
import app.llegue.timer.model.ScheduledSmsModel
import app.llegue.R

/**
 * This receiver is used to process sms sending result.
 * It updates [Sms.status] and [Sms.sentTimestamp] and schedules next sms sending
 */
class SmsDeliveredReceiver : BroadcastReceiver() {
    companion object {
        const val actionDeliverySms = "app.llegue.timer.SmsDeliveredReceiver.actionDeliverySms"
        const val smsIdKey = "app.llegue.timer.SmsDeliveredReceiver.smsIdKey"
        const val taskIdKey = "app.llegue.timer.SmsDeliveredReceiver.taskIdKey"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null && actionDeliverySms == intent.action) {
            val realm = Realm.getDefaultInstance()
            val smsId = intent.getStringExtra(smsIdKey) ?: return
            val sms = realm.where(Sms::class.java).equalTo("id", smsId).findFirst()
            if (sms == null) {
                Log.e(ScheduledSmsModel.logTag, "Failed to get sms for deliveredSmsId=$smsId")
            } else {
                Log.d(ScheduledSmsModel.logTag, "Received sms status intent")
                val smsStatus = Sms.Status.fromResultCode(resultCode)
                realm.executeTransaction {
                    sms.sentTimestamp = RSTSystem.currentTimeMillis()
                    sms.setStatus(smsStatus)
                }
                if (smsStatus == Sms.Status.FAILED_TO_SEND) {
                    Toast.makeText(context, context.getString(R.string.sms_error_message), Toast.LENGTH_SHORT).show()
                }
                ForceSmsModel.notifyTask(context, intent.getStringExtra(taskIdKey), sms)
                // schedule next sms sending for scheduled sms
                if (sms.getType() == Sms.Type.LOCATION_SCHEDULED) {
                    ScheduledSmsModel.startSmsSending(context)
                }
            }
        }
    }
}