package com.stc.terminowo.platform

actual class NotificationPermissionHandler {
    actual fun hasPermission(): Boolean = true

    actual fun requestPermission(onResult: (Boolean) -> Unit) {
        onResult(true)
    }
}
