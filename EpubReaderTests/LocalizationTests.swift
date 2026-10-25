import XCTest
import SwiftUI
@testable import EpubReader

final class LocalizationTests: XCTestCase {

    func testBothLanguagesResolveTheShellTitles() {
        XCTAssertEqual(L10n.string("tab.home", language: .english), "Home")
        XCTAssertEqual(L10n.string("tab.home", language: .vietnamese), "Trang chủ")
        XCTAssertEqual(L10n.string("library.delete", language: .english), "Delete")
        XCTAssertEqual(L10n.string("library.delete", language: .vietnamese), "Xoá")
    }

    func testStringTablesHaveTheSameKeys() throws {
        let en = try keys(in: "en", file: "Localizable.strings")
        let vi = try keys(in: "vi", file: "Localizable.strings")
        XCTAssertEqual(en, vi)

        let enPlural = try keys(in: "en", file: "Localizable.stringsdict")
        let viPlural = try keys(in: "vi", file: "Localizable.stringsdict")
        XCTAssertEqual(enPlural, viPlural)
        XCTAssertTrue(enPlural.contains("library.selectedCount"))
        XCTAssertTrue(enPlural.contains("library.delete.multiple.message"))
    }

    func testSystemLanguageIsNotAnOverride() {
        XCTAssertNil(AppLanguage.system.code)
        XCTAssertEqual(AppLanguage.vietnamese.code, "vi")
        XCTAssertEqual(AppLanguage.english.code, "en")
    }

    private func keys(in language: String, file: String) throws -> Set<String> {
        let folder = try XCTUnwrap(
            Bundle.main.path(forResource: language, ofType: "lproj"),
            "missing \(language).lproj in the app"
        )
        let path = try XCTUnwrap(
            Bundle(path: folder)?.path(forResource: (file as NSString).deletingPathExtension, ofType: (file as NSString).pathExtension)
        )
        let dictionary = try XCTUnwrap(NSDictionary(contentsOfFile: path) as? [String: Any])
        return Set(dictionary.keys)
    }
}

final class AppearanceBoundaryTests: XCTestCase {

    func testTheShellHasItsOwnLightAndDarkFaces() {
        let light = ReaderPalette.shell(dark: false)
        let dark = ReaderPalette.shell(dark: true)

        XCTAssertFalse(light.isDark)
        XCTAssertTrue(dark.isDark)
        XCTAssertEqual(light.id, "shell")
        XCTAssertEqual(dark.id, "shell")
    }

    func testReaderThemesKeepTheirOwnPalettesAndNamesInBothLanguages() {
        for spec in ReaderTheme.themes {
            let palette = spec.palette(dark: false)
            XCTAssertNotEqual(palette.id, "shell")
            XCTAssertFalse(L10n.string("theme.\(spec.id)", language: .english).isEmpty)
            XCTAssertFalse(L10n.string("theme.\(spec.id)", language: .vietnamese).isEmpty)
        }
    }

    func testSavedReaderThemeStillResolvesAfterALanguageChoice() {
        XCTAssertEqual(ReaderTheme.spec(id: "focus").id, "focus")
        XCTAssertEqual(ReaderTheme.spec(id: "not-a-theme").id, ReaderTheme.defaultThemeID)
    }
}
