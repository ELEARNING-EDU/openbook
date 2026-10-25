import SwiftUI

/// How many columns of text a page carries.
enum ColumnSetting: String, CaseIterable, Identifiable {
    case auto
    case one
    case two

    var id: String { rawValue }

    var label: String {
        switch self {
        case .auto: return L10n.string("columns.auto")
        case .one: return "1"
        case .two: return "2"
        }
    }
}

/// The faces the reader can set a book in. Every one of them ships with iOS.
enum ReaderFonts {

    struct Choice: Identifiable, Equatable {
        let id: String
        let name: String
        /// PostScript family name, or nil for the system face.
        let family: String?
        /// System sans instead of the system serif.
        var sans = false
    }

    static let defaultID = "newyork"

    static let all: [Choice] = [
        Choice(id: "newyork", name: "New York", family: nil),
        Choice(id: "charter", name: "Charter", family: "Charter"),
        Choice(id: "georgia", name: "Georgia", family: "Georgia"),
        Choice(id: "palatino", name: "Palatino", family: "Palatino"),
        Choice(id: "times", name: "Times New Roman", family: "Times New Roman"),
        Choice(id: "iowan", name: "Iowan Old Style", family: "IowanOldStyle-Roman"),
        Choice(id: "avenir", name: "Avenir Next", family: "AvenirNext-Regular", sans: true)
    ]

    static func choice(_ id: String) -> Choice {
        all.first { $0.id == id } ?? all[0]
    }

    /// The face at a given size and weight, with the italic trait carried over
    /// from whatever the importer produced.
    static func font(id: String, size: CGFloat, weight: UIFont.Weight, italic: Bool) -> UIFont {
        let choice = self.choice(id)

        var base: UIFont
        if let family = choice.family, let named = UIFont(name: family, size: size) {
            base = named
        } else if choice.sans {
            base = .systemFont(ofSize: size, weight: weight)
        } else if let serif = UIFont.systemFont(ofSize: size, weight: weight)
            .fontDescriptor.withDesign(.serif) {
            base = UIFont(descriptor: serif, size: size)
        } else {
            base = .systemFont(ofSize: size, weight: weight)
        }

        var traits = base.fontDescriptor.symbolicTraits
        if weight >= .semibold { traits.insert(.traitBold) }
        if italic { traits.insert(.traitItalic) }
        if traits != base.fontDescriptor.symbolicTraits,
           let descriptor = base.fontDescriptor.withSymbolicTraits(traits) {
            base = UIFont(descriptor: descriptor, size: size)
        }
        return base
    }
}

/// One theme's own settings: type, spacing, margins and layout.
///
/// Kept per theme, the way Apple Books does it: Original can sit at one size
/// while Focus is larger and wider. A theme is "customised" — and wears the
/// asterisk in the grid — as soon as any of these differs from the default.
struct ReaderCustom: Codable, Equatable {

    /// The Customize switch. While it is off the spacing sliders do not apply.
    var enabled = false
    var fontID = ReaderFonts.defaultID
    var bold = false
    var bodySize = Double(ReaderMetrics.baseBodySize)
    var lineSpacing = 1.14
    var characterSpacing = 0.0
    var wordSpacing = 0.0
    var margin = 0.0
    var columns = ColumnSetting.auto.rawValue
    var justify = false

    static let sizeOptions: [Double] = [16, 17, 19, 21, 23]
    static let lineSpacingRange: ClosedRange<Double> = 1.0...2.0
    static let characterSpacingRange: ClosedRange<Double> = -0.5...3.0
    static let wordSpacingRange: ClosedRange<Double> = 0...6
    static let marginRange: ClosedRange<Double> = 0...36

    /// What the reader applies. With the Customize switch off only the type face
    /// and the type size are in force; the spacing sliders wait, so a reader who
    /// never opened the drawer still gets the theme exactly as designed.
    var applied: ReaderCustom {
        guard !enabled else { return self }
        var plain = self
        plain.lineSpacing = ReaderCustom().lineSpacing
        plain.characterSpacing = 0
        plain.wordSpacing = 0
        plain.margin = 0
        return plain
    }

    var fontName: String { ReaderFonts.choice(fontID).name }
    var columnSetting: ColumnSetting { ColumnSetting(rawValue: columns) ?? .auto }

    var sizeLabel: String { "\(Int(bodySize))" }
    var lineSpacingLabel: String { String(format: "%.2f", lineSpacing).replacingOccurrences(of: ".", with: ",") }
    var characterSpacingLabel: String { String(format: "%.2f pt", characterSpacing).replacingOccurrences(of: ".", with: ",") }
    var wordSpacingLabel: String { String(format: "%.1f pt", wordSpacing).replacingOccurrences(of: ".", with: ",") }
    var marginLabel: String { String(format: "%.0f pt", margin) }

    var canGrowText: Bool { bodySize < Self.sizeOptions[Self.sizeOptions.count - 1] }
    var canShrinkText: Bool { bodySize > Self.sizeOptions[0] }
}

/// Reading appearance. Shared by the paginator and the page renderer — both must
/// use the same values or lines will overflow the page — and persisted so the book
/// opens the way the reader left it.
final class ReaderSettings: ObservableObject {

    /// One settings object for the whole app, not one per book.
    ///
    /// The look belongs to the reader, and the shelf is printed on the same paper:
    /// the library reads this object, so a theme chosen in a book (or on the panel)
    /// repaints the library too, without either screen owning a copy of the palette.
    /// Android does the same with its process-wide `ReaderSettings` feeding
    /// `ReaderTheme`.
    static let shared = ReaderSettings()

    private static let themeKey = "readerThemeID"
    private static let appearanceKey = "readerAppearanceDark"
    private static let customsKey = "readerCustoms"
    private static let customisedKey = "readerCustomisedThemes"

    /// The look the reader is set in.
    @Published var themeID: String {
        didSet { UserDefaults.standard.set(themeID, forKey: Self.themeKey) }
    }

    /// Day or night. The theme is the same either way; this picks its face.
    @Published var isDarkAppearance: Bool {
        didSet { UserDefaults.standard.set(isDarkAppearance, forKey: Self.appearanceKey) }
    }

    /// Every theme's own settings, keyed by theme id.
    @Published private(set) var customs: [String: ReaderCustom] {
        didSet { persistCustoms() }
    }

    /// The themes that have been redesigned on the customise screen.
    ///
    /// The asterisk in the theme grid answers "have I changed this look?", and a
    /// look is changed by choosing a face, spacing or margin — not by pressing the
    /// A/A buttons on the panel, which only pick a size.
    @Published private(set) var customisedThemes: Set<String> {
        didSet { UserDefaults.standard.set(Array(customisedThemes), forKey: Self.customisedKey) }
    }

    var spec: ReaderThemeSpec { ReaderTheme.spec(id: themeID) }
    var palette: ReaderPalette { spec.palette(dark: isDarkAppearance) }

    /// The open theme's settings as the reader has them.
    var custom: ReaderCustom { customs[themeID] ?? ReaderCustom() }
    /// …and as the reader applies them.
    var applied: ReaderCustom { custom.applied }

    /// The open theme differs from the way it was designed, wherever the change
    /// came from — the reset button's question.
    var isChanged: Bool { custom != ReaderCustom() }

    var isCustomised: Bool { isCustomised(themeID) }
    func isCustomised(_ id: String) -> Bool {
        // Marked on the customise screen *and* still different from the theme as
        // it was designed: a look put back the way it started has no asterisk.
        customisedThemes.contains(id) && (customs[id] ?? ReaderCustom()) != ReaderCustom()
    }

    init() {
        let savedTheme = UserDefaults.standard.string(forKey: Self.themeKey) ?? ""
        let theme = ReaderTheme.themes.contains { $0.id == savedTheme }
            ? savedTheme
            : ReaderTheme.defaultThemeID

        themeID = theme
        isDarkAppearance = UserDefaults.standard.bool(forKey: Self.appearanceKey)

        customisedThemes = Set(UserDefaults.standard.stringArray(forKey: Self.customisedKey) ?? [])

        if let data = UserDefaults.standard.data(forKey: Self.customsKey),
           let decoded = try? JSONDecoder().decode([String: ReaderCustom].self, from: data) {
            customs = decoded
        } else {
            customs = [:]
        }

        migrateOldSettings(into: theme)
    }

    // MARK: - Editing

    /// Changes the open theme's settings.
    ///
    /// `inCustomise` separates the two places a reader can change a look: the
    /// customise screen, whose changes earn the asterisk, and the panel's size
    /// buttons, whose changes do not.
    func update(inCustomise: Bool = true, _ change: (inout ReaderCustom) -> Void) {
        var next = customs[themeID] ?? ReaderCustom()
        change(&next)
        customs[themeID] = next
        if inCustomise {
            customisedThemes.insert(themeID)
        }
    }

    /// Back to the theme as it was designed: clears the asterisk.
    func resetCurrentTheme() {
        customs[themeID] = ReaderCustom()
        customisedThemes.remove(themeID)
    }

    /// Puts a theme's settings back the way they were when a screen opened, which
    /// is what the X on the customise screen means.
    func restore(_ custom: ReaderCustom) {
        customs[themeID] = custom
        if custom == ReaderCustom() {
            customisedThemes.remove(themeID)
        }
    }

    func setTheme(_ id: String) {
        guard ReaderTheme.themes.contains(where: { $0.id == id }) else { return }
        themeID = id
    }

    func toggleAppearance() { isDarkAppearance.toggle() }

    /// Day or night, without toggling: the capture harness has to be able to say
    /// which face it wants, whatever the last run left behind.
    func setAppearance(dark: Bool) { isDarkAppearance = dark }

    // MARK: - Type size

    func growText() { step(by: 1) }
    func shrinkText() { step(by: -1) }

    var sizeLabel: String { custom.sizeLabel }
    var canGrowText: Bool { custom.canGrowText }
    var canShrinkText: Bool { custom.canShrinkText }

    private func step(by delta: Int) {
        let options = ReaderCustom.sizeOptions
        let index = options.firstIndex(of: custom.bodySize) ?? 2
        update(inCustomise: false) {
            $0.bodySize = options[min(max(index + delta, 0), options.count - 1)]
        }
    }

    // MARK: - Storage

    private func persistCustoms() {
        guard let data = try? JSONEncoder().encode(customs) else { return }
        UserDefaults.standard.set(data, forKey: Self.customsKey)
    }

    /// The reader used to keep one size and one leading for every theme. Fold
    /// those into the theme that was open so nobody loses their place.
    private func migrateOldSettings(into theme: String) {
        let defaults = UserDefaults.standard
        guard defaults.object(forKey: "readerBodySize") != nil else { return }

        var custom = customs[theme] ?? ReaderCustom()
        let size = defaults.double(forKey: "readerBodySize")
        if ReaderCustom.sizeOptions.contains(size) { custom.bodySize = size }
        let spacing = defaults.double(forKey: "readerLineSpacing")
        if ReaderCustom.lineSpacingRange.contains(spacing), spacing != 1.14 {
            custom.lineSpacing = spacing
            custom.enabled = true
        }
        if defaults.object(forKey: "readerBodySerif") as? Bool == false {
            custom.fontID = "avenir"
        }
        customs[theme] = custom

        for key in ["readerBodySize", "readerLineSpacing", "readerBodySerif"] {
            defaults.removeObject(forKey: key)
        }
    }
}
