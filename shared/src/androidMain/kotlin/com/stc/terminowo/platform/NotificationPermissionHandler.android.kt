package com.stc.terminowo.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

actual class NotificationPermissionHandler(
    private val context: Context
) {
    companion object {
        var permissionLauncher: ((onResult: (Boolean) -> Unit) -> Unit)? = null
    }

    actual fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    actual fun requestPermission(onResult: (Boolean) -> Unit) {
        if (hasPermission()) {
            onResult(true)
            return
        }
        val launcher = permissionLauncher
        if (launcher != null) {
            launcher(onResult)
        } else {
            onResult(false)
        }
    }
}
