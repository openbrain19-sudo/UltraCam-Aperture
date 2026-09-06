/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.app.Application
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.MainScope
import org.lineageos.aperture.repositories.CameraRepository
import org.lineageos.aperture.repositories.MediaRepository
import org.lineageos.aperture.repositories.OverlaysRepository
import org.lineageos.aperture.repositories.PreferencesRepository

class ApertureApplication : Application() {
    private val coroutineScope = MainScope()

    val cameraRepository by lazy { CameraRepository(this, coroutineScope, overlaysRepository) }
    val mediaRepository by lazy { MediaRepository(this) }
    val overlaysRepository by lazy { OverlaysRepository(this) }
    val preferencesRepository by lazy { PreferencesRepository(this, coroutineScope) }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
