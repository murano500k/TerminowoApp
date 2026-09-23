import UIKit
import UserNotifications
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

        #if DEBUG
        // Reminder UI tests: replace all data with seeded documents (see UiTestSupport.kt)
        let uiTestMode = ProcessInfo.processInfo.arguments.contains("-uiTestSeedReminders")
        if uiTestMode {
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
            UiTestSupport.shared.resetAndSeed()
        }
        #endif

        let viewController = MainViewControllerKt.MainViewController()

        let tapGesture = UITapGestureRecognizer(target: viewController.view, action: #selector(UIView.endEditing(_:)))
        tapGesture.cancelsTouchesInView = false
        viewController.view.addGestureRecognizer(tapGesture)

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = viewController
        window?.makeKeyAndVisible()

        #if DEBUG
        if uiTestMode, let window = window {
            installPendingRemindersLabel(in: window)
        }
        #endif
        return true
    }

    #if DEBUG
    private var pendingRemindersTimer: Timer?

    /// UI tests read scheduled reminder identifiers (`<documentId>_<daysBefore>`)
    /// from this label; the app's pending notifications aren't visible to XCUITest.
    private func installPendingRemindersLabel(in window: UIWindow) {
        let label = UILabel()
        label.accessibilityIdentifier = "uitest_pending"
        label.font = .systemFont(ofSize: 9)
        label.numberOfLines = 0
        label.textColor = .systemRed
        label.backgroundColor = UIColor.systemBackground.withAlphaComponent(0.8)
        label.isUserInteractionEnabled = false
        label.text = "pending:"
        label.translatesAutoresizingMaskIntoConstraints = false
        window.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: window.safeAreaLayoutGuide.leadingAnchor, constant: 4),
            label.trailingAnchor.constraint(equalTo: window.safeAreaLayoutGuide.trailingAnchor, constant: -4),
            label.bottomAnchor.constraint(equalTo: window.safeAreaLayoutGuide.bottomAnchor),
        ])

        pendingRemindersTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { _ in
            UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
                let ids = requests.map(\.identifier).sorted().joined(separator: ",")
                DispatchQueue.main.async {
                    label.text = "pending:" + ids
                    window.bringSubviewToFront(label)
                }
            }
        }
    }
    #endif
}
