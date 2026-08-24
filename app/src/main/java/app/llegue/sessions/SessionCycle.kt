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

import android.content.Context
import android.location.Location
import android.os.PowerManager
import android.util.Log
import app.llegue.LlegueBatteryManager
import app.llegue.LlegueForegroundService
import app.llegue.location.LocationFinder
import app.llegue.sms.SessionSms
import app.llegue.sms.SmsGateway

object SessionCycle {

    private const val logTag = "SessionCycle"
    private const val maxGpsMisses = 3
    private const val prepareTimeoutSeconds = 90L
    private const val sendTimeoutSeconds = 20L

    fun prepare(context: Context, sessionId: Long) {
        val dao = LlegueDatabase.get(context).sessions()
        val session = dao.byId(sessionId) ?: return
        if (!stillRunnable(context, session)) return

        SessionScheduler.syncForegroundService(context)
        val location = LocationFinder.await(
                context,
                timeoutSeconds = prepareTimeoutSeconds,
                fallbackToLastKnown = false)
        val stillActive = dao.byId(sessionId)?.active == true
        if (!stillActive) return
        if (location != null) {
            persistFix(dao, session.id, location)
            Log.d(logTag, "GPS preparado para sesion $sessionId")
        } else {
            Log.w(logTag, "Sin GPS en la ventana de preparacion de sesion $sessionId")
        }
    }

    fun send(context: Context, sessionId: Long) {
        val dao = LlegueDatabase.get(context).sessions()
        val session = dao.byId(sessionId) ?: return
        if (!stillRunnable(context, session)) return

        SessionScheduler.syncForegroundService(context)

        val decision = resolveLocation(context, session)
        val current = dao.byId(session.id) ?: return
        if (!current.active) return
        if (decision.closeSession) {
            SessionScheduler.close(context, session.id)
            Log.w(logTag, "Sesion $sessionId cerrada tras $maxGpsMisses fallos de GPS")
            return
        }

        if (decision.location != null && !decision.reused) {
            persistFix(dao, session.id, decision.location)
            dao.setGpsStatus(session.id, 0, false)
        } else {
            dao.setGpsStatus(session.id, decision.misses, decision.reused || session.reusedLocation)
        }

        val battery = LlegueBatteryManager.getCurrentBatteryLevel(context)
        val text = SessionSms.compose(current, decision.location, battery, decision.reused)
        SmsGateway.send(context, current.contactPhone, text)
        SessionScheduler.scheduleNext(context, current, System.currentTimeMillis())
    }

    /**
     * Respuesta bajo demanda: el contacto envio la palabra clave.
     * No mueve el intervalo programado ni cierra la sesion por fallos de GPS.
     */
    fun sendOnDemand(context: Context, sessionId: Long) {
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "llegue:keyword-sms")
        wakeLock.acquire(60_000L)
        try {
            replyToKeyword(context, sessionId)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
            SessionScheduler.syncForegroundService(context)
        }
    }

    private fun replyToKeyword(context: Context, sessionId: Long) {
        val dao = LlegueDatabase.get(context).sessions()
        val session = dao.byId(sessionId) ?: return
        if (!stillRunnable(context, session)) return

        try {
            LlegueForegroundService.start(context)
        } catch (e: Exception) {
            Log.w(logTag, "No se pudo abrir el servicio para GPS por palabra clave", e)
        }

        val fresh = LocationFinder.await(
                context,
                timeoutSeconds = sendTimeoutSeconds,
                fallbackToLastKnown = false)
        val location: Location?
        val reused: Boolean
        if (fresh != null) {
            persistFix(dao, session.id, fresh)
            dao.setGpsStatus(session.id, 0, false)
            location = fresh
            reused = false
        } else {
            location = storedLocation(session)
            reused = location != null
            Log.w(logTag, "SMS por clave sin GPS fresco para sesion $sessionId")
        }

        val current = dao.byId(session.id) ?: session
        val battery = LlegueBatteryManager.getCurrentBatteryLevel(context)
        val text = SessionSms.compose(current, location, battery, reused)
        val sent = SmsGateway.send(context, current.contactPhone, text)
        Log.d(logTag, "SMS por clave sesion $sessionId enviado=$sent")
    }

    private fun resolveLocation(context: Context, session: Session): LocationDecision {
        val prepared = preparedFix(session)
        if (prepared != null) {
            return LocationDecision(prepared, reused = false, misses = 0)
        }

        val fresh = LocationFinder.await(
                context,
                timeoutSeconds = sendTimeoutSeconds,
                fallbackToLastKnown = false)
        if (fresh != null) {
            return LocationDecision(fresh, reused = false, misses = 0)
        }

        val stored = storedLocation(session)
        val misses = session.consecutiveGpsMisses + 1
        if (stored != null && !session.reusedLocation) {
            return LocationDecision(stored, reused = true, misses = misses)
        }
        if (misses >= maxGpsMisses) {
            return LocationDecision(stored, reused = false, misses = misses, closeSession = true)
        }
        return LocationDecision(stored, reused = false, misses = misses)
    }

    private fun stillRunnable(context: Context, session: Session): Boolean {
        if (!session.active) return false
        val endsAt = session.endsAt ?: return true
        if (System.currentTimeMillis() < endsAt) return true
        SessionScheduler.close(context, session.id)
        return false
    }

    private fun persistFix(dao: SessionDao, sessionId: Long, location: Location) {
        dao.saveLocation(sessionId, location.latitude, location.longitude, System.currentTimeMillis())
    }

    private fun preparedFix(session: Session): Location? {
        val sendAt = session.nextSendAt ?: return storedLocation(session)
        val obtainedAt = session.lastLocationAt ?: return null
        val windowStart = sendAt - SessionScheduler.prepareLeadMs(session.intervalMinutes)
        return if (obtainedAt >= windowStart) storedLocation(session) else null
    }

    private fun storedLocation(session: Session): Location? {
        val lat = session.lastLatitude ?: return null
        val lng = session.lastLongitude ?: return null
        return Location("llegue").apply {
            latitude = lat
            longitude = lng
            time = session.lastLocationAt ?: 0L
        }
    }

    private data class LocationDecision(
            val location: Location?,
            val reused: Boolean,
            val misses: Int,
            val closeSession: Boolean = false
    )
}
