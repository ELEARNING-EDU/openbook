import SwiftUI
import UIKit

/// Warm paper palette, shared by the library and the reader's default look.
enum ReaderTheme {
    static let paper = UIColor(red: 0.969, green: 0.949, blue: 0.906, alpha: 1)
    static let paperEdge = UIColor(red: 0.925, green: 0.902, blue: 0.855, alpha: 1)
    static let ink = UIColor(red: 0.161, green: 0.141, blue: 0.122, alpha: 1)
    static let inkSoft = UIColor(red: 0.451, green: 0.416, blue: 0.376, alpha: 1)
    static let accent = UIColor(red: 0.620, green: 0.349, blue: 0.196, alpha: 1)
    /// Amber wash painted behind the sentence being read aloud. Held constant
    /// across themes on purpose: it is a marker, not a decoration.
    static let highlight = UIColor(red: 0.980, green: 0.780, blue: 0.330, alpha: 0.42)

    static let swiftPaper = Color(paper)
    static let swiftPaperEdge = Color(paperEdge)
    static let swiftInk = Color(ink)
    static let swiftInkSoft = Color(inkSoft)
    static let swiftAccent = Color(accent)

    // MARK: - Themes

    /// Every look the reader offers, in the order the settings grid shows them.
    ///
    /// Each theme carries two faces: the one it wears in daylight and the one it
    /// wears at night. The appearance decides which is in force, so the same six
    /// themes work in both — the way a paper book looks under a lamp and under a
    /// reading light.
    static let themes: [ReaderThemeSpec] = [
        ReaderThemeSpec(
            id: "original", name: "Original",
            light: PaletteInk(0xFFFFFF, 0xEDEDEF, 0x121212, 0x6E6E73, 0xB4632C),
            dark: PaletteInk(0x000000, 0x1C1C1E, 0xF2F2F2, 0x9E9EA4, 0xE0A06A)
        ),
        ReaderThemeSpec(
            id: "quiet", name: "Quiet",
            light: PaletteInk(0x3A3A3C, 0x48484A, 0xD1D1D6, 0xA0A0A6, 0xF0C98A),
            dark: PaletteInk(0x101012, 0x1E1E20, 0xA9A9B0, 0x77777E, 0xC08A5A)
        ),
        ReaderThemeSpec(
            id: "paper", name: "Paper",
            light: PaletteInk(0xFBF7EE, 0xEFE9DC, 0x2A241D, 0x6E6459, 0x9E5932),
            dark: PaletteInk(0x0F0D0B, 0x1E1B17, 0xEDE6DA, 0x9C9385, 0xD9A06A)
        ),
        ReaderThemeSpec(
            id: "bold", name: "Bold",
            light: PaletteInk(0xFFFFFF, 0xEDEDED, 0x000000, 0x6A6A6A, 0xB4632C),
            dark: PaletteInk(0x000000, 0x161616, 0xFFFFFF, 0x9E9EA4, 0xD08A4A),
            boldBody: true
        ),
        ReaderThemeSpec(
            id: "calm", name: "Calm",
            light: PaletteInk(0xF2E2C4, 0xE6D3AE, 0x5A4527, 0x7C6644, 0xA96A25),
            dark: PaletteInk(0x6B5844, 0x7C6852, 0xF5EDDD, 0xD6C7AF, 0xF0C98A)
        ),
        ReaderThemeSpec(
            id: "focus", name: "Focus",
            light: PaletteInk(0xFFFFFF, 0xEFEFEF, 0x14100A, 0x6B6B6B, 0xA96A25),
            dark: PaletteInk(0x2A2114, 0x3A2D1C, 0xD9B471, 0xA98F5E, 0xE8C073)
        )
    ]

    static let defaultThemeID = "paper"

    /// The look the reader opens in, before anyone has chosen one.
    static let defaultPalette = spec(id: defaultThemeID).palette(dark: false)

    static func spec(id: String) -> ReaderThemeSpec {
        themes.first { $0.id == id } ?? themes.first { $0.id == defaultThemeID } ?? themes[0]
    }
}

/// One theme's two faces.
struct ReaderThemeSpec: Identifiable {
    let id: String
    let name: String
    let light: PaletteInk
    let dark: PaletteInk
    /// The Bold theme sets the body heavier whatever font is chosen.
    var boldBody = false

    func ink(dark: Bool) -> PaletteInk { dark ? self.dark : light }

    func palette(dark: Bool) -> ReaderPalette {
        let ink = self.ink(dark: dark)
        return ReaderPalette(
            id: id,
            name: name,
            paper: ink.paper,
            paperEdge: ink.paperEdge,
            ink: ink.ink,
            inkSoft: ink.inkSoft,
            accent: ink.accent,
            bold: boldBody
        )
    }
}

/// The five colours a single face of a theme uses.
struct PaletteInk: Equatable {
    let paper: UIColor
    let paperEdge: UIColor
    let ink: UIColor
    let inkSoft: UIColor
    let accent: UIColor

    init(_ paper: Int, _ paperEdge: Int, _ ink: Int, _ inkSoft: Int, _ accent: Int) {
        self.paper = UIColor(rgb: paper)
        self.paperEdge = UIColor(rgb: paperEdge)
        self.ink = UIColor(rgb: ink)
        self.inkSoft = UIColor(rgb: inkSoft)
        self.accent = UIColor(rgb: accent)
    }
}

/// A theme resolved against one appearance — what the page, the chrome and the
/// settings panel actually paint with.
struct ReaderPalette: Equatable, Identifiable {

    /// The app shell's own paper, resolved against the phone's appearance.
    ///
    /// Home, Library, Store and their dialogs are not the reader: the theme and
    /// day/night choice inside a book never repaint them. This palette follows
    /// the system instead, and `MainView` hands down the current face.
    static func shell(dark: Bool) -> ReaderPalette {
        let ink = dark
            ? PaletteInk(0x121110, 0x2A2724, 0xF2EDE4, 0xA49B8F, 0xD9A06A)
            : PaletteInk(0xFAF7F2, 0xE7E1D6, 0x1C1814, 0x6E655B, 0x9E5932)
        return ReaderPalette(
            id: "shell",
            name: "Shell",
            paper: ink.paper,
            paperEdge: ink.paperEdge,
            ink: ink.ink,
            inkSoft: ink.inkSoft,
            accent: ink.accent,
            bold: false
        )
    }

    let id: String
    let name: String
    let paper: UIColor
    let paperEdge: UIColor
    let ink: UIColor
    let inkSoft: UIColor
    let accent: UIColor
    /// Sets the body heavier — the whole point of Bold.
    let bold: Bool

    var isDark: Bool { paper.perceivedBrightness < 0.5 }

    /// The theme's name in the app's language. The stored id never changes, so a
    /// language switch cannot lose the reader's saved theme.
    var localizedName: String { L10n.string("theme.\(id)") }

    var swiftPaper: Color { Color(paper) }
    var swiftPaperEdge: Color { Color(paperEdge) }
    var swiftInk: Color { Color(ink) }
    var swiftInkSoft: Color { Color(inkSoft) }
    var swiftAccent: Color { Color(accent) }

    /// The wash behind the sentence being read aloud.
    var highlight: UIColor { ReaderTheme.highlight }

    /// A surface that stands a little proud of the paper: the pills and the cards
    /// in the settings panel. In the dark it is a lift of white, in the light a
    /// film of black, so the panel keeps the theme's colour underneath.
    var panelFill: Color { isDark ? Color.white.opacity(0.14) : Color.black.opacity(0.055) }
    var cardFill: Color { isDark ? Color.white.opacity(0.10) : Color.black.opacity(0.04) }

    /// The ring around the chosen theme. White reads on a dark card, ink on a
    /// light one.
    var selectionRing: Color { isDark ? .white : Color(ink).opacity(0.75) }

    /// The colour of the reader's floating controls — the toolbar pairs, the
    /// audio button, the gear and the player.
    ///
    /// A material would take the system's neutral grey whatever paper the theme
    /// is printed on, so a cream theme wore grey buttons. These controls are the
    /// page's own paper lifted a step off it instead: cream buttons on cream
    /// paper, night buttons on the night theme.
    var controlPaper: UIColor {
        paper.mixed(with: isDark ? .white : .black, amount: isDark ? 0.14 : 0.05)
    }

    /// The floating controls' fill, a hair short of opaque so a line of text
    /// passing behind one still reads as being behind it.
    var controlFill: Color { Color(controlPaper).opacity(0.95) }

    /// The surface of a sheet over the page — the settings panel and the customise
    /// screen behind it.
    ///
    /// Those screens used to be painted with the page's own paper, which made them
    /// melt into the page: on a night theme the panel was a black sheet on a dimmed
    /// black page, and the edge — the one line that says "a panel is open" — was
    /// invisible. This is the paper lifted a step further than the controls are,
    /// so the panel keeps the theme's colour and still reads as lying on top of
    /// it.
    var sheetPaper: Color {
        Color(paper.mixed(with: isDark ? .white : .black, amount: isDark ? 0.19 : 0.045))
    }

    /// The full player's panel: the page's own paper, lifted toward white so the
    /// popup reads as sitting above the page rather than as part of it.
    var popupPaper: Color {
        Color(paper.mixed(with: .white, amount: isDark ? 0.12 : 0.14))
    }

    /// The hairline that draws the top of an open sheet, strongest where the panel
    /// has to part from the page.
    var sheetEdge: Color { Color(ink).opacity(isDark ? 0.14 : 0.10) }
}

extension UIColor {

    convenience init(rgb: Int) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }

    var perceivedBrightness: CGFloat {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        guard getRed(&red, green: &green, blue: &blue, alpha: &alpha) else { return 1 }
        return 0.299 * red + 0.587 * green + 0.114 * blue
    }

    /// Blends the colour towards `other`. The chrome uses it to lift the paper a
    /// step — towards white on a dark theme, towards black on a light one.
    func mixed(with other: UIColor, amount: CGFloat) -> UIColor {
        var r1: CGFloat = 0, g1: CGFloat = 0, b1: CGFloat = 0, a1: CGFloat = 0
        var r2: CGFloat = 0, g2: CGFloat = 0, b2: CGFloat = 0, a2: CGFloat = 0
        guard getRed(&r1, green: &g1, blue: &b1, alpha: &a1),
              other.getRed(&r2, green: &g2, blue: &b2, alpha: &a2) else { return self }
        let t = min(max(amount, 0), 1)
        return UIColor(
            red: r1 + (r2 - r1) * t,
            green: g1 + (g2 - g1) * t,
            blue: b1 + (b2 - b1) * t,
            alpha: a1 + (a2 - a1) * t
        )
    }
}

/// Every measurement the reader's chrome and its pages agree on.
enum ReaderMetrics {
    static let hInset: CGFloat = 20
    static let headerHeight: CGFloat = 14
    static let footerHeight: CGFloat = 14
    static let gap: CGFloat = 14
    /// Every button in the reader — toolbar, audio, settings and the player's
    /// controls — is this exact size, so the chrome reads as one row of equals.
    static let buttonSize: CGFloat = 44
    /// The player's own controls: same icons, tighter frames, so the transport
    /// buttons sit close to the play/pause they belong with.
    static let panelButtonSize: CGFloat = 32
    static let buttonIconSize: CGFloat = 15
    /// Gap between the safe-area top and the floating toolbar.
    static let topButtonGap: CGFloat = 6
    /// Distance from the screen's bottom edge to the footer's bottom.
    static let bottomInset: CGFloat = 18
    /// Room kept clear for the floating controls, tall enough for the unfurled
    /// player so opening it never moves the text.
    static let controlBand: CGFloat = 48
    /// Breathing room between the footer and the floating controls.
    static let footerToControls: CGFloat = 10
    /// Side margin shared by the toolbar and the bottom controls.
    static let hMargin: CGFloat = 16
    /// Gap between two columns of text, when the reader is set to two.
    static let columnGap: CGFloat = 20
    /// The body size the reader opens at, before any customisation.
    static let baseBodySize: CGFloat = 19

    /// Distance from the screen's bottom edge to the floating controls' bottom.
    static let controlsBottomInset: CGFloat = bottomInset + footerHeight + footerToControls

    /// The band the floating bottom controls occupy, in page coordinates.
    static func controlsBand(in bounds: CGSize) -> CGRect {
        let bottom = bounds.height - bottomInset - footerHeight - footerToControls
        return CGRect(x: 0, y: bottom - controlBand, width: bounds.width, height: controlBand)
    }

    /// The running head rides the toolbar's own line, in the gap the two button
    /// pairs leave free, instead of sitting in a band of its own below them.
    static func headerRect(in bounds: CGSize, safeTop: CGFloat) -> CGRect {
        // Two buttons a side, so the head gets what is left between them.
        let cluster = buttonSize * 2 + 10
        let x = hMargin + cluster + 10
        let width = max(40, bounds.width - x * 2)
        let y = safeTop + topButtonGap + (buttonSize - headerHeight) / 2
        return CGRect(x: x, y: y, width: width, height: headerHeight)
    }

    static func footerRect(in bounds: CGSize) -> CGRect {
        CGRect(
            x: hInset,
            y: bounds.height - bottomInset - footerHeight,
            width: bounds.width - hInset * 2,
            height: footerHeight
        )
    }

    /// How many columns fit. "Auto" gives a phone in portrait one column and
    /// anything wider — landscape, a tablet — two.
    static func columnCount(setting: String, in bounds: CGSize) -> Int {
        switch ColumnSetting(rawValue: setting) ?? .auto {
        case .one: return 1
        case .two: return 2
        case .auto: return bounds.width > 560 ? 2 : 1
        }
    }

    /// The whole area text may go in, before it is split into columns.
    /// The paginator measures this and the page draws in it, so the two cannot
    /// drift apart.
    static func textRect(in bounds: CGSize, safeTop: CGFloat, margin: CGFloat = 0) -> CGRect {
        let top = safeTop + topButtonGap + buttonSize + gap
        let bottom = controlsBand(in: bounds).minY - gap
        let side = hInset + margin
        let width = bounds.width - side * 2
        // A hair of slack so a rounding difference never pushes a line off the page.
        return CGRect(
            x: side,
            y: top,
            width: max(40, width - 1),
            height: max(40, bottom - top - 1)
        )
    }

    /// Where each column of text sits. One rect for one column, two side by side
    /// for two. The page draws in these; the paginator measures the first.
    static func columnRects(in bounds: CGSize, safeTop: CGFloat, margin: CGFloat, columns: Int) -> [CGRect] {
        let area = textRect(in: bounds, safeTop: safeTop, margin: margin)
        guard columns > 1 else { return [area] }
        let width = (area.width - columnGap * CGFloat(columns - 1)) / CGFloat(columns)
        return (0..<columns).map { index in
            CGRect(
                x: area.minX + (width + columnGap) * CGFloat(index),
                y: area.minY,
                width: width,
                height: area.height
            )
        }
    }
}
