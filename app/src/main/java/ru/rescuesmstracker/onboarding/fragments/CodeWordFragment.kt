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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import ru.rescuesmstracker.utils.hideKeyboard
import ru.rst.rescuesmstracker.databinding.FCodeWordBinding

class CodeWordFragment : BaseOnBoardingFragment() {

    private lateinit var binding: FCodeWordBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listener = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                binding.btnSentCodeWord.isEnabled = !s.isEmpty()
                binding.btnRemoveCodeWord.isEnabled = !s.isEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        }
        binding.inputCodeWord.addTextChangedListener(listener)
        binding.inputCodeWord.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(binding.inputCodeWord)
                true
            } else {
                false
            }
        }

        binding.btnSentCodeWord.setOnClickListener {
            onBoardingController?.sendCodeWord(
                onBoardingController!!.getContact(),
                binding.inputCodeWord.text.toString()
            )
        }
        containerBinding.goFurtherButton.setOnClickListener {
            onBoardingController?.onCodeWordSet(binding.inputCodeWord.text.toString())
            onBoardingController?.goToNextScreen()
        }
        binding.btnRemoveCodeWord.setOnClickListener {
            binding.inputCodeWord.text = null
        }
    }

    override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup) {
        binding = FCodeWordBinding.inflate(inflater, container, true)
    }
}