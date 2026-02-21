package com.stc.terminowo.platform

import timber.log.Timber

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).e(throwable, message)
    }

    actual fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    actual fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }
}
