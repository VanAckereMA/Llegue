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

package app.llegue.sessions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import app.llegue.Background
import app.llegue.RSTForegroundService
import app.llegue.timer.model.ScheduledSmsModel
import java.util.concurrent.TimeUnit

object SessionScheduler {

    private const val logTag = "SessionScheduler"

    /** Prompt C: pedir GPS 3 minutos antes del envio programado. */
    val PREPARE_LEAD_MS: Long = TimeUnit.MINUTES.toMillis(3)

    fun afterFirstSend(context: Context, session: Session) {
        val interval = session.intervalMinutes ?: return
        val nextSendAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(interval.toLong())
        if (session.endsAt != null && nextSendAt >= session.endsAt) return
        dao(context).setNextSendAt(session.id, nextSendAt)
        scheduleAlarms(context, session.copy(nextSendAt = nextSendAt))
        syncForegroundService(context)
    }

    fun resume(context: Context, sessionId: Long) {
        val session = dao(context).byId(sessionId) ?: return
        val interval = session.intervalMinutes
        if (interval == null) {
            syncForegroundService(context)
            return
        }
        val now = System.currentTimeMillis()
        val nextSendAt = when {
            session.nextSendAt != null && session.nextSendAt > now -> session.nextSendAt
            else -> now + TimeUnit.MINUTES.toMillis(interval.toLong())
        }
        if (session.endsAt != null && nextSendAt >= session.endsAt) {
            close(context, session.id)
            return
        }
        dao(context).setNextSendAt(session.id, nextSendAt)
        scheduleAlarms(context, session.copy(nextSendAt = nextSendAt, active = true))
        syncForegroundService(context)
    }

    fun pause(context: Context, sessionId: Long) {
        cancelAlarms(context, sessionId)
        dao(context).setNextSendAt(sessionId, null)
        syncForegroundService(context)
    }

    fun close(context: Context, sessionId: Long) {
        cancelAlarms(context, sessionId)
        dao(context).setNextSendAt(sessionId, null)
        dao(context).close(sessionId)
        syncForegroundService(context)
        Log.d(logTag, "Sesion $sessionId cerrada")
    }

    fun delete(context: Context, sessionId: Long) {
        cancelAlarms(context, sessionId)
        dao(context).delete(sessionId)
        syncForegroundService(context)
        Log.d(logTag, "Sesion $sessionId eliminada")
    }

    fun scheduleNext(context: Context, session: Session, fromTime: Long) {
        val interval = session.intervalMinutes ?: return
        val nextSendAt = fromTime + TimeUnit.MINUTES.toMillis(interval.toLong())
        if (session.endsAt != null && nextSendAt >= session.endsAt) {
            close(context, session.id)
            return
        }
        dao(context).setNextSendAt(session.id, nextSendAt)
        scheduleAlarms(context, session.copy(nextSendAt = nextSendAt))
        syncForegroundService(context)
    }

    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        dao(context).activeWithInterval().forEach { session ->
            val nextSendAt = session.nextSendAt ?: return@forEach
            if (session.endsAt != null && now >= session.endsAt) {
                close(context, session.id)
                return@forEach
            }
            if (nextSendAt <= now) {
                SessionCycle.send(context, session.id)
            } else {
                scheduleAlarms(context, session)
            }
        }
        syncForegroundService(context)
    }

    fun prepareLeadMs(intervalMinutes: Int?): Long {
        val intervalMs = TimeUnit.MINUTES.toMillis((intervalMinutes ?: return 0).toLong())
        return minOf(PREPARE_LEAD_MS, (intervalMs - 30_000L).coerceAtLeast(0L))
    }

    fun isPreparingLocation(session: Session, now: Long = System.currentTimeMillis()): Boolean {
        if (!session.active) return false
        val sendAt = session.nextSendAt ?: return false
        val lead = prepareLeadMs(session.intervalMinutes)
        return now in (sendAt - lead) until sendAt
    }

    private fun scheduleAlarms(context: Context, session: Session) {
        val sendAt = session.nextSendAt ?: return
        val now = System.currentTimeMillis()
        val prepareAt = sendAt - prepareLeadMs(session.intervalMinutes)
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (prepareAt > now) {
            setExact(alarms, prepareAt, pending(context, session.id, SessionAlarmReceiver.ACTION_PREPARE))
        } else if (sendAt > now) {
            Background.execute { SessionCycle.prepare(context, session.id) }
        }
        if (sendAt > now) {
            setExact(alarms, sendAt, pending(context, session.id, SessionAlarmReceiver.ACTION_SEND))
        }
        Log.d(logTag, "Alarmas sesion ${session.id}: prepare=$prepareAt send=$sendAt")
    }

    fun cancelAlarms(context: Context, sessionId: Long) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(pending(context, sessionId, SessionAlarmReceiver.ACTION_PREPARE))
        alarms.cancel(pending(context, sessionId, SessionAlarmReceiver.ACTION_SEND))
    }

    fun syncForegroundService(context: Context) {
        val keepAlive = dao(context).activeWithInterval().isNotEmpty()
                || ScheduledSmsModel.isSmsSendingEnabled(context)
        if (keepAlive) {
            try {
                RSTForegroundService.start(context)
            } catch (e: Exception) {
                Log.w(logTag, "No se pudo iniciar el servicio en primer plano", e)
            }
        } else {
            RSTForegroundService.stop(context)
        }
    }

    private fun setExact(alarms: AlarmManager, time: Long, pending: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
            return
        }
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }

    private fun pending(context: Context, sessionId: Long, action: String): PendingIntent {
        val intent = Intent(context, SessionAlarmReceiver::class.java)
                .setAction(action)
                .putExtra(SessionAlarmReceiver.EXTRA_SESSION_ID, sessionId)
        val requestCode = requestCode(sessionId, action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context.applicationContext, requestCode, intent, flags)
    }

    private fun requestCode(sessionId: Long, action: String): Int {
        val base = (sessionId and 0x7fff).toInt()
        return if (action == SessionAlarmReceiver.ACTION_PREPARE) base else base or 0x10000
    }

    private fun dao(context: Context) = LlegueDatabase.get(context).sessions()
}
