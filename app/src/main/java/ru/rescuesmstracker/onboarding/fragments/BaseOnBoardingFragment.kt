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
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.rescuesmstracker.onboarding.OnBoardingController
import ru.rst.rescuesmstracker.R
import ru.rst.rescuesmstracker.databinding.FBaseOnboardingBinding

abstract class BaseOnBoardingFragment : Fragment() {

    open var onBoardingController: OnBoardingController? = null

    protected lateinit var containerBinding: FBaseOnboardingBinding

    protected abstract fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup)

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        containerBinding = FBaseOnboardingBinding.inflate(layoutInflater, container,false)
        onCreateViewBinding(layoutInflater, containerBinding.onboardingContainer)

        containerBinding.goFurtherButton.text = getString(R.string.code_word_further)
        containerBinding.goFurtherButton.setOnClickListener { onBoardingController?.goToNextScreen() }

        return containerBinding.root
    }
}