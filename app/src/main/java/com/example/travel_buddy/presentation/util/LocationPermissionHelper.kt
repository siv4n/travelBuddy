package com.example.travel_buddy.presentation.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class LocationPermissionHelper(private val fragment: Fragment) {

    companion object {
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }

    fun hasLocationPermission(): Boolean {
        val context = fragment.requireContext()
        return LOCATION_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestLocationPermissions() {
        if (!hasLocationPermission()) {
            fragment.requestPermissions(LOCATION_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            return grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
        return false
    }
}
