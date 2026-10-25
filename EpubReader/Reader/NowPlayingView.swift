import SwiftUI

/// The full player, opened by tapping the title in the reading screen's player
/// row: the book, the whole-book scrubber, play/pause, chapter skips, a sleep
/// timer and the voice the book is read in.
struct NowPlayingView: View {

    let book: BookItem
    @ObservedObject var model: ReaderViewModel
    @ObservedObject var speech: SpeechController
    /// Where the reader is in the book, shown above the scrubber.
    let pageNumber: Int
    let pageCount: Int
    @ObservedObject private var settings = ReaderSettings.shared
    @Environment(\.dismiss) private var dismiss

    /// A local copy while the finger is on the slider, so the knob does not fight
    /// the narration's own progress updates.
    @State private var scrub: Double = 0
    @State private var scrubbing = false
    @State private var showVoices = false

    private var palette: ReaderPalette { settings.palette }

    var body: some View {
        VStack(spacing: 18) {
            bookRow
            scrubber
            transport
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 22)
        .padding(.top, 22)
        .padding(.bottom, 12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(palette.popupPaper.ignoresSafeArea())
        // The sheet's own surface, not just the content's: without this the
        // system's white shows around and through the panel.
        .presentationBackground(palette.popupPaper)
        // A short panel: the content is a book row, a scrubber and one row of
        // controls, so there is no reason to cover half the page.
        .presentationDetents([.height(352), .large])
        .presentationDragIndicator(.visible)
        .sheet(isPresented: $showVoices) {
            VoicePickerSheet(speech: speech)
        }
        .onAppear { scrub = progressFraction }        .onChange(of: speech.highlighted) { _, _ in
            if !scrubbing { scrub = progressFraction }
        }
    }

    // MARK: - Book

    private var bookRow: some View {
        HStack(alignment: .top, spacing: 16) {
            BookCover(book: book)
                .frame(width: 96)

            VStack(alignment: .leading, spacing: 5) {
                Text(verbatim: book.title)
                    .font(.system(size: 19, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.swiftInk)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)

                Text(verbatim: book.author)
                    .font(.footnote)
                    .foregroundStyle(palette.swiftInkSoft)
                    .lineLimit(1)

                Text(verbatim: model.chapterTitle(at: speech.currentChapter))
                    .font(.system(size: 12, weight: .semibold, design: .rounded))
                    .foregroundStyle(palette.swiftAccent)
                    .lineLimit(1)

                Rectangle()
                    .fill(palette.swiftInk.opacity(0.12))
                    .frame(height: 1)
                    .padding(.vertical, 3)

                Text(verbatim: L10n.format("reader.voice.currentName", speech.currentVoiceName))
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundStyle(palette.swiftInkSoft)
                    .lineLimit(1)

                speedControl
                    .padding(.top, 1)
            }

            Spacer(minLength: 0)
        }
    }

    // MARK: - Scrubber

    private var progressFraction: Double {
        guard let spoken = speech.highlighted else { return 0 }
        return model.narrationProgress(chapter: speech.currentChapter, offset: spoken.location)
    }

    private var scrubber: some View {
        VStack(spacing: 6) {
            HStack {
                Text(verbatim: L10n.format("reader.pageOf", pageNumber, pageCount))
                Spacer()
                Text(verbatim: "\(Int((min(max(scrub, 0), 1) * 100).rounded()))%")
            }
            .font(.system(size: 11, weight: .semibold, design: .rounded))
            .monospacedDigit()
            .foregroundStyle(palette.swiftInkSoft)

            Slider(value: $scrub, in: 0...1) { editing in
                scrubbing = editing
                if !editing { seek(to: scrub) }
            }
            .tint(palette.swiftAccent)
            .accessibilityLabel(Text("reader.player.progress"))
        }
    }

    // MARK: - Transport

    private var transport: some View {
        HStack(spacing: 10) {
            sleepMenu

            skipButton(
                "backward.end.fill",
                label: L10n.string("reader.player.previous"),
                enabled: speech.canSkipBackward
            ) { speech.skipChapter(by: -1) }

            Button {
                speech.togglePlayPause()
            } label: {
                Image(systemName: speech.state == .playing ? "pause.fill" : "play.fill")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(palette.swiftPaper)
                    .frame(width: 60, height: 60)
                    .background(Circle().fill(palette.swiftAccent))
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(
                Text(speech.state == .playing ? "reader.pause" : "reader.play")
            )

            skipButton(
                "forward.end.fill",
                label: L10n.string("reader.player.next"),
                enabled: speech.canSkipForward
            ) { speech.skipChapter(by: 1) }

            voiceButton
        }
    }

    /// The speed as a small control beside the chapter: − and + with the current
    /// rate between them, so a press lands on a real target.
    private var speedControl: some View {
        HStack(spacing: 0) {
            Button {
                speech.slower()
            } label: {
                speedIcon("minus")
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(verbatim: L10n.string("reader.slower")))

            Text(speech.rateLabel)
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(palette.swiftInk)
                .frame(minWidth: 42)

            Button {
                speech.faster()
            } label: {
                speedIcon("plus")
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(verbatim: L10n.string("reader.faster")))
        }
        .frame(height: 34)
        .background(Capsule().fill(palette.controlFill))
        .overlay(Capsule().strokeBorder(palette.swiftInk.opacity(0.14)))
        .fixedSize()
    }

    private func speedIcon(_ symbol: String) -> some View {
        Image(systemName: symbol)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(palette.swiftInk)
            .frame(width: 36, height: 34)
            .contentShape(Rectangle())
    }

    private func skipButton(
        _ symbol: String,
        label: String,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 19, weight: .semibold))
                .foregroundStyle(palette.swiftInk)
                .frame(width: 48, height: 48)
                .background(Circle().fill(palette.controlFill))
                .overlay(Circle().strokeBorder(palette.swiftInk.opacity(0.14)))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.3)
        .accessibilityLabel(Text(verbatim: label))
    }

    // MARK: - Options

    private var sleepMenu: some View {
        Menu {
            Button("reader.sleep.off") { speech.setSleepTimer(minutes: nil) }
            ForEach(SpeechController.sleepOptions, id: \.self) { minutes in
                Button {
                    speech.setSleepTimer(minutes: minutes)
                } label: {
                    if speech.sleepMinutes == minutes {
                        Label(L10n.format("reader.sleep.minutes", minutes), systemImage: "checkmark")
                    } else {
                        Text(verbatim: L10n.format("reader.sleep.minutes", minutes))
                    }
                }
            }
        } label: {
            optionIcon(
                "timer",
                tint: speech.sleepMinutes == nil ? palette.swiftInk : palette.swiftAccent
            )
        }
        .accessibilityLabel(Text("reader.sleep"))
        .accessibilityValue(
            Text(verbatim: speech.sleepMinutes.map { L10n.format("reader.sleep.minutes", $0) }
                ?? L10n.string("reader.sleep.off"))
        )
    }

    private var voiceButton: some View {
        Button {
            showVoices = true
        } label: {
            optionIcon("waveform", tint: palette.swiftInk)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("reader.voice"))
    }

    private func optionIcon(_ symbol: String, tint: Color) -> some View {
        Image(systemName: symbol)
            .font(.system(size: 18, weight: .semibold))
            .foregroundStyle(tint)
            .frame(width: 48, height: 48)
            .background(Circle().fill(palette.controlFill))
            .overlay(Circle().strokeBorder(palette.swiftInk.opacity(0.14)))
            .contentShape(Circle())
    }

    // MARK: - Actions

    /// Jumps the narration to a place in the book and keeps it paused if that is
    /// how the reader left it.
    private func seek(to fraction: Double) {
        guard let place = model.narrationPlace(fraction: fraction) else { return }
        let wasPaused = speech.state == .paused
        speech.speak(chapter: place.chapter, fromChapterOffset: place.offset)
        if wasPaused { speech.pause() }
        scrub = progressFraction
    }
}
