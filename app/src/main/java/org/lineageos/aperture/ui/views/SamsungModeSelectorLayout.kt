package org.lineageos.aperture.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import org.lineageos.aperture.R
import org.lineageos.aperture.samsung.SamsungVendorKeys

/**
 * Two-row Samsung mode selector.
 * Top row: photo modes (Photo, 108MP, Beauty, Night, Super Night, Pro, HDR, Food, Live Focus)
 * Bottom row: video/special modes (Video, Pro Video, Slow Motion, Single Take, Director's View, RAW)
 */
class SamsungModeSelectorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val row1: LinearLayout
    private val row2: LinearLayout
    private val buttons = mutableMapOf<Int, Button>()
    private var currentMode = SamsungVendorKeys.MODE_SINGLE

    var onSamsungModeSelected: (mode: Int) -> Unit = {}

    init {
        orientation = VERTICAL
        setPadding(8, 36, 8, 4)

        row1 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 0, 0, 3)
        }
        addView(row1, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        row2 = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        addView(row2, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setModes(modes: List<Int>) {
        row1.removeAllViews()
        row2.removeAllViews()
        buttons.clear()

        // Photo-focused modes on row 1
        val row1Modes = modes.filter { mode ->
            mode in listOf(
                SamsungVendorKeys.MODE_SINGLE,
                SamsungVendorKeys.MODE_108MP,
                SamsungVendorKeys.MODE_BEAUTY,
                SamsungVendorKeys.MODE_NIGHT,
                SamsungVendorKeys.MODE_SUPER_NIGHT,
                SamsungVendorKeys.MODE_PRO,
                SamsungVendorKeys.MODE_HDR,
                SamsungVendorKeys.MODE_FOOD,
                SamsungVendorKeys.MODE_LIVE_FOCUS,
            )
        }

        // Video/special modes on row 2
        val row2Modes = modes.filter { mode ->
            mode !in row1Modes
        }

        for (mode in row1Modes) {
            val btn = createModeButton(mode)
            buttons[mode] = btn
            row1.addView(btn)
        }

        for (mode in row2Modes) {
            val btn = createModeButton(mode)
            buttons[mode] = btn
            row2.addView(btn)
        }

        updateHighlight()
    }

    private fun createModeButton(mode: Int): Button {
        val btn = LayoutInflater.from(context)
            .inflate(R.layout.samsung_mode_button, this, false) as Button
        btn.text = SamsungVendorKeys.modeName(mode)
        btn.setOnClickListener {
            if (mode != currentMode) {
                selectMode(mode)
                onSamsungModeSelected(mode)
            }
        }
        return btn
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
