package com.stc.terminowo.platform

import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene

/** Returns the top-most presented view controller of the active window scene. */
internal fun topViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
    val scene = scenes.firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: scenes.firstOrNull()
    var controller = scene?.keyWindow?.rootViewController ?: return null
    while (true) {
        controller = controller.presentedViewController ?: return controller
    }
}
