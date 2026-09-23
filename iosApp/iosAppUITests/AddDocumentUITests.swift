import Photos
import XCTest

/// End-to-end tests for the "Add Document" sheet. The gallery tests put a known test image
/// into the photo library, pick it through the system photo picker, run OCR against the real
/// backend, and delete the image again afterwards.
final class AddDocumentUITests: XCTestCase {
    private var app: XCUIApplication!
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
    private var seededAssetIds: [String] = []
    private var seededTimes: [Date] = []

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments += ["-uiTesting", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        acceptConsentIfNeeded()
        dismissSystemAlertIfPresent()
    }

    override func tearDownWithError() throws {
        try deleteSeededAssets()
    }

    // MARK: - Tests

    func testGalleryHeicPhotoIsScanned() throws {
        try seedPhoto(named: "polisa_photo", ext: "heic")
        pickNewestPhotoFromGallery()
        assertScanFindsExpiryDate()
    }

    func testGalleryPngScreenshotIsScanned() throws {
        try seedPhoto(named: "polisa_screenshot", ext: "png")
        pickNewestPhotoFromGallery()
        assertScanFindsExpiryDate()
    }

    func testGalleryPickerCancelReturnsToApp() {
        openAddSheet()
        element("Select Photo from Gallery").tap()
        let cancel = app.buttons["Cancel"]
        XCTAssertTrue(cancel.waitForExistence(timeout: 10), "Photo picker did not appear")
        cancel.tap()
        XCTAssertTrue(addButton.waitForExistence(timeout: 10), "App did not return after cancelling picker")
    }

    func testChooseFileCancelReturnsToApp() {
        openAddSheet()
        element("Choose File").tap()
        let cancel = app.buttons["Cancel"]
        XCTAssertTrue(cancel.waitForExistence(timeout: 10), "Document picker did not appear")
        cancel.tap()
        XCTAssertTrue(addButton.waitForExistence(timeout: 10), "App did not return after cancelling file picker")
    }

    // MARK: - Flows

    private var addButton: XCUIElement { element("Add Document") }

    private func openAddSheet() {
        XCTAssertTrue(addButton.waitForExistence(timeout: 15), "Add button not found")
        addButton.tap()
        XCTAssertTrue(element("Select Photo from Gallery").waitForExistence(timeout: 5), "Add sheet did not open")
    }

    private func pickNewestPhotoFromGallery() {
        openAddSheet()
        element("Select Photo from Gallery").tap()

        // The picker grid lists the newest photo first
        let newest = app.images.matching(NSPredicate(format: "label BEGINSWITH 'Photo'")).firstMatch
        XCTAssertTrue(newest.waitForExistence(timeout: 15), "No photos in picker")

        // Never pick one of the device owner's own photos: the cell must carry the seeded image's time
        let times = seededTimes.flatMap { date in
            ["HH:mm", "h:mm a"].map { format -> String in
                let formatter = DateFormatter()
                formatter.dateFormat = format
                return formatter.string(from: date)
            }
        }
        guard times.contains(where: { newest.label.contains($0) }) else {
            XCTFail("Newest picker photo '\(newest.label)' is not the seeded test image (expected one of \(times))")
            return
        }
        // The picker is a remote view that can report cells as not hittable while it settles
        let hittable = expectation(for: NSPredicate(format: "isHittable == true"), evaluatedWith: newest)
        if XCTWaiter.wait(for: [hittable], timeout: 5) == .completed {
            newest.tap()
        } else {
            newest.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
    }

    private func assertScanFindsExpiryDate() {
        let scan = element("Get Expiry Date")
        XCTAssertTrue(scan.waitForExistence(timeout: 15), "Preview screen did not appear")
        scan.tap()

        // DD/MM/YYYY in the Expiry Date field, as extracted by OCR from the test document
        let date = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS '14/11/2026' OR value CONTAINS '14/11/2026'"))
            .firstMatch
        if !date.waitForExistence(timeout: 45) {
            let screenshot = XCTAttachment(screenshot: app.screenshot())
            screenshot.lifetime = .keepAlways
            add(screenshot)
            print(app.debugDescription)
            XCTFail("OCR did not extract the expiry date")
        }
        XCTAssertTrue(element("Save Document").exists)
    }

    private func acceptConsentIfNeeded() {
        let tos = element("I accept the Terms of Service")
        guard tos.waitForExistence(timeout: 5) else { return }
        // The checkbox sits just left of its label
        for label in ["I accept the Terms of Service", "I accept the Privacy Policy"] {
            element(label).coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0.5))
                .withOffset(CGVector(dx: -24, dy: 0)).tap()
        }
        element("Continue").tap()
    }

    // MARK: - Helpers

    private func element(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    // System alerts follow the device language, not the app's -AppleLanguages override
    private let allowLabels = ["Allow", "OK", "Pozwalaj", "Zezwól"]
    private let fullAccessLabels = ["Allow Full Access", "Pełny dostęp"]
    private let deleteLabels = ["Delete", "Usuń"]

    private func systemAlertButton(_ labels: [String]) -> XCUIElement {
        springboard.alerts.buttons.matching(NSPredicate(format: "label IN %@", labels)).firstMatch
    }

    private func dismissSystemAlertIfPresent() {
        let button = systemAlertButton(allowLabels)
        if button.waitForExistence(timeout: 2) { button.tap() }
    }

    private func requestPhotoLibraryAccess() throws {
        if PHPhotoLibrary.authorizationStatus(for: .readWrite) == .authorized { return }

        var status = PHAuthorizationStatus.notDetermined
        PHPhotoLibrary.requestAuthorization(for: .readWrite) { status = $0 }

        // First run shows a system permission alert for the test runner (possibly followed by a
        // "full access" confirmation), so keep tapping the allow button until access is granted
        let deadline = Date().addingTimeInterval(30)
        while status == .notDetermined && Date() < deadline {
            let allow = systemAlertButton(fullAccessLabels + allowLabels)
            if allow.waitForExistence(timeout: 1) { allow.tap() }
            RunLoop.current.run(until: Date().addingTimeInterval(0.5))
        }
        XCTAssertEqual(status, .authorized, "Test runner needs full photo library access to seed and clean up test images")
    }

    private func seedPhoto(named name: String, ext: String) throws {
        try requestPhotoLibraryAccess()
        let url = try XCTUnwrap(Bundle(for: Self.self).url(forResource: name, withExtension: ext))
        var assetId: String?
        let now = Date()
        // Allow for the picker label rolling over to the next minute
        seededTimes = [now, now.addingTimeInterval(60)]
        try PHPhotoLibrary.shared().performChangesAndWait {
            let request = PHAssetCreationRequest.forAsset()
            request.addResource(with: .photo, fileURL: url, options: nil)
            request.creationDate = now
            assetId = request.placeholderForCreatedAsset?.localIdentifier
        }
        seededAssetIds.append(try XCTUnwrap(assetId))
    }

    private func deleteSeededAssets() throws {
        guard !seededAssetIds.isEmpty else { return }
        let assets = PHAsset.fetchAssets(withLocalIdentifiers: seededAssetIds, options: nil)
        let deleted = expectation(description: "delete seeded photos")
        var success = false
        PHPhotoLibrary.shared().performChanges({
            PHAssetChangeRequest.deleteAssets(assets)
        }, completionHandler: { ok, _ in
            success = ok
            deleted.fulfill()
        })
        // iOS asks for confirmation before an app deletes photos
        let confirm = systemAlertButton(deleteLabels)
        if confirm.waitForExistence(timeout: 5) { confirm.tap() }
        wait(for: [deleted], timeout: 15)
        XCTAssertTrue(success, "Seeded test photos were not deleted from the photo library")
        seededAssetIds = []
    }
}
