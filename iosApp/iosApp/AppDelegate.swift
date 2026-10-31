import UIKit
import UserNotifications
import FBSDKCoreKit
import Sentry
import shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    #if DEBUG
    static let uiTestSeedReminders = ProcessInfo.processInfo.arguments.contains("-uiTestSeedReminders")
    #endif

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        startCrashReporting()

        ApplicationDelegate.shared.application(
            application,
            didFinishLaunchingWithOptions: launchOptions
        )

        let proxyUrl = Bundle.main.infoDictionary?["ProxyURL"] as? String ?? ""
        let apiKey = Bundle.main.infoDictionary?["ProxyAPIKey"] as? String ?? ""
        MainViewControllerKt.doInitKoin(proxyUrl: proxyUrl, apiKey: apiKey)

        #if DEBUG
        // Reminder UI tests: replace all data with seeded documents (see UiTestSupport.kt)
        if AppDelegate.uiTestSeedReminders {
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
            UiTestSupport.shared.resetAndSeed()
        }
        if ProcessInfo.processInfo.arguments.contains("-sentryTestCrash") {
            CrashHook_iosKt.triggerTestCrash()
        }
        #endif
        return true
    }

    private func startCrashReporting() {
        SentrySDK.start { options in
            options.dsn = "https://baea974dada038223b1d80b6d88eff9a@o4512283986493440.ingest.de.sentry.io/4512283992981584"
            #if DEBUG
            options.environment = "debug"
            // Reinstalling or stopping a dev build kills it in the foreground, which Sentry reports as a watchdog termination.
            options.enableWatchdogTerminationTracking = false
            #else
            options.environment = "production"
            #endif
            options.sendDefaultPii = false
            options.tracesSampleRate = 0.1
            // SDK app hang tracking reports idle-main-thread false positives (sentry-cocoa#9329);
            // MetricKit hang diagnostics replace it.
            options.enableAppHangTracking = false
            options.enableMetricKit = true
        }

        // Kotlin/Native aborts on an uncaught exception; record what was thrown so the
        // SIGABRT crash report carries the Kotlin exception instead of just abort().
        CrashHook_iosKt.installKotlinCrashHook { type, message, stackTrace in
            SentrySDK.configureScope { scope in
                scope.setTag(value: type, key: "kotlin.exception")
                scope.setContext(
                    value: ["type": type, "message": message, "stack_trace": stackTrace],
                    key: "Kotlin exception"
                )
            }
        }

        CrashHook_iosKt.installErrorReporterSink { type, message, stackTrace in
            let event = Event(level: .error)
            event.exceptions = [Exception(value: message, type: type)]
            event.context = ["Kotlin exception": ["stack_trace": stackTrace]]
            SentrySDK.capture(event: event)
        }
    }
}

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        let viewController = MainViewControllerKt.MainViewController()

        let tapGesture = UITapGestureRecognizer(target: viewController.view, action: #selector(UIView.endEditing(_:)))
        tapGesture.cancelsTouchesInView = false
        viewController.view.addGestureRecognizer(tapGesture)

        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = viewController
        window.makeKeyAndVisible()
        self.window = window

        #if DEBUG
        if AppDelegate.uiTestSeedReminders {
            installPendingRemindersLabel(in: window)
        }
        #endif
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        AppEvents.shared.activateApp()
    }

    #if DEBUG
    func sceneDidDisconnect(_ scene: UIScene) {
        pendingRemindersTimer?.invalidate()
        pendingRemindersTimer = nil
    }
    #endif

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

        pendingRemindersTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak window, weak label] _ in
            UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
                let ids = requests.map(\.identifier).sorted().joined(separator: ",")
                DispatchQueue.main.async {
                    guard let window, let label else { return }
                    label.text = "pending:" + ids
                    window.bringSubviewToFront(label)
                }
            }
        }
    }
    #endif
}
