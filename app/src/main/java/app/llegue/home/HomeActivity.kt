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

package app.llegue.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import app.llegue.Background
import app.llegue.R
import app.llegue.databinding.AHomeBinding
import app.llegue.databinding.VSessionBinding
import app.llegue.sessions.CreateSessionActivity
import app.llegue.sessions.LlegueDatabase
import app.llegue.sessions.Session
import app.llegue.sessions.SessionScheduler
import app.llegue.settings.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeActivity : Activity() {

    private lateinit var binding: AHomeBinding
    private var themePanelOpen = false

    private val clock = SimpleDateFormat("HH.mm", Locale.getDefault())
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshList = object : Runnable {
        override fun run() {
            loadSessions()
            refreshHandler.postDelayed(this, REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.apply(this)
        super.onCreate(savedInstanceState)
        binding = AHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createSession.setOnClickListener { openForm(null) }
        binding.addSession.setOnClickListener { openForm(null) }
        themePanelOpen = savedInstanceState?.getBoolean(KEY_THEME_PANEL_OPEN) ?: false
        bindThemeSwitch()
        setThemePanelOpen(themePanelOpen, animate = false)
        requestReceiveSmsIfNeeded()
    }

    private fun requestReceiveSmsIfNeeded() {
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_RECEIVE_SMS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_THEME_PANEL_OPEN, themePanelOpen)
    }

    override fun onResume() {
        super.onResume()
        loadSessions()
        refreshHandler.postDelayed(refreshList, REFRESH_MS)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshList)
    }

    private fun openForm(sessionId: Long?) {
        val intent = Intent(this, CreateSessionActivity::class.java)
        if (sessionId != null) {
            intent.putExtra(CreateSessionActivity.EXTRA_SESSION_ID, sessionId)
        }
        startActivity(intent)
    }

    private fun loadSessions() {
        val context = applicationContext
        Background.run({ LlegueDatabase.get(context).sessions().all() }) { sessions ->
            render(sessions)
        }
    }

    private fun render(sessions: List<Session>) {
        val hasSessions = sessions.isNotEmpty()
        binding.emptyState.visibility = if (hasSessions) View.GONE else View.VISIBLE
        binding.listState.visibility = if (hasSessions) View.VISIBLE else View.GONE

        binding.sessions.removeAllViews()
        sessions.forEach { session ->
            binding.sessions.addView(bindCard(session).root)
        }
    }

    private fun bindCard(session: Session): VSessionBinding {
        val card = VSessionBinding.inflate(layoutInflater, binding.sessions, false)
        card.root.clipToOutline = true
        card.name.text = session.name
        card.contact.text = session.contactName
        card.codeWord.text = formatCodeWord(session)
        card.schedule.text = formatSchedule(session)
        card.interval.text = session.intervalMinutes
                ?.let { getString(R.string.session_interval_format, it) }
                ?: getString(R.string.session_no_interval)
        card.nextSend.text = formatNextSend(session)
        card.nextSend.visibility = if (!session.active || session.intervalMinutes != null) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val preparing = SessionScheduler.isPreparingLocation(session)
        card.statusAccent.setBackgroundColor(when {
            preparing -> getColor(R.color.amber_600)
            session.active -> getColor(R.color.llegue_brand)
            else -> AppTheme.color(this, R.attr.statusInactive)
        })
        card.activeSwitch.isChecked = session.active
        card.activeSwitch.trackTintMode = PorterDuff.Mode.SRC_IN
        card.activeSwitch.trackTintList = if (preparing) {
            ColorStateList.valueOf(getColor(R.color.amber_600))
        } else {
            getColorStateList(R.color.session_switch_track)
        }
        card.activeSwitch.setOnClickListener {
            val active = card.activeSwitch.isChecked
            Background.run({
                val db = LlegueDatabase.get(applicationContext)
                db.sessions().setActive(session.id, active)
                if (active) {
                    SessionScheduler.resume(applicationContext, session.id)
                } else {
                    SessionScheduler.pause(applicationContext, session.id)
                }
                db.sessions().all()
            }) { render(it) }
        }
        card.activeSwitch.setOnLongClickListener { true }

        if (!session.active) {
            card.content.setOnClickListener { openForm(session.id) }
        }
        card.content.setOnLongClickListener {
            confirmDelete(session)
            true
        }
        return card
    }

    private fun bindThemeSwitch() {
        val dark = AppTheme.isDark(this)
        binding.themeSwitch.contentDescription = getString(
                if (dark) R.string.theme_switch_to_light else R.string.theme_switch_to_dark)
        binding.themeSun.imageTintList = ColorStateList.valueOf(
                if (dark) getColor(R.color.white_50_alpha) else getColor(R.color.amber_600))
        binding.themeMoon.imageTintList = ColorStateList.valueOf(
                if (dark) getColor(R.color.moon_icon) else getColor(R.color.black_40_alpha))
        binding.themeSwitch.post {
            binding.themeThumb.translationX = themeThumbTravel(dark)
        }
        binding.themeSwitch.setOnClickListener {
            binding.themeSwitch.isEnabled = false
            val nextDark = !dark
            AppTheme.setDark(this, nextDark)
            binding.themeThumb.animate()
                    .translationX(themeThumbTravel(nextDark))
                    .setDuration(180)
                    .withEndAction { recreate() }
                    .start()
        }
        binding.themeTab.setOnClickListener {
            setThemePanelOpen(!themePanelOpen, animate = true)
        }
    }

    private fun setThemePanelOpen(open: Boolean, animate: Boolean) {
        themePanelOpen = open
        binding.themeTab.text = getString(
                if (open) R.string.theme_tab_close else R.string.theme_tab_open)
        binding.themeTab.contentDescription = getString(
                if (open) R.string.theme_tab_hide else R.string.theme_tab_show)
        if (!animate) {
            binding.themeSwitch.visibility = if (open) View.VISIBLE else View.GONE
            binding.themeSwitch.scaleX = 1f
            if (open) {
                binding.themeSwitch.post {
                    binding.themeThumb.translationX = themeThumbTravel(AppTheme.isDark(this))
                }
            }
            return
        }
        if (open) {
            binding.themeSwitch.scaleX = 0f
            binding.themeSwitch.visibility = View.VISIBLE
            binding.themeSwitch.post {
                binding.themeSwitch.pivotX = binding.themeSwitch.width.toFloat()
                binding.themeThumb.translationX = themeThumbTravel(AppTheme.isDark(this))
                binding.themeSwitch.animate()
                        .scaleX(1f)
                        .setDuration(200)
                        .start()
            }
        } else {
            binding.themeSwitch.pivotX = binding.themeSwitch.width.toFloat()
            binding.themeSwitch.animate()
                    .scaleX(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.themeSwitch.visibility = View.GONE
                        binding.themeSwitch.scaleX = 1f
                    }
                    .start()
        }
    }

    private fun themeThumbTravel(dark: Boolean): Float {
        val travel = (binding.themeSwitch.width
                - binding.themeSwitch.paddingLeft
                - binding.themeSwitch.paddingRight
                - binding.themeThumb.width).toFloat()
        return if (dark) travel.coerceAtLeast(0f) else 0f
    }

    private fun confirmDelete(session: Session) {
        AlertDialog.Builder(this)
                .setTitle(R.string.session_delete_title)
                .setMessage(getString(R.string.session_delete_message, session.name))
                .setPositiveButton(R.string.session_delete_confirm) { _, _ ->
                    Background.run({
                        SessionScheduler.delete(applicationContext, session.id)
                        LlegueDatabase.get(applicationContext).sessions().all()
                    }) { render(it) }
                }
                .setNegativeButton(R.string.form_cancel, null)
                .show()
    }

    private fun formatCodeWord(session: Session): CharSequence {
        val text = getString(R.string.session_code_word_format, session.codeWord)
        if (!session.active) return text
        val start = text.indexOf(session.codeWord)
        if (start < 0) return text
        return SpannableString(text).apply {
            val end = start + session.codeWord.length
            setSpan(
                    ForegroundColorSpan(getColor(R.color.llegue_brand_highlight)),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun formatNextSend(session: Session): String {
        if (!session.active) return getString(R.string.session_inactive_status)
        val interval = session.intervalMinutes ?: return ""
        val remainingMs = session.nextSendAt?.let { it - System.currentTimeMillis() }
                ?: run {
                    val intervalMs = TimeUnit.MINUTES.toMillis(interval.toLong())
                    val elapsed = System.currentTimeMillis() - session.startedAt
                    intervalMs - (elapsed % intervalMs)
                }
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs.coerceAtLeast(0)).toInt().coerceAtLeast(1)
        return getString(R.string.session_next_send_format, minutes)
    }

    private fun formatSchedule(session: Session): String {
        val start = clock.format(Date(session.startedAt))
        val end = session.endsAt?.let { clock.format(Date(it)) }
        return if (end == null) {
            getString(R.string.session_start_format, start)
        } else {
            getString(R.string.session_start_end_format, start, end)
        }
    }

    companion object {
        private const val REFRESH_MS = 15_000L
        private const val KEY_THEME_PANEL_OPEN = "theme_panel_open"
        private const val REQUEST_RECEIVE_SMS = 11
    }
}
