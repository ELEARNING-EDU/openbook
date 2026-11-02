import SwiftUI

/// Measurements shared by the floating player button and the screen content
/// that has to keep clear of it.
enum MiniPlayerMetrics {
    static let size: CGFloat = 56
    static let discSize: CGFloat = 48
    static let trailing: CGFloat = 20
    /// The prompt's minimum gap between the button and the top of the tab bar.
    static let gapAboveTabBar: CGFloat = 8
    /// Extra bottom room the scrolling screens need while the button is shown.
    static let reserve: CGFloat = size + gapAboveTabBar
}

/// Whether the shared player button belongs on the current tab.
///
/// Kept apart from the view so the four states the prompt cares about — no
/// book, reader open, playing, paused — are unit-tested without a screen.
enum MiniPlayerVisibility {
    static func isVisible(
        state: NarrationState,
        book: BookItem?,
        readerOpen: Bool,
        bookInLibrary: Bool
    ) -> Bool {
        !readerOpen && state != .idle && book != nil && bookInLibrary
    }
}

/// The floating player on Home, Library and Store: the book's cover as a disc,
/// turning while the voice reads, still while it is paused.
struct MiniPlayerButton: View {

    let book: BookItem
    let state: NarrationState
    let palette: ReaderPalette
    let action: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var spinning: Bool {
        state == .playing && !reduceMotion
    }

    private var stateKey: String {
        state == .playing ? "reader.player.state.playing" : "reader.player.state.paused"
    }

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle().fill(palette.controlFill)
                VinylCover(
                    title: book.title,
                    spinning: spinning,
                    size: MiniPlayerMetrics.discSize
                )
                .accessibilityHidden(true)
            }
            .frame(width: MiniPlayerMetrics.size, height: MiniPlayerMetrics.size)
            .overlay(Circle().strokeBorder(palette.swiftInk.opacity(0.14)))
            .shadow(color: .black.opacity(0.22), radius: 9, y: 4)
            .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: L10n.string("reader.player.open")))
        .accessibilityValue(Text(verbatim: L10n.string(stateKey)))
        .accessibilityAddTraits(.isButton)
    }
}

/// How much extra room a scrolling tab adds at the foot while the shared player
/// is on screen. `MainView` sets it to `MiniPlayerMetrics.reserve` or zero.
private struct PlayerInsetKey: EnvironmentKey {
    static let defaultValue: CGFloat = 0
}

extension EnvironmentValues {
    var playerInset: CGFloat {
        get { self[PlayerInsetKey.self] }
        set { self[PlayerInsetKey.self] = newValue }
    }
}
