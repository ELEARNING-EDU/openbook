import SwiftUI

/// The reader's one-time gesture guide, the same four-step card as Android's
/// `ReaderGuideOverlay`. It is a plain walkthrough, not anchored callouts: the
/// toolbar can be hidden or the page still opening, and the card reads either way.
enum ReaderGuide {
    static let shownKey = "readerGuide.shown"

    /// Never over a screenshot or test launch (`-demo…` arguments).
    static func pending(_ defaults: UserDefaults = .standard,
                        arguments: [String] = ProcessInfo.processInfo.arguments) -> Bool {
        !defaults.bool(forKey: shownKey) && !arguments.contains { $0.hasPrefix("-demo") }
    }

    static func done(_ defaults: UserDefaults = .standard) {
        defaults.set(true, forKey: shownKey)
    }
}

struct ReaderGuideOverlay: View {
    let palette: ReaderPalette
    let onDone: () -> Void

    private let steps: [(title: String, body: String)] = [
        ("guide.turn.title", "guide.turn.body"),
        ("guide.chrome.title", "guide.chrome.body"),
        ("guide.mark.title", "guide.mark.body"),
        ("guide.voice.title", "guide.voice.body"),
    ]
    @State private var step = 0

    var body: some View {
        let last = step == steps.count - 1
        ZStack(alignment: .bottom) {
            // The guide owns every touch, so nothing underneath reacts to it.
            Color(red: 0x12 / 255, green: 0x10 / 255, blue: 0x0E / 255).opacity(0.62)
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture {}

            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: "\(step + 1)/\(steps.count)")
                    .font(.system(size: 12, design: .serif))
                    .tracking(1)
                    .foregroundStyle(palette.swiftAccent)
                Text(LocalizedStringKey(steps[step].title))
                    .font(.system(size: 20, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.swiftInk)
                    .padding(.top, 6)
                Text(LocalizedStringKey(steps[step].body))
                    .font(.system(size: 15, design: .serif))
                    .lineSpacing(5)
                    .foregroundStyle(palette.swiftInk.opacity(0.78))
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 8)
                HStack(spacing: 6) {
                    ForEach(steps.indices, id: \.self) { index in
                        Circle()
                            .fill(index == step ? palette.swiftAccent : palette.swiftInk.opacity(0.20))
                            .frame(width: index == step ? 8 : 6, height: index == step ? 8 : 6)
                    }
                }
                .accessibilityHidden(true)
                .padding(.top, 16)
                HStack {
                    Button("guide.skip", action: onDone)
                        .foregroundStyle(palette.swiftInk.opacity(0.6))
                    Spacer()
                    Button(last ? "guide.start" : "guide.next") {
                        if last { onDone() } else { step += 1 }
                    }
                    .fontWeight(.semibold)
                    .foregroundStyle(palette.swiftAccent)
                }
                .font(.system(size: 16, design: .serif))
                .frame(minHeight: 44)
                .padding(.top, 8)
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 20)
            .frame(maxWidth: 420, alignment: .leading)
            .background(palette.swiftPaper, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .padding(.horizontal, 20)
            .padding(.vertical, 28)
            .accessibilityElement(children: .contain)
            .accessibilityAddTraits(.isModal)
        }
    }
}
