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

package app.llegue.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object LocationFinder {

    private const val logTag = "LocationFinder"

    /**
     * Bloquea el hilo actual hasta conseguir una posicion o agotar [timeoutSeconds].
     * Nunca debe llamarse desde el hilo principal.
     * Si [fallbackToLastKnown] es false, no usa la ultima posicion del sistema al fallar.
     */
    @SuppressLint("MissingPermission")
    fun await(
            context: Context,
            timeoutSeconds: Long = 45,
            fallbackToLastKnown: Boolean = true
    ): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { manager.isProviderEnabled(it) }

        if (providers.isEmpty()) {
            Log.w(logTag, "No hay proveedores de ubicacion habilitados")
            return if (fallbackToLastKnown) lastKnown(manager) else null
        }

        val latch = CountDownLatch(1)
        val fresh = AtomicReference<Location?>(null)

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                fresh.set(location)
                latch.countDown()
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        val main = Handler(Looper.getMainLooper())
        main.post {
            providers.forEach { provider ->
                try {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                } catch (e: SecurityException) {
                    Log.w(logTag, "Sin permiso para usar $provider", e)
                } catch (e: IllegalArgumentException) {
                    Log.w(logTag, "Proveedor $provider no disponible", e)
                }
            }
        }

        latch.await(timeoutSeconds, TimeUnit.SECONDS)
        main.post { manager.removeUpdates(listener) }

        val obtained = fresh.get()
        if (obtained != null) return obtained
        return if (fallbackToLastKnown) lastKnown(manager) else null
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? =
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                    .mapNotNull {
                        try {
                            manager.getLastKnownLocation(it)
                        } catch (e: SecurityException) {
                            Log.w(logTag, "Sin permiso para leer la ultima ubicacion de $it", e)
                            null
                        }
                    }
                    .maxByOrNull { it.time }
}
