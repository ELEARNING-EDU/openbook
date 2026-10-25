import AVFoundation
import SwiftUI
import UIKit

/// The voice chooser: every voice installed on the phone, grouped by language
/// and region, each one playable before it is applied.
struct VoicePickerSheet: View {

    @ObservedObject var speech: SpeechController
    @ObservedObject private var settings = ReaderSettings.shared
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @StateObject private var previewer = VoicePreviewer()
    @State private var query = ""

    private var palette: ReaderPalette { settings.palette }
    private var allGroups: [VoiceGroup] {
        let installed = VoiceEntry.entries(from: SpeechController.availableVoices())
        return VoiceCatalog.groups(
            from: VoiceCatalog.voices(installed, speaking: speech.bookLanguage),
            preferredLanguage: speech.bookLanguage
        )
    }
    private var groups: [VoiceGroup] {
        VoiceCatalog.groups(allGroups, matching: query)
    }
    private var offeredCount: Int {
        allGroups.reduce(0) { $0 + $1.entries.count }
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            searchField

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if groups.isEmpty {
                        Text("reader.voice.noResults")
                            .font(.footnote)
                            .foregroundStyle(palette.swiftInkSoft)
                    }
                    ForEach(groups) { group in
                        section(group)
                    }
                    downloadHint
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
            }
        }
        .background(palette.popupPaper.ignoresSafeArea())
        .presentationBackground(palette.popupPaper)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .onDisappear { previewer.stop() }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 3) {
                Text("reader.voice")
                    .font(.system(size: 20, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.swiftInk)
                Text(verbatim: L10n.format("reader.voice.count", offeredCount))
                    .font(.footnote)
                    .foregroundStyle(palette.swiftInkSoft)
            }

            Spacer()

            Button {
                previewer.stop()
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(palette.swiftInk)
                    .frame(width: 32, height: 32)
                    .background(Circle().fill(palette.controlFill))
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("reader.voice.close"))
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 8)
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(palette.swiftInkSoft)

            TextField("reader.voice.search", text: $query)
                .font(.system(size: 16))
                .foregroundStyle(palette.swiftInk)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)

            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 15))
                        .foregroundStyle(palette.swiftInkSoft)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("reader.voice.clearSearch"))
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 44)
        .background(
            palette.controlFill,
            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(palette.swiftInk.opacity(0.10))
        )
        .padding(.horizontal, 20)
        .padding(.bottom, 4)
    }

    private func section(_ group: VoiceGroup) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(verbatim: group.title)
                .font(.system(size: 12, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
                .textCase(.uppercase)

            VStack(spacing: 0) {
                ForEach(group.entries) { entry in
                    row(entry)
                    if entry != group.entries.last {
                        Divider().overlay(palette.swiftInk.opacity(0.10))
                    }
                }
            }
            .background(RoundedRectangle(cornerRadius: 14).fill(palette.controlFill))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .strokeBorder(palette.swiftInk.opacity(0.10))
            )
        }
    }

    private func row(_ entry: VoiceEntry) -> some View {
        let selected = speech.selectedVoiceID == entry.id
        let playing = previewer.speakingID == entry.id

        return HStack(spacing: 12) {
            Button {
                preview(entry)
            } label: {
                Image(systemName: playing ? "stop.fill" : "play.fill")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(palette.swiftInk)
                    .frame(width: 38, height: 38)
                    .background(Circle().fill(palette.popupPaper))
                    .overlay(Circle().strokeBorder(palette.swiftInk.opacity(0.14)))
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(playing ? "reader.voice.stopPreview" : "reader.voice.preview"))

            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: entry.name)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(palette.swiftInk)
                    .lineLimit(1)
                Text(verbatim: qualityLabel(entry))
                    .font(.system(size: 11, weight: .medium, design: .rounded))
                    .foregroundStyle(palette.swiftInkSoft)
            }

            Spacer(minLength: 8)

            Button {
                apply(entry)
            } label: {
                if selected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 21, weight: .semibold))
                        .foregroundStyle(palette.swiftAccent)
                        .frame(width: 38, height: 38)
                } else {
                    Text("reader.voice.use")
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .foregroundStyle(palette.swiftInk)
                        .padding(.horizontal, 12)
                        .frame(height: 32)
                        .background(Capsule().fill(palette.popupPaper))
                        .overlay(Capsule().strokeBorder(palette.swiftInk.opacity(0.14)))
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(selected ? "reader.voice.current" : "reader.voice.use"))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }

    private var downloadHint: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("reader.voice.more.title")
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInk)

            Text("reader.voice.more.body")
                .font(.footnote)
                .foregroundStyle(palette.swiftInkSoft)
                .fixedSize(horizontal: false, vertical: true)

            Button {
                openSettings()
            } label: {
                Text("reader.voice.more.button")
                    .font(.system(size: 13, weight: .semibold, design: .rounded))
                    .foregroundStyle(palette.swiftInk)
                    .padding(.horizontal, 14)
                    .frame(height: 34)
                    .background(Capsule().fill(palette.controlFill))
                    .overlay(Capsule().strokeBorder(palette.swiftInk.opacity(0.14)))
            }
            .buttonStyle(.plain)
            .padding(.top, 2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14).fill(palette.controlFill.opacity(0.6)))
    }

    private func qualityLabel(_ entry: VoiceEntry) -> String {
        // Named quality, then the voice's own locale: "vi-VN".
        let quality = switch entry.quality {
        case 2: L10n.string("reader.voice.quality.enhanced")
        case 3: L10n.string("reader.voice.quality.premium")
        default: L10n.string("reader.voice.quality.default")
        }
        return "\(quality) · \(entry.language)"
    }

    private func preview(_ entry: VoiceEntry) {
        // One voice at a time: reading the book aloud while a sample plays would
        // put two narrations over each other.
        if speech.state == .playing { speech.pause() }
        previewer.toggle(
            id: entry.id,
            sample: L10n.string(entry.isVietnamese ? "reader.voice.sample.vi" : "reader.voice.sample.en")
        )
    }

    private func apply(_ entry: VoiceEntry) {
        previewer.stop()
        speech.setVoice(id: entry.id)
        dismiss()
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        openURL(url)
    }
}

/// Plays one sample sentence in a chosen voice, apart from the book's own
/// narration so previewing cannot disturb where the reader is.
final class VoicePreviewer: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {

    @Published private(set) var speakingID: String?

    private let synthesizer = AVSpeechSynthesizer()

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    func toggle(id: String, sample: String) {
        if speakingID == id {
            stop()
            return
        }
        stop()
        guard let voice = AVSpeechSynthesisVoice(identifier: id) else { return }

        let utterance = AVSpeechUtterance(string: sample)
        utterance.voice = voice
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        speakingID = id
        synthesizer.speak(utterance)
    }

    func stop() {
        if synthesizer.isSpeaking { synthesizer.stopSpeaking(at: .immediate) }
        speakingID = nil
    }

    func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didFinish utterance: AVSpeechUtterance
    ) {
        finish()
    }

    func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didCancel utterance: AVSpeechUtterance
    ) {
        finish()
    }

    private func finish() {
        DispatchQueue.main.async { [weak self] in self?.speakingID = nil }
    }
}
