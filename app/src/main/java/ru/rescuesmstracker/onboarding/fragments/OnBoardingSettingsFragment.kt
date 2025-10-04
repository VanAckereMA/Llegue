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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.rescuesmstracker.settings.MainSettingsController
import ru.rst.rescuesmstracker.databinding.FOnboardingSettingsBinding

class OnBoardingSettingsFragment : BaseOnBoardingFragment() {

    private lateinit var binding: FOnboardingSettingsBinding

    override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup) {
        binding = FOnboardingSettingsBinding.inflate(inflater, container, true)
    }

    private lateinit var mainSettingsController: MainSettingsController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainSettingsController = MainSettingsController(
            activity = requireActivity(),
            pref_interval = binding.mainSettings.prefInterval,
            pref_max_sms_count = binding.mainSettings.prefMaxSmsCount,
            pref_coords_format = binding.mainSettings.prefCoordsFormat
        )
        mainSettingsController.initViews()
    }
}