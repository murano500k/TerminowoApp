import UIKit
import FBSDKCoreKit
import shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        ApplicationDelegate.shared.application(
            application,
            didFinishLaunchingWithOptions: launchOptions
        )
        AppEvents.shared.activateApp()

        let proxyUrl = Bundle.main.infoDictionary?["ProxyURL"] as? String ?? ""
        let apiKey = Bundle.main.infoDictionary?["ProxyAPIKey"] as? String ?? ""
        MainViewControllerKt.doInitKoin(proxyUrl: proxyUrl, apiKey: apiKey)

        let viewController = MainViewControllerKt.MainViewController()

        let tapGesture = UITapGestureRecognizer(target: viewController.view, action: #selector(UIView.endEditing(_:)))
        tapGesture.cancelsTouchesInView = false
        viewController.view.addGestureRecognizer(tapGesture)

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = viewController
        window?.makeKeyAndVisible()
        return true
    }
}
