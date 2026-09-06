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
 * Horizontal scrollable Samsung mode selector with rounded buttons.
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
            setPadding(12, 40, 12, 12)
        }
        addView(container, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ))
    }

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
            btn.alpha = if (isSelected) 1.0f else 0.55f
            btn.setTextColor(
                if (isSelected) 0xFFFFFFFF.toInt()
                else 0xAAFFFFFF.toInt()
            )
            btn.background.setTint(
                if (isSelected) 0x40FFFFFF
                else 0x00000000
            )
        }
    }

    fun getCurrentMode() = currentMode
}
