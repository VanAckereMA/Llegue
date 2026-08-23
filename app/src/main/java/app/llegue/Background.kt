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

package app.llegue

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

object Background {

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Corre [work] fuera del hilo principal y entrega el resultado en el hilo de UI. */
    fun <T> run(work: () -> T, onResult: (T) -> Unit) {
        executor.execute {
            val result = work()
            main.post { onResult(result) }
        }
    }

    fun execute(work: () -> Unit) {
        executor.execute(work)
    }
}
