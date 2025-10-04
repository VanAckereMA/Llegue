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

package ru.rescuesmstracker.onboarding.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import ru.rescuesmstracker.data.Contact
import ru.rescuesmstracker.onboarding.ActivitySearchContact
import ru.rescuesmstracker.Constants
import ru.rescuesmstracker.onboarding.FormatUtils
import ru.rescuesmstracker.utils.TextWatcherAdapter
import ru.rescuesmstracker.widget.ContactView
import ru.rst.rescuesmstracker.R
import ru.rst.rescuesmstracker.databinding.FTrustedPhoneNumberBinding
import ru.rst.rescuesmstracker.databinding.VContactInputBinding
import java.lang.IllegalArgumentException

class TrustedPhoneNumberFragment : BaseOnBoardingFragment() {

    enum class Mode {
        DIRECT_INPUT() {
            override fun inflate(container: ViewGroup, contact: Contact, button: View) {
                val result = VContactInputBinding.inflate(
                    LayoutInflater.from(container.context),
                    container,
                    true
                )
                val formatUtils = FormatUtils(container.context)
                val onTextChanged: (String) -> Unit = { extractedValue ->
                    contact.phone = extractedValue
                    button.isEnabled = !extractedValue.isEmpty() && formatUtils.isValidPhoneNumber(extractedValue)
                }
                result.inputPhone.addTextChangedListener(object : TextWatcherAdapter() {
                    override fun afterTextChanged(s: Editable?) {
                        super.afterTextChanged(s)
                        onTextChanged.invoke(s.toString())
                    }
                })
                if (!contact.phone.isEmpty()) {
                    result.inputPhone.setText(contact.phone)
                }
            }
        },
        SELECTED_CONTACT {
            override fun inflate(container: ViewGroup, contact: Contact, button: View) {
                val result = LayoutInflater.from(container.context).inflate(R.layout.v_contact_selected, container, true)
                ((result as FrameLayout).getChildAt(0) as ContactView).setContact(contact)
                button.isEnabled = true
            }
        };

        abstract fun inflate(container: ViewGroup, contact: Contact, button: View)
    }

    private var contact: Contact = Contact()
        set(value) {
            field = value
            mode = if (value.isFromPhoneContacts) Mode.SELECTED_CONTACT else Mode.DIRECT_INPUT
        }
    private var mode: Mode = Mode.DIRECT_INPUT
        set(value) {
            binding.contactContainer.removeAllViews()
            value.inflate(binding.contactContainer, contact, containerBinding.goFurtherButton)
            field = value
        }

    private lateinit var binding: FTrustedPhoneNumberBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        containerBinding.goFurtherButton.setOnClickListener {
            onBoardingController?.onContactSelected(contact)
            onBoardingController?.goToNextScreen()
        }
        containerBinding.goFurtherButton.isEnabled = false

        binding.btnSelectContact.setOnClickListener {
            startActivityForResult(Intent(activity, ActivitySearchContact::class.java),
                    Constants.PHONE_SELECT_ACTIVITY_REQUEST_CODE)
        }

        if (savedInstanceState != null) {
            contact = savedInstanceState.getParcelable("contact") ?: throw IllegalArgumentException("No contact")
        } else {
            mode = Mode.DIRECT_INPUT
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("contact", contact)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.PHONE_SELECT_ACTIVITY_REQUEST_CODE) {
            val receivedContact: Contact? = data?.getParcelableExtra(ActivitySearchContact.CONTACT_KEY)
            if (receivedContact != null) {
                contact = receivedContact
            }
        }
    }

    override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup) {
        binding = FTrustedPhoneNumberBinding.inflate(inflater, container, true)
    }
}