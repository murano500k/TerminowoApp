package com.stc.terminowo.platform

/**
 * Forwards handled errors to the crash reporter configured by the host app
 * (Sentry, set up in AppDelegate / TerminowoApp).
 */
object ErrorReporter {
    var sink: ((Throwable) -> Unit)? = null

    fun capture(throwable: Throwable) {
        try {
            sink?.invoke(throwable)
        } catch (_: Throwable) {
        }
    }
}
