package ru.rescuesmstracker.location

import android.location.Location
import android.location.LocationListener

class AccumulationLocationListener : LocationListener {

    private var mostAccurateLocation: Location? = null

    @Synchronized
    override fun onLocationChanged(location: Location) {
        if (location.hasAccuracy()) {
            if (location.isMoreAccurateThan(mostAccurateLocation)) {
                mostAccurateLocation = location
            }
        }
    }

    @Synchronized
    fun getMostAccurateLocation(): Location? = mostAccurateLocation

    private fun Location.isMoreAccurateThan(other: Location?): Boolean =
            other == null || accuracy < other.accuracy

}