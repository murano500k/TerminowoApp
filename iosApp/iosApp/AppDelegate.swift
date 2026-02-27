import UIKit
import shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let proxyUrl = Bundle.main.infoDictionary?["ProxyURL"] as? String ?? ""
        let apiKey = Bundle.main.infoDictionary?["ProxyAPIKey"] as? String ?? ""
        MainViewControllerKt.doInitKoin(proxyUrl: proxyUrl, apiKey: apiKey)

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = MainViewControllerKt.MainViewController()
        window?.makeKeyAndVisible()
        return true
    }
}
