import AVFoundation
import Foundation

/// Splits text into sentence ranges (UTF-16, so they line up with `NSRange`).
enum SentenceSplitter {

    static func ranges(in text: String) -> [NSRange] {
        let ns = text as NSString
        guard ns.length > 0 else { return [] }

        var result: [NSRange] = []
        ns.enumerateSubstrings(
            in: NSRange(location: 0, length: ns.length),
            options: [.bySentences, .substringNotRequired]
        ) { _, range, _, _ in
            result.append(range)
        }
        return result
    }
}

enum NarrationState: Equatable {
    case idle
    case playing
    case paused
}

/// Reads the book aloud and reports which sentence is being spoken, so the reader
/// can highlight it and follow along.
///
/// Everything that touches published state runs on the main thread; the
/// synthesizer's callbacks arrive on an internal queue.
final class SpeechController: NSObject, ObservableObject {

    static let rateOptions: [Double] = [0.5, 0.75, 1.0, 1.25, 1.5]
    private static let rateKey = "narrationRateFactor"

    @Published private(set) var state: NarrationState = .idle
    /// Range of the sentence being spoken, in chapter coordinates.
    @Published private(set) var highlighted: NSRange?
    @Published private(set) var currentChapter = 0
    @Published private(set) var voiceName = "—"
    @Published private(set) var rateFactor: Double = 1.0

    private let synthesizer = AVSpeechSynthesizer()
    private var chapters: [Chapter] = []
    /// Where the current utterance starts inside its chapter string.
    private var utteranceBase = 0
    /// The utterance we still consider "ours". `stopSpeaking` reports `didFinish`
    /// rather than `didCancel`, so a user-initiated stop has to be told apart from
    /// the end of a chapter by clearing this first.
    private var activeUtterance: AVSpeechUtterance?
    /// Where the voice last reached, so a speed change can resume in the same
    /// sentence instead of jumping back to the top of the page.
    private var lastSpokenOffset = 0

    var isSpeaking: Bool { state == .playing }
    var isBusy: Bool { state != .idle }
    var rateLabel: String { String(format: "%g×", rateFactor) }

    override init() {
        super.init()
        synthesizer.delegate = self

        let saved = UserDefaults.standard.double(forKey: Self.rateKey)
        rateFactor = Self.rateOptions.contains(saved) ? saved : 1.0

        activateAudioSession()
    }

    func configure(chapters: [Chapter]) {
        self.chapters = chapters
    }

    /// Starts narration at the sentence containing `offset` (chapter coordinates).
    func speak(chapter index: Int, fromChapterOffset offset: Int) {
        guard chapters.indices.contains(index) else { return }

        let chapter = chapters[index]
        guard let sentence = sentence(in: chapter, nearest: offset) else { return }

        let text = chapter.text.string as NSString
        guard sentence.location < text.length else { return }

        // `stopSpeaking` on an idle synthesizer wedges the queue, so only interrupt
        // when something is actually playing — or paused, whose utterance would
        // otherwise sit in front of the new one and never let it be heard.
        // Detach first so the resulting callback cannot be mistaken for the end
        // of a chapter.
        activeUtterance = nil
        let wasPaused = synthesizer.isPaused
        if synthesizer.isSpeaking || wasPaused {
            synthesizer.stopSpeaking(at: .immediate)
        }

        let utterance = AVSpeechUtterance(string: text.substring(from: sentence.location))
        if let voice = Self.vietnameseVoice() {
            utterance.voice = voice
            voiceName = "\(voice.name) [\(voice.language)]"
        } else {
            voiceName = "no vi voice — using default"
        }
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * Float(rateFactor)

        utteranceBase = sentence.location
        lastSpokenOffset = sentence.location
        currentChapter = index
        highlighted = sentence
        state = .playing
        activeUtterance = utterance
        NSLog(
            "[speech] speak chapter \(index) from \(sentence.location) "
            + "rate \(rateFactor) len \(text.length - sentence.location) "
            + "wasPaused \(wasPaused)"
        )
        synthesizer.speak(utterance)
    }

    func pause() {
        guard state == .playing else { return }
        // `.word` lets the current word finish rather than clipping it.
        synthesizer.pauseSpeaking(at: .word)
        state = .paused
    }

    func resume() {
        guard state == .paused else { return }
        if synthesizer.continueSpeaking() {
            state = .playing
        } else {
            // The queue was dropped while paused, so pick the sentence back up.
            speak(chapter: currentChapter, fromChapterOffset: lastSpokenOffset)
        }
    }

    func togglePlayPause() {
        switch state {
        case .idle: break
        case .playing: pause()
        case .paused: resume()
        }
    }

    func stop() {
        activeUtterance = nil
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        state = .idle
        highlighted = nil
    }

    /// Rate can only be set on a fresh utterance, so the current sentence restarts
    /// at the new speed.
    func setRate(_ factor: Double) {
        let clamped = min(max(factor, Self.rateOptions[0]), Self.rateOptions[Self.rateOptions.count - 1])
        guard clamped != rateFactor else { return }
        rateFactor = clamped
        UserDefaults.standard.set(clamped, forKey: Self.rateKey)

        guard state != .idle else { return }
        let wasPaused = state == .paused
        speak(chapter: currentChapter, fromChapterOffset: lastSpokenOffset)
        if wasPaused { pause() }
    }

    func slower() { setRate(step(from: rateFactor, by: -1)) }
    func faster() { setRate(step(from: rateFactor, by: 1)) }

    var canSlowDown: Bool { rateFactor > Self.rateOptions[0] }
    var canSpeedUp: Bool { rateFactor < Self.rateOptions[Self.rateOptions.count - 1] }

    private func step(from current: Double, by delta: Int) -> Double {
        let options = Self.rateOptions
        let index = options.firstIndex(of: current)
            ?? options.firstIndex { $0 > current }
            ?? options.count - 1
        return options[min(max(index + delta, 0), options.count - 1)]
    }

    // MARK: - Helpers

    private func sentence(in chapter: Chapter, nearest offset: Int) -> NSRange? {
        if let containing = chapter.sentences.first(where: { NSLocationInRange(offset, $0) }) {
            return containing
        }
        // Between sentences, or past the end: take the next one, else the last.
        return chapter.sentences.first(where: { $0.location >= offset }) ?? chapter.sentences.last
    }

    private static func vietnameseVoice() -> AVSpeechSynthesisVoice? {
        if let exact = AVSpeechSynthesisVoice(language: "vi-VN") { return exact }
        return AVSpeechSynthesisVoice.speechVoices().first { $0.language.hasPrefix("vi") }
    }

    private func activateAudioSession() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
            try session.setActive(true)
            NSLog("[speech] audio session ready")
        } catch {
            NSLog("[speech] audio session failed: \(error)")
        }
    }
}

extension SpeechController: AVSpeechSynthesizerDelegate {

    func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        willSpeakRangeOfSpeechString characterRange: NSRange,
        utterance: AVSpeechUtterance
    ) {
        let absolute = utteranceBase + characterRange.location
        let chapterIndex = currentChapter
        let chapters = self.chapters

        DispatchQueue.main.async { [weak self] in
            guard let self, chapters.indices.contains(chapterIndex) else { return }
            self.lastSpokenOffset = absolute
            let chapter = chapters[chapterIndex]
            // Snap to whole sentences — highlighting word by word is noisy.
            guard let sentence = chapter.sentences.first(where: { NSLocationInRange(absolute, $0) }) else { return }
            if sentence != self.highlighted { self.highlighted = sentence }
        }
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async { [weak self] in
            guard let self, utterance === self.activeUtterance else {
                NSLog("[speech] didFinish ignored (stopped or replaced)")
                return
            }
            self.activeUtterance = nil
            let next = self.currentChapter + 1
            guard self.chapters.indices.contains(next) else {
                self.stop()
                return
            }
            self.speak(chapter: next, fromChapterOffset: 0)
        }
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        NSLog("[speech] didCancel")
        DispatchQueue.main.async { [weak self] in
            guard let self, utterance === self.activeUtterance else { return }
            self.activeUtterance = nil
            self.state = .idle
        }
    }
}
