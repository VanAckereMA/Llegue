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

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import io.realm.Realm
import app.llegue.RSTBatteryManager
import app.llegue.data.Sms
import app.llegue.location.LocationCallback
import app.llegue.location.LocationProvider
import app.llegue.onboarding.FormatUtils
import app.llegue.timer.model.BaseSmsModel
import app.llegue.timer.model.ScheduledSmsModel

/**
 * This receiver is posted to [AlarmManager] to schedule sms sending.
 */
class SmsActionSendReceiver : BroadcastReceiver() {

    companion object {
        const val actionSendSms = "app.llegue.timer.SmsActionSendReceiver.actionSendSms"
        const val smsIdsKey = "app.llegue.timer.SmsActionSendReceiver.smsIdsKey"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null && actionSendSms == intent.action) {
            if (!ScheduledSmsModel.isSmsSendingEnabled(context)) {
                return
            }
            val realm = Realm.getDefaultInstance()
            Log.d(ScheduledSmsModel.logTag, "Received send sms intent")
            val smsId = intent.getStringArrayExtra(smsIdsKey) ?: return
            val smsList = realm.where(Sms::class.java).`in`("id", smsId).findAll()
            if (smsList.size == 0) {
                Log.e(ScheduledSmsModel.logTag, "Failed to get sms for deliveredSmsId=$smsId")
            } else {
                LocationProvider.currentLocation(context, object : LocationCallback {
                    override fun onReceivedLocation(location: Location, isLastKnown: Boolean) {
                        val batteryLevel = RSTBatteryManager.getCurrentBatteryLevel(context)
                        val smsText = FormatUtils(context).formatLocationSms(
                            location = location,
                            isLastKnown = isLastKnown,
                            batteryLevel = batteryLevel
                        )
                        smsList.forEach { sms ->
                            realm.executeTransaction { sms.text = smsText }
                            BaseSmsModel.performLocationSmsSending(context, sms)
                        }
                    }

                    override fun onFailedToGetLocation() {
                    }
                })
            }
        }
    }
}