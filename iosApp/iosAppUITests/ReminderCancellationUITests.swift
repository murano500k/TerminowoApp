import XCTest

/// Verifies that editing or deleting documents cancels their scheduled reminders,
/// including custom-date ones.
///
/// The app is launched with `-uiTestSeedReminders` (DEBUG builds only), which
/// replaces all documents with two seeded ones (see `UiTestSupport.kt`) and shows
/// the pending notification identifiers (`<documentId>_<daysBefore>`) in the
/// `uitest_pending` label. `-uiTesting` exposes Compose semantics to XCUITest.
final class ReminderCancellationUITests: XCTestCase {
    private let docA = "uitest-a"  // expires in 30 days; reminders 0, 1, 7, 14 + custom 23
    private let docB = "uitest-b"  // expires in 40 days; reminders 7 + custom 25 (control)

    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting", "-uiTestSeedReminders", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        allowNotificationsIfAsked()

        waitForPending("seeded reminders") { ids in
            [0, 1, 7, 14, 23].allSatisfy { ids.contains("\(self.docA)_\($0)") } &&
                [7, 25].allSatisfy { ids.contains("\(self.docB)_\($0)") }
        }
    }

    func testTurningOffCustomReminderCancelsIt() {
        openDocument(docA)
        tap("customReminderSwitch")
        tap("saveDocumentButton")

        waitForPending("custom reminder cancelled, others kept") { ids in
            !ids.contains("\(self.docA)_23") &&
                [0, 1, 7, 14].allSatisfy { ids.contains("\(self.docA)_\($0)") } &&
                ids.contains("\(self.docB)_25")
        }
    }

    func testChangingCustomDateCancelsOldOne() {
        let calendar = Calendar(identifier: .gregorian)
        let today = calendar.startOfDay(for: Date())
        let current = calendar.date(byAdding: .day, value: 7, to: today)!  // 30 - 23
        // Pick a neighbouring day in the same month so the picker needn't page.
        let next = calendar.date(byAdding: .day, value: 1, to: current)!
        let sameMonth = calendar.component(.month, from: next) == calendar.component(.month, from: current)
        let target = sameMonth ? next : calendar.date(byAdding: .day, value: -1, to: current)!
        let newDaysBefore = sameMonth ? 22 : 24

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US")
        formatter.dateFormat = "MMMM d, yyyy"
        let targetLabel = formatter.string(from: target)

        openDocument(docA)
        tap("customReminderDateField")
        let day = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS[c] %@", targetLabel))
            .firstMatch
        XCTAssertTrue(day.waitForExistence(timeout: 5), "No day '\(targetLabel)' in picker:\n\(app.debugDescription)")
        day.tap()
        tap("customDatePickerConfirm")
        tap("saveDocumentButton")

        waitForPending("old custom reminder replaced by new one") { ids in
            !ids.contains("\(self.docA)_23") &&
                ids.contains("\(self.docA)_\(newDaysBefore)") &&
                ids.contains("\(self.docB)_25")
        }
    }

    func testDeletingFromDetailCancelsAllReminders() {
        openDocument(docA)
        tap("deleteDocumentButton")

        waitForPending("all reminders of deleted document cancelled") { ids in
            !ids.contains { $0.hasPrefix("\(self.docA)_") } && ids.contains("\(self.docB)_25")
        }
    }

    func testDeletingFromListCancelsAllReminders() {
        element("document_\(docA)").swipeLeft()
        tap("swipeDelete_\(docA)")
        tap("confirmDialogConfirm")

        waitForPending("all reminders of deleted document cancelled") { ids in
            !ids.contains { $0.hasPrefix("\(self.docA)_") } && ids.contains("\(self.docB)_25")
        }
    }

    func testDeleteAllCancelsAllReminders() {
        tap("settingsMenu")
        tap("menuDeleteAll")
        tap("confirmDialogConfirm")

        waitForPending("no reminders left") { ids in
            !ids.contains { $0.hasPrefix("\(self.docA)_") || $0.hasPrefix("\(self.docB)_") }
        }
    }

    // MARK: - Helpers

    private func element(_ id: String) -> XCUIElement {
        let el = app.descendants(matching: .any)[id].firstMatch
        XCTAssertTrue(el.waitForExistence(timeout: 10), "Element '\(id)' not found:\n\(app.debugDescription)")
        return el
    }

    /// Taps by coordinate: XCUITest's hit test often reports Compose elements as
    /// not hittable even when they're on screen. Compose reports elements outside
    /// the viewport of a scrolling column with an empty frame and can be slow to
    /// refresh frames after a scroll, so scroll and nudge until the frame is real.
    private func tap(_ id: String, file: StaticString = #filePath, line: UInt = #line) {
        _ = element(id)
        let screen = app.windows.firstMatch.frame
        let matches = app.descendants(matching: .any).matching(identifier: id)
        // Compose can keep stale zero-frame copies of a node in the tree, so
        // pick the match that is actually laid out on screen.
        var el = matches.firstMatch
        func onScreen() -> Bool {
            // A full snapshot makes Compose refresh frames of scrolled content;
            // plain element queries can return stale (empty) frames.
            _ = app.debugDescription
            for candidate in matches.allElementsBoundByIndex {
                let f = candidate.frame
                if !f.isEmpty && screen.contains(CGPoint(x: f.midX, y: f.midY)) {
                    el = candidate
                    return true
                }
            }
            return false
        }
        var attempts = 0
        while !onScreen() && attempts < 12 {
            if attempts < 4 {
                app.swipeUp()
            } else {
                // Small drag: refreshes Compose's accessibility frames without paging.
                let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.6))
                start.press(forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 0, dy: -40)))
            }
            attempts += 1
            // Let the fling settle; a tap during it only stops the scroll.
            Thread.sleep(forTimeInterval: 1.0)
        }
        XCTAssertTrue(onScreen(), "Element '\(id)' never got an on-screen frame (\(matches.count) matches)", file: file, line: line)
        // A short press, not tap(): XCUITest's instantaneous taps are unreliable in Compose.
        el.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).press(forDuration: 0.15)
    }

    private func openDocument(_ id: String) {
        tap("document_\(id)")
        _ = element("saveDocumentButton")
    }

    private func pendingIds() -> Set<String> {
        let label = app.staticTexts["uitest_pending"].label
        let list = label.replacingOccurrences(of: "pending:", with: "")
        return Set(list.split(separator: ",").map(String.init))
    }

    private func waitForPending(
        _ description: String,
        timeout: TimeInterval = 10,
        file: StaticString = #filePath,
        line: UInt = #line,
        _ condition: @escaping (Set<String>) -> Bool
    ) {
        let deadline = Date().addingTimeInterval(timeout)
        var ids = pendingIds()
        while !condition(ids) && Date() < deadline {
            Thread.sleep(forTimeInterval: 0.5)
            ids = pendingIds()
        }
        XCTAssertTrue(condition(ids), "Expected \(description); pending: \(ids.sorted())", file: file, line: line)
    }

    private func allowNotificationsIfAsked() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let allow = springboard.buttons["Allow"]
        if allow.waitForExistence(timeout: 2) {
            allow.tap()
        }
    }
}
