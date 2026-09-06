package org.lineageos.aperture.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import org.lineageos.aperture.R
import org.lineageos.aperture.samsung.SamsungVendorKeys

/**
 * Horizontal scrollable selector for Samsung shooting modes.
 * Shows all available Samsung HAL modes when in Photo mode.
 */
class SamsungModeSelectorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val container: LinearLayout
    private val buttons = mutableMapOf<Int, Button>()
    private var currentMode = SamsungVendorKeys.MODE_SINGLE

    var onSamsungModeSelected: (mode: Int) -> Unit = {}

    init {
        isHorizontalScrollBarEnabled = false
        container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }
        addView(container, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ))
    }

    /**
     * Set available Samsung modes and create buttons for each.
     */
    fun setModes(modes: List<Int>) {
        container.removeAllViews()
        buttons.clear()

        for (mode in modes) {
            val btn = LayoutInflater.from(context)
                .inflate(R.layout.samsung_mode_button, container, false) as Button

            btn.text = SamsungVendorKeys.modeName(mode)
            btn.setOnClickListener {
                if (mode != currentMode) {
                    selectMode(mode)
                    onSamsungModeSelected(mode)
                }
            }

            buttons[mode] = btn
            container.addView(btn)
        }

        // Select the current mode
        updateHighlight()
    }

    fun selectMode(mode: Int) {
        currentMode = mode
        updateHighlight()
    }

    private fun updateHighlight() {
        for ((mode, btn) in buttons) {
            val isSelected = mode == currentMode
            btn.isSelected = isSelected
            btn.alpha = if (isSelected) 1.0f else 0.6f
        }
    }

    fun getCurrentMode() = currentMode
}
