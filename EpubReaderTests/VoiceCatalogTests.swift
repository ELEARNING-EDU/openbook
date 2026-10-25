import XCTest
@testable import EpubReader

final class VoiceCatalogTests: XCTestCase {

    private let english = Locale(identifier: "en_US")

    private func entry(_ name: String, _ language: String) -> VoiceEntry {
        VoiceEntry(id: "com.test.\(name)", name: name, language: language, quality: 1)
    }

    func testVietnameseComesFirstWhenThePhoneIsVietnamese() {
        let groups = VoiceCatalog.groups(
            from: [entry("Daniel", "en-GB"), entry("Linh", "vi-VN")],
            preferredLanguage: "vi-VN",
            locale: english
        )
        XCTAssertEqual(groups.map(\.id), ["vi-VN", "en-GB"])
    }

    func testThePhonesLanguageComesFirst() {
        let groups = VoiceCatalog.groups(
            from: [entry("Linh", "vi-VN"), entry("Daniel", "en-GB"), entry("Majed", "ar-001")],
            preferredLanguage: "ar-SA",
            locale: english
        )
        XCTAssertEqual(groups.first?.id, "ar-001")
        XCTAssertEqual(groups.map(\.id), ["ar-001", "vi-VN", "en-GB"])
    }

    func testVoicesAreGroupedByLanguage() {
        let groups = VoiceCatalog.groups(
            from: [
                entry("Mai", "vi-VN"),
                entry("Daniel", "en-GB"),
                entry("Kate", "en-GB"),
                entry("Linh", "vi-VN"),
            ],
            preferredLanguage: "vi-VN",
            locale: english
        )
        XCTAssertEqual(groups.count, 2)
        XCTAssertEqual(groups[0].entries.map(\.name), ["Linh", "Mai"])
        XCTAssertEqual(groups[1].entries.map(\.name), ["Daniel", "Kate"])
    }

    func testGroupTitleNamesTheRegion() {
        let groups = VoiceCatalog.groups(
            from: [entry("Linh", "vi-VN")],
            preferredLanguage: "vi-VN",
            locale: english
        )
        XCTAssertTrue(groups[0].title.contains("Vietnamese"), groups[0].title)
        XCTAssertTrue(groups[0].title.contains("Vietnam"), groups[0].title)
    }

    func testSearchFindsALanguageAndKeepsItsWholeGroup() {
        let groups = VoiceCatalog.groups(
            from: [entry("Linh", "vi-VN"), entry("Mai", "vi-VN"), entry("Daniel", "en-GB")],
            preferredLanguage: "vi-VN",
            locale: english
        )
        let hits = VoiceCatalog.groups(groups, matching: "vietnam")
        XCTAssertEqual(hits.map(\.id), ["vi-VN"])
        XCTAssertEqual(hits[0].entries.count, 2)
    }

    func testSearchFindsOneVoiceByName() {
        let groups = VoiceCatalog.groups(
            from: [entry("Linh", "vi-VN"), entry("Mai", "vi-VN")],
            preferredLanguage: "vi-VN",
            locale: english
        )
        let hits = VoiceCatalog.groups(groups, matching: "mai")
        XCTAssertEqual(hits.map(\.id), ["vi-VN"])
        XCTAssertEqual(hits[0].entries.map(\.name), ["Mai"])
    }

    func testAnEmptySearchKeepsEveryGroup() {
        let groups = VoiceCatalog.groups(
            from: [entry("Linh", "vi-VN"), entry("Daniel", "en-GB")],
            preferredLanguage: "vi-VN",
            locale: english
        )
        XCTAssertEqual(VoiceCatalog.groups(groups, matching: "   ").count, 2)
    }

    func testVoicesAreLimitedToTheBooksLanguage() {
        let entries = [
            entry("Linh", "vi-VN"),
            entry("Mai", "vi-VN"),
            entry("Daniel", "en-GB"),
        ]
        let spoken = VoiceCatalog.voices(entries, speaking: "vi")
        XCTAssertEqual(spoken.map(\.name), ["Linh", "Mai"])
    }

    func testABookWithNoInstalledVoiceKeepsEveryVoice() {
        let entries = [entry("Daniel", "en-GB"), entry("Majed", "ar-001")]
        XCTAssertEqual(VoiceCatalog.voices(entries, speaking: "ja").count, 2)
        XCTAssertEqual(VoiceCatalog.voices(entries, speaking: nil).count, 2)
    }

    func testNoVoicesMakesNoGroups() {
        XCTAssertTrue(
            VoiceCatalog.groups(from: [], preferredLanguage: "vi-VN", locale: english).isEmpty
        )
    }
}
