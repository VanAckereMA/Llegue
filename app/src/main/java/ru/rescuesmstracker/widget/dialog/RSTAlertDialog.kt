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

package ru.rescuesmstracker.widget.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import ru.rst.rescuesmstracker.databinding.VRstDialogBinding

class RSTAlertDialog : DialogFragment() {

    interface DialogMode {
        fun inflateView(container: ViewGroup)
    }

    init {
        arguments = Bundle()
    }

    var dialogMode: DialogMode? = null
        set(value) {
            field = value
            if (isViewInflated) {
                binding.containerRstDialog.removeAllViews()
                if (value == null) {
                    binding.containerRstDialog.visibility = View.GONE
                } else {
                    value.inflateView(binding.containerRstDialog)
                    binding.containerRstDialog.visibility = View.VISIBLE
                }
            }
        }
    var onOkListener: View.OnClickListener? = null
    var onCancelListener: View.OnClickListener? = null
    var dismissOnCancel = true

    /**
     * pass null to hide the button and empty string to use the default value
     */
    var cancelText: CharSequence? = ""
        set(value) {
            if (isViewInflated) {
                binding.textRstDialogCancel.text = value
            }
            field = value
        }
    var dismissOnOk = true

    /**
     * pass null to hide the button and empty string to use the default value
     */
    var okText: CharSequence? = ""
        set(value) {
            if (isViewInflated) {
                binding.textRstDialogOk.text = value
            }
            field = value
        }
    var okEnabled: Boolean = true
        set(value) {
            field = value
            if (isViewInflated) {
                binding.textRstDialogOk.isEnabled = value
            }
        }
    private var isViewInflated = false
    private lateinit var binding: VRstDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = VRstDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textRstDialogOk.visibility = if (okText != null) View.VISIBLE else View.GONE
        binding.textRstDialogOk.setOnClickListener {
            if (dismissOnOk) {
                dismiss()
            }
            onOkListener?.onClick(binding.textRstDialogOk)
        }
        if (!okText.isNullOrEmpty()) {
            binding.textRstDialogOk.text = okText
        }
        okEnabled = true

        binding.textRstDialogCancel.visibility = if (cancelText != null) View.VISIBLE else View.GONE
        binding.textRstDialogCancel.setOnClickListener {
            if (dismissOnCancel) {
                dismiss()
            }
            onCancelListener?.onClick(binding.textRstDialogCancel)
        }
        if (!cancelText.isNullOrEmpty()) {
            binding.textRstDialogCancel.text = cancelText
        }

        binding.textRstDialogTitle.text = arguments?.getCharSequence("title", "")

        this.dialogMode?.inflateView(binding.containerRstDialog)
        binding.containerRstDialog.visibility = if (dialogMode == null) View.GONE else View.VISIBLE
        isViewInflated = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    fun setTitle(title: CharSequence): RSTAlertDialog {
        arguments?.putCharSequence("title", title)
        if (isViewInflated) {
            binding.textRstDialogTitle.text = title
        }
        return this
    }

    override fun onDestroyView() {
        isViewInflated = false
        super.onDestroyView()
    }
}