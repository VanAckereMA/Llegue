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

package app.llegue.sessions

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import app.llegue.Background
import app.llegue.R
import app.llegue.LlegueBatteryManager
import app.llegue.LlegueForegroundService
import app.llegue.databinding.ACreateSessionBinding
import app.llegue.location.LocationFinder
import app.llegue.sms.SessionSms
import app.llegue.sms.SmsGateway
import app.llegue.settings.AppTheme
import java.util.Calendar

class CreateSessionActivity : Activity() {

    private lateinit var binding: ACreateSessionBinding

    private var contactName: String? = null
    private var contactPhone: String? = null
    private var endHour: Int? = null
    private var endMinute: Int? = null
    private var editingId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.apply(this)
        super.onCreate(savedInstanceState)
        binding = ACreateSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpIntervalPickers()

        binding.contact.setOnClickListener { pickContact() }

        binding.hasEnd.setOnCheckedChangeListener { _, checkedId ->
            val withEnd = checkedId == R.id.endYes
            binding.endTime.visibility = if (withEnd) View.VISIBLE else View.GONE
            binding.noEndHint.visibility = if (withEnd) View.GONE else View.VISIBLE
        }

        binding.endTime.setOnClickListener { pickEndTime() }
        binding.start.setOnClickListener { onStartRequested() }
        binding.cancel.setOnClickListener { finish() }

        editingId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
        if (editingId != 0L) {
            binding.formTitle.setText(R.string.form_title_edit)
            binding.start.setText(R.string.form_save)
            Background.run({ LlegueDatabase.get(applicationContext).sessions().byId(editingId) }) { session ->
                if (session == null) {
                    finish()
                } else {
                    bindSession(session)
                }
            }
        }
    }

    private fun bindSession(session: Session) {
        binding.tripName.setText(session.name)
        contactName = session.contactName
        contactPhone = session.contactPhone
        binding.contact.text = session.contactName
        binding.contact.setTextColor(AppTheme.color(this, R.attr.contentPrimary))
        binding.codeWord.setText(session.codeWord)
        populateInterval(session.intervalMinutes)
        session.endsAt?.let { endsAt ->
            val calendar = Calendar.getInstance().apply { timeInMillis = endsAt }
            endHour = calendar.get(Calendar.HOUR_OF_DAY)
            endMinute = calendar.get(Calendar.MINUTE)
            binding.endTime.text = String.format("%02d:%02d", endHour, endMinute)
            binding.hasEnd.check(R.id.endYes)
        }
    }

    private fun populateInterval(minutes: Int?) {
        val unit = intervalUnitOf(minutes)
        binding.intervalUnit.value = unit
        applyUnit(unit)
        if (unit == UNIT_NONE || minutes == null) return
        binding.intervalValue.value = when (unit) {
            UNIT_MINUTES -> minutes
            UNIT_HOURS -> minutes / 60
            else -> minutes / (24 * 60)
        }
    }

    private fun intervalUnitOf(minutes: Int?): Int = when {
        minutes == null -> UNIT_NONE
        minutes % (24 * 60) == 0 -> UNIT_DAYS
        minutes % 60 == 0 -> UNIT_HOURS
        else -> UNIT_MINUTES
    }

    private fun setUpIntervalPickers() {
        val units = arrayOf(
                getString(R.string.form_interval_none),
                getString(R.string.form_interval_minutes),
                getString(R.string.form_interval_hours),
                getString(R.string.form_interval_days))

        binding.intervalUnit.apply {
            minValue = 0
            maxValue = units.size - 1
            displayedValues = units
            value = UNIT_MINUTES
            wrapSelectorWheel = false
            setOnValueChangedListener { _, _, newValue -> applyUnit(newValue) }
        }
        applyUnit(UNIT_MINUTES)
    }

    private fun applyUnit(unit: Int) {
        val picker: NumberPicker = binding.intervalValue
        val min = when (unit) {
            UNIT_NONE -> 0
            UNIT_MINUTES -> MINIMUM_INTERVAL_MINUTES
            else -> 1
        }
        val max = when (unit) {
            UNIT_NONE -> 0
            UNIT_MINUTES -> 59
            UNIT_HOURS -> 23
            else -> 30
        }
        val value = when (unit) {
            UNIT_NONE -> 0
            UNIT_MINUTES -> MINIMUM_INTERVAL_MINUTES
            else -> 1
        }
        picker.isEnabled = unit != UNIT_NONE
        if (max > picker.maxValue) picker.maxValue = max
        if (min < picker.minValue) picker.minValue = min
        picker.value = value
        picker.minValue = min
        picker.maxValue = max
        picker.wrapSelectorWheel = false
    }

    private fun selectedIntervalMinutes(): Int? = when (binding.intervalUnit.value) {
        UNIT_NONE -> null
        UNIT_MINUTES -> binding.intervalValue.value
        UNIT_HOURS -> binding.intervalValue.value * 60
        UNIT_DAYS -> binding.intervalValue.value * 24 * 60
        else -> null
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, requestPickContact)
    }

    private fun pickEndTime() {
        val now = Calendar.getInstance()
        val hour = endHour ?: now.get(Calendar.HOUR_OF_DAY)
        val minute = endMinute ?: now.get(Calendar.MINUTE)
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            endHour = selectedHour
            endMinute = selectedMinute
            binding.endTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
        }, hour, minute, true).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != requestPickContact || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        readContact(uri)
    }

    private fun readContact(uri: Uri) {
        val columns = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER)
        contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(0)
                contactPhone = cursor.getString(1)
                binding.contact.text = contactName
                binding.contact.setTextColor(AppTheme.color(this, R.attr.contentPrimary))
            }
        }
    }

    private fun onStartRequested() {
        if (binding.tripName.text.isBlank()) {
            toast(getString(R.string.form_error_trip_name))
            return
        }
        if (contactPhone.isNullOrBlank()) {
            toast(getString(R.string.form_error_contact))
            return
        }
        if (binding.codeWord.text.isBlank()) {
            toast(getString(R.string.form_error_code_word))
            return
        }
        if (binding.hasEnd.checkedRadioButtonId == R.id.endYes && endHour == null) {
            toast(getString(R.string.form_error_end_time))
            return
        }

        if (editingId != 0L) {
            saveEdit()
            return
        }

        val missing = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), permissionsRequest)
            return
        }

        saveAndSend()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != permissionsRequest) return
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            saveAndSend()
        } else {
            toast(getString(R.string.permissions_needed))
        }
    }

    private fun saveAndSend() {
        val session = Session(
                name = binding.tripName.text.toString().trim(),
                contactName = contactName ?: "",
                contactPhone = contactPhone ?: return,
                codeWord = binding.codeWord.text.toString().trim(),
                intervalMinutes = selectedIntervalMinutes(),
                startedAt = System.currentTimeMillis(),
                endsAt = selectedEndsAt())

        binding.start.isEnabled = false
        toast(getString(R.string.sending_first_sms))
        if (session.intervalMinutes != null) {
            LlegueForegroundService.start(this)
        }

        val context = applicationContext
        Background.run({
            val id = LlegueDatabase.get(context).sessions().insert(session)
            val location = LocationFinder.await(context)
            if (location != null) {
                LlegueDatabase.get(context).sessions()
                        .saveLocation(id, location.latitude, location.longitude, System.currentTimeMillis())
            }
            val battery = LlegueBatteryManager.getCurrentBatteryLevel(context)
            val text = SessionSms.compose(session, location, battery)
            val sent = SmsGateway.send(context, session.contactPhone, text)
            SessionScheduler.afterFirstSend(context, session.copy(id = id))
            id to sent
        }) { (_, sent) ->
            val message = if (sent) {
                getString(R.string.first_sms_sent, session.contactName)
            } else {
                getString(R.string.first_sms_failed, session.contactName)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun saveEdit() {
        val phone = contactPhone ?: return
        val name = binding.tripName.text.toString().trim()
        val trusted = contactName ?: ""
        val codeWord = binding.codeWord.text.toString().trim()
        val interval = selectedIntervalMinutes()
        val endsAt = selectedEndsAt()
        binding.start.isEnabled = false
        val context = applicationContext
        Background.run({
            val dao = LlegueDatabase.get(context).sessions()
            val current = dao.byId(editingId) ?: return@run false
            dao.update(current.copy(
                    name = name,
                    contactName = trusted,
                    contactPhone = phone,
                    codeWord = codeWord,
                    intervalMinutes = interval,
                    endsAt = endsAt))
            true
        }) { saved ->
            if (saved) {
                toast(getString(R.string.session_updated))
                finish()
            } else {
                binding.start.isEnabled = true
            }
        }
    }

    private fun selectedEndsAt(): Long? {
        val hour = endHour ?: return null
        val minute = endMinute ?: return null
        if (binding.hasEnd.checkedRadioButtonId != R.id.endYes) return null

        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (end.timeInMillis <= System.currentTimeMillis()) {
            end.add(Calendar.DAY_OF_MONTH, 1)
        }
        return end.timeInMillis
    }

    private fun toast(message: String) =
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {

        const val EXTRA_SESSION_ID = "session_id"

        private const val UNIT_NONE = 0
        private const val UNIT_MINUTES = 1
        private const val UNIT_HOURS = 2
        private const val UNIT_DAYS = 3

        private const val MINIMUM_INTERVAL_MINUTES = 5

        private const val requestPickContact = 1
        private const val permissionsRequest = 2

        private val requiredPermissions = arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
