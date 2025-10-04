/**
 * Copyright (C) 2020 Safety Tracker
 *
 * This file is part of Open SMS Locator
 *
 * Open SMS Locator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Open SMS Locator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open SMS Locator. If not, see <https://www.gnu.org/licenses/>.
 */

package ru.rescuesmstracker.timer

import android.content.Intent
import android.os.Bundle
import android.view.View
import ru.rescuesmstracker.data.Contact
import ru.rescuesmstracker.data.Sms
import ru.rescuesmstracker.location.LocationProvider
import ru.rescuesmstracker.settings.ActivitySettings
import ru.rescuesmstracker.widget.BaseRSTActivity
import ru.rescuesmstracker.widget.LocationStatusBar
import ru.rescuesmstracker.widget.RSTTimerView
import ru.rst.rescuesmstracker.databinding.ATimer2Binding

class ActivityTimer2 : BaseRSTActivity(), TimerView2 {

    private lateinit var binding: ATimer2Binding

    private lateinit var presenter: TimerPresenter2

    override fun createActivity(savedInstanceState: Bundle?) {
        super.createActivity(savedInstanceState)

        binding = ATimer2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        initDisabledView()
        initEnabledView()

        presenter = TimerPresenterImpl(this, this)

        binding.switchbuttonMain.setOnCheckedChangeListener { _, enabled ->
            presenter.enabled = enabled
        }

        binding.contentEnabled.rstTimer.setOnClickListener {
            presenter.toggleSmsSending()
        }
        binding.contentEnabled.rstTimer.getUpdate = {
            presenter.getTimerUpdate(binding.contentEnabled.rstTimer.max)
        }

        binding.contentEnabled.locationStatusBar.setOnClickListener {
            if (binding.contentEnabled.locationStatusBar.status == LocationStatusBar.Status.LOCATION_DISABLED) {
                LocationProvider.requestLocationEnabling(this@ActivityTimer2)
            }
        }

        binding.contentEnabled.forceSendBtn.setOnClickListener {
            presenter.forceSendSms()
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun showEnabled() {
        binding.contentDisabled.root.visibility = View.INVISIBLE
        binding.contentEnabled.root.visibility = View.VISIBLE
        binding.switchbuttonMain.setCheckedImmediatelyNoEvent(true)
    }

    override fun showDisabled() {
        binding.contentDisabled.root.visibility = View.VISIBLE
        binding.contentEnabled.root.visibility = View.INVISIBLE
        binding.switchbuttonMain.setCheckedImmediatelyNoEvent(false)
    }

    override fun onGotData(status: RSTTimerView.State, lastSmsSentTimestamp: Long, progress: Float) {
        binding.contentEnabled.rstTimer.progress = progress
        binding.contentEnabled.rstTimer.setState(status, lastSmsSentTimestamp)
    }

    override fun onUpdateStatus(accuracy: Float, isLocationEnabled: Boolean) {
        binding.contentEnabled.locationStatusBar.onUpdateStatus(accuracy, isLocationEnabled)
    }

    override fun startTimer() {
        binding.contentEnabled.rstTimer.start()
        binding.contentEnabled.playBtn.playing = true
    }

    override fun stopTimer() {
        binding.contentEnabled.rstTimer.stop()
        binding.contentEnabled.playBtn.playing = false
    }

    override fun setContact(contact: Contact?) {
        binding.contentEnabled.contactView.setContact(contact)
        binding.contentEnabled.forceSendContainer.visibility = if (contact == null) View.INVISIBLE else binding.contentEnabled.forceSendBtn.visibility
    }

    override fun forceSmsSendingStatusUpdated(status: Sms.Status) {
        if (status == Sms.Status.SENDING) {
            binding.contentEnabled.forceSendBtn.visibility = View.INVISIBLE
            binding.contentEnabled.forceSendProgress.visibility = View.VISIBLE
        } else {
            binding.contentEnabled.forceSendBtn.visibility = View.VISIBLE
            binding.contentEnabled.forceSendProgress.visibility = View.INVISIBLE
        }
    }

    private fun initDisabledView() {
        binding.contentDisabled.btnDisabledSettings.setOnClickListener {
            startActivity(Intent(this, ActivitySettings::class.java))
        }
    }

    private fun initEnabledView() {
        binding.contentEnabled.btnEnabledSettings.setOnClickListener {
            startActivity(Intent(this, ActivitySettings::class.java))
        }
    }
}