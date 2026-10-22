import SwiftUI
import UIKit

/// A one-line label that scrolls itself when the space it is given is too narrow
/// for the whole string, and stands still when it is not.
///
/// The movement comes from a timer rather than a repeating animation, for the
/// same reason the record spins that way: it can be stopped exactly where it is.
/// The string is laid out twice, so the second copy slides in as the first
/// leaves and the run loops seamlessly.
struct MarqueeText: View {

    let text: String
    let font: UIFont
    var color: Color = ReaderTheme.swiftInk

    /// Points travelled per second.
    private static let speed: CGFloat = 26
    /// Blank run between the end of the string and its repeat.
    private static let repeatGap: CGFloat = 40
    /// Matches the record's tick, so the two animations look like one system.
    private static let tick: TimeInterval = 1.0 / 30.0

    @State private var offset: CGFloat = 0
    @State private var timer: Timer?

    private var textWidth: CGFloat {
        ceil((text as NSString).size(withAttributes: [.font: font]).width)
    }

    var body: some View {
        GeometryReader { geo in
            let width = textWidth
            let travels = width > geo.size.width

            HStack(spacing: Self.repeatGap) {
                label
                if travels { label }
            }
            .frame(width: geo.size.width, height: geo.size.height, alignment: .leading)
            .offset(x: travels ? offset : 0)
            .onAppear { syncScroll(travels: travels, width: width) }
            .onChange(of: geo.size.width) { _, _ in syncScroll(travels: travels, width: width) }
            // The strip follows the voice, so its text changes as chapters turn
            // over: the run has a new length and has to be measured again.
            .onChange(of: text) { _, _ in syncScroll(travels: travels, width: width) }
            .onDisappear { stopScroll() }
        }
        .clipped()
    }

    private var label: some View {
        Text(text)
            .font(Font(font))
            .foregroundStyle(color)
            .lineLimit(1)
            .fixedSize()
    }

    private func syncScroll(travels: Bool, width: CGFloat) {
        stopScroll()
        offset = 0
        guard travels else { return }

        let span = width + Self.repeatGap
        let step = Self.speed * CGFloat(Self.tick)
        timer = Timer.scheduledTimer(withTimeInterval: Self.tick, repeats: true) { _ in
            offset -= step
            if offset <= -span { offset += span }
        }
    }

    private func stopScroll() {
        timer?.invalidate()
        timer = nil
    }
}
