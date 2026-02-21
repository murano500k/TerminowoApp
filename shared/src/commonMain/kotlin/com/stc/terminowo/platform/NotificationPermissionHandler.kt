package com.stc.terminowo.platform

expect class NotificationPermissionHandler {
    fun hasPermission(): Boolean
    fun requestPermission(onResult: (Boolean) -> Unit)
}
