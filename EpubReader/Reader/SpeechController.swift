import AVFoundation
import Foundation
import MediaPlayer
import NaturalLanguage
import UIKit

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
/// One instance for the whole app: the voice is the reading session, not the
/// screen that opened it. A reader may leave the book — or the app may go to the
/// background — and the reading carries on; the lock screen and Control Centre
/// control the same engine through `MPRemoteCommandCenter`.
///
/// Everything that touches published state runs on the main thread; the
/// synthesizer's callbacks arrive on an internal queue.
final class SpeechController: NSObject, ObservableObject {

    static let shared = SpeechController()

    static let rateOptions: [Double] = [0.5, 0.75, 1.0, 1.25, 1.5]
    /// Minutes the player can be told to stop after; `nil` is "no timer".
    static let sleepOptions: [Int] = [5, 15, 30, 60]
    private static let rateKey = "narrationRateFactor"
    private static let voiceKey = "narrationVoiceID"

    @Published private(set) var state: NarrationState = .idle
    /// Range of the sentence being spoken, in chapter coordinates.
    @Published private(set) var highlighted: NSRange?
    @Published private(set) var currentChapter = 0
    @Published private(set) var voiceName = "—"
    @Published private(set) var rateFactor: Double = 1.0
    /// The chosen voice, by its system identifier.
    @Published private(set) var voiceID: String?
    /// The language the book is written in, worked out from its text first and
    /// its package tag second.
    @Published private(set) var bookLanguage: String?
    /// Minutes left on the sleep timer, or `nil` when it is off.
    @Published private(set) var sleepMinutes: Int?
    private var sleepTask: Task<Void, Never>?

    /// The book currently loaded into the voice. The app uses it to come back to
    /// the right book when the now-playing controls bring it forward.
    private(set) var book: BookItem?
    private(set) var bookID: String?

    private let synthesizer = AVSpeechSynthesizer()
    /// AVAudioSession activation blocks, and the runtime flags it as a main-thread
    /// hang risk: every call goes through this serial queue instead.
    private let audioQueue = DispatchQueue(
        label: "com.elearning.openbook.audio-session",
        qos: .userInitiated
    )
    private var sessionReady = false
    private var activatingSession = false
    private var activationWaiters: [() -> Void] = []
    private var chapters: [Chapter] = []
    private var chapterTitles: [String] = []
    /// Where the current utterance starts inside its chapter string.
    private var utteranceBase = 0
    /// The utterance we still consider "ours". `stopSpeaking` reports `didFinish`
    /// rather than `didCancel`, so a user-initiated stop has to be told apart from
    /// the end of a chapter by clearing this first.
    private var activeUtterance: AVSpeechUtterance?
    /// Where the voice last reached, so a speed change can resume in the same
    /// sentence instead of jumping back to the top of the page.
    private var lastSpokenOffset = 0
    /// The page the reader is looking at, while a reader is open. Play follows it,
    /// so a page turned during a pause is where the voice picks up.
    private(set) var visiblePage: VisiblePage?
    /// The page the paused sentence was printed on, captured when the pause
    /// happened. `nil` when the reader was closed at that moment.
    private var pausedPage: Int?
    private var artwork: MPMediaItemArtwork?
    private var interruptionObserver: NSObjectProtocol?
    private var routeObserver: NSObjectProtocol?

    var isSpeaking: Bool { state == .playing }
    var isBusy: Bool { state != .idle }
    var rateLabel: String { String(format: "%g×", rateFactor) }

    /// The page on screen, with the sentence Play should start from when the
    /// reader has moved away from the paused one.
    struct VisiblePage: Equatable {
        let index: Int
        let chapter: Int
        let firstSentence: Int
    }

    /// The reader owns this: call it on every page change, and with `nil` when
    /// the book closes.
    func setVisiblePage(_ page: VisiblePage?) {
        visiblePage = page
    }

    override init() {
        super.init()
        synthesizer.delegate = self

        let saved = UserDefaults.standard.double(forKey: Self.rateKey)
        rateFactor = Self.rateOptions.contains(saved) ? saved : 1.0
        voiceID = UserDefaults.standard.string(forKey: Self.voiceKey)

        ensureAudioSession {}
        configureRemoteCommands()
        observeAudioChanges()
    }

    deinit {
        if let interruptionObserver {
            NotificationCenter.default.removeObserver(interruptionObserver)
        }
        if let routeObserver {
            NotificationCenter.default.removeObserver(routeObserver)
        }
    }

    // MARK: - The book

    /// Loads a book into the voice.
    ///
    /// Opening the book that is already reading keeps the voice exactly where it
    /// is — that is what makes reopening a book from the shelf or from the lock
    /// screen continue the narration instead of starting it again. A different
    /// book stops the old one first.
    func openBook(_ book: BookItem, chapters: [Chapter], languageTag: String?) {
        let sameBook = bookID == book.id && self.chapters.count == chapters.count

        if !sameBook {
            if bookID != nil { stop() }
            bookID = book.id
            artwork = MPMediaItemArtwork(boundsSize: CGSize(width: 512, height: 512)) { _ in
                BookArtwork.image(for: book.title)
            }
            bookLanguage = BookLanguage.resolve(
                tag: languageTag,
                sample: Self.sampleText(from: chapters)
            )
            currentChapter = min(currentChapter, max(0, chapters.count - 1))
        }

        self.book = book
        self.chapters = chapters
        chapterTitles = chapters.map(\.title)
        updateNowPlaying()
    }

    /// A re-typeset changes the chapter objects but not the book. The reader stops
    /// the voice before rebuilding, so only the text and its language are replaced.
    func adopt(chapters: [Chapter], languageTag: String?) {
        self.chapters = chapters
        chapterTitles = chapters.map(\.title)
        bookLanguage = BookLanguage.resolve(
            tag: languageTag,
            sample: Self.sampleText(from: chapters)
        )
        updateNowPlaying()
    }

    private static func sampleText(from chapters: [Chapter]) -> String {
        for chapter in chapters {
            let text = chapter.text.string
            if text.count > 20 { return String(text.prefix(1200)) }
        }
        return chapters.first.map { String($0.text.string.prefix(1200)) } ?? ""
    }

    // MARK: - Reading

    /// Starts narration at the sentence containing `offset` (chapter coordinates),
    /// once the audio session is active.
    func speak(chapter index: Int, fromChapterOffset offset: Int) {
        ensureAudioSession { [weak self] in
            self?.startSpeaking(chapter: index, fromChapterOffset: offset)
        }
    }

    private func startSpeaking(chapter index: Int, fromChapterOffset offset: Int) {
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
        if let voice = selectedVoice {
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
        updateNowPlaying()
    }

    func pause() {
        guard state == .playing else { return }
        // `.word` lets the current word finish rather than clipping it.
        synthesizer.pauseSpeaking(at: .word)
        pausedPage = visiblePage?.index
        state = .paused
        updateNowPlaying()
    }

    func resume() {
        guard state == .paused else { return }
        // The reader may have turned away while the voice was paused: Play reads
        // the page on screen, not the sentence it stopped on. `speak` drops the
        // paused utterance before queueing the new one.
        let start = NarrationMath.resumeStart(
            pausedPage: pausedPage,
            visiblePage: visiblePage?.index
        )
        if start == .visiblePage, let page = visiblePage {
            speak(chapter: page.chapter, fromChapterOffset: page.firstSentence)
        } else if synthesizer.continueSpeaking() {
            state = .playing
        } else {
            // The queue was dropped while paused, so pick the sentence back up.
            speak(chapter: currentChapter, fromChapterOffset: lastSpokenOffset)
        }
        updateNowPlaying()
    }

    func togglePlayPause() {
        switch state {
        case .idle: break
        case .playing: pause()
        case .paused: resume()
        }
    }

    func stop() {
        NSLog("[speech] stop \(bookID ?? "none")")
        activeUtterance = nil
        if synthesizer.isSpeaking || synthesizer.isPaused {
            synthesizer.stopSpeaking(at: .immediate)
        }
        state = .idle
        highlighted = nil
        updateNowPlaying()
    }

    /// Rate can only be set on a fresh utterance, so the current sentence restarts
    /// at the new speed.
    func setRate(_ factor: Double) {
        let clamped = min(max(factor, Self.rateOptions[0]), Self.rateOptions[Self.rateOptions.count - 1])
        guard clamped != rateFactor else { return }
        rateFactor = clamped
        UserDefaults.standard.set(clamped, forKey: Self.rateKey)

        guard state != .idle else {
            updateNowPlaying()
            return
        }
        let wasPaused = state == .paused
        speak(chapter: currentChapter, fromChapterOffset: lastSpokenOffset)
        if wasPaused { pause() }
    }

    func slower() { setRate(step(from: rateFactor, by: -1)) }
    func faster() { setRate(step(from: rateFactor, by: 1)) }

    var canSlowDown: Bool { rateFactor > Self.rateOptions[0] }
    var canSpeedUp: Bool { rateFactor < Self.rateOptions[Self.rateOptions.count - 1] }

    // MARK: - Seeking

    /// Moves the narration a whole chapter at a time — the player's skip buttons
    /// and the lock screen's previous/next. Chapters with no words (a cover, a
    /// picture-only page) are stepped over rather than become a dead stop.
    func skipChapter(by delta: Int) {
        guard let target = playableChapter(from: currentChapter, by: delta) else { return }
        let wasPaused = state == .paused
        speak(chapter: target, fromChapterOffset: 0)
        if wasPaused { pause() }
    }

    var canSkipBackward: Bool { playableChapter(from: currentChapter, by: -1) != nil }
    var canSkipForward: Bool { playableChapter(from: currentChapter, by: 1) != nil }

    private func playableChapter(from index: Int, by delta: Int) -> Int? {
        guard !chapters.isEmpty, delta != 0 else { return nil }
        var next = index + delta
        while chapters.indices.contains(next) {
            if !chapters[next].sentences.isEmpty { return next }
            next += delta
        }
        return nil
    }

    // MARK: - Sleep timer

    func setSleepTimer(minutes: Int?) {
        sleepTask?.cancel()
        sleepTask = nil
        sleepMinutes = minutes
        guard let minutes else { return }

        sleepTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(minutes) * 60 * 1_000_000_000)
            guard !Task.isCancelled else { return }
            await MainActor.run {
                guard let self else { return }
                self.sleepMinutes = nil
                self.pause()
            }
        }
    }

    // MARK: - Voices

    /// Every voice installed on the phone, so the picker can offer them all
    /// grouped by language and region.
    static func availableVoices() -> [AVSpeechSynthesisVoice] {
        AVSpeechSynthesisVoice.speechVoices()
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    /// The voice that will read right now — the saved one when it speaks the
    /// book's language, otherwise the best fit for the book.
    var selectedVoiceID: String? {
        selectedVoice?.identifier
    }

    /// Shown beside the speed control in the player.
    var currentVoiceName: String {
        selectedVoice?.name ?? "—"
    }

    /// Changing voice mid-sentence restarts that sentence in the new voice, the
    /// same trade `setRate` makes.
    func setVoice(id: String) {
        guard id != voiceID else { return }
        voiceID = id
        UserDefaults.standard.set(id, forKey: Self.voiceKey)

        guard state != .idle else { return }
        let wasPaused = state == .paused
        speak(chapter: currentChapter, fromChapterOffset: lastSpokenOffset)
        if wasPaused { pause() }
    }

    private var selectedVoice: AVSpeechSynthesisVoice? {
        if let id = voiceID,
           let chosen = AVSpeechSynthesisVoice(identifier: id),
           Self.speaks(chosen, bookLanguage) {
            return chosen
        }
        return Self.defaultVoice(for: bookLanguage)
    }

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

    /// Whether a voice can read the given language tag, e.g. `vi` matches `vi-VN`.
    static func speaks(_ voice: AVSpeechSynthesisVoice, _ language: String?) -> Bool {
        guard let language else { return true }
        return languageCode(voice.language) == languageCode(language)
    }

    private static func languageCode(_ tag: String) -> String {
        String(tag.split(separator: "-").first ?? Substring(tag)).lowercased()
    }

    /// The voice used until the reader picks one: one for the book's own
    /// language when installed, then the phone's language, then Vietnamese,
    /// then anything installed.
    static func defaultVoice(for language: String? = nil) -> AVSpeechSynthesisVoice? {
        let installed = AVSpeechSynthesisVoice.speechVoices()
        if let language,
           let voice = installed.first(where: { speaks($0, language) }) {
            return voice
        }
        if let device = Locale.preferredLanguages.first,
           let voice = AVSpeechSynthesisVoice(language: device) {
            return voice
        }
        if let exact = AVSpeechSynthesisVoice(language: "vi-VN") { return exact }
        return installed.first { $0.language.hasPrefix("vi") } ?? installed.first
    }

    /// Configures and activates the session on the audio queue, then runs `work`
    /// on the main thread with the session already active — the synthesizer must
    /// never speak before it. Repeated asks while the first activation is in
    /// flight wait for that same activation instead of starting another.
    private func ensureAudioSession(_ work: @escaping () -> Void) {
        if sessionReady {
            work()
            return
        }
        activationWaiters.append(work)
        guard !activatingSession else { return }
        activatingSession = true
        audioQueue.async { [weak self] in
            let session = AVAudioSession.sharedInstance()
            do {
                // Never add `.duckOthers`: it implies `.mixWithOthers`, and a
                // mixable session can never own the Now Playing slot, so the lock
                // screen and Control Centre would ignore this app's controls.
                try session.setCategory(
                    .playback,
                    mode: .spokenAudio,
                    options: [.allowBluetoothA2DP, .allowAirPlay]
                )
                try session.setActive(true)
                NSLog("[speech] audio session ready")
            } catch {
                NSLog("[speech] audio session failed: \(error)")
            }
            DispatchQueue.main.async {
                guard let self else { return }
                self.sessionReady = true
                self.activatingSession = false
                let waiting = self.activationWaiters
                self.activationWaiters.removeAll()
                waiting.forEach { $0() }
            }
        }
    }

    /// The system deactivated the session (an interruption, or the call end that
    /// releases it): activate again in the background.
    private func reactivateAudioSession() {
        sessionReady = false
        ensureAudioSession {}
    }

    private func observeAudioChanges() {
        let center = NotificationCenter.default

        interruptionObserver = center.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: nil,
            queue: .main
        ) { [weak self] note in
            guard let self,
                  let raw = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
                  let type = AVAudioSession.InterruptionType(rawValue: raw)
            else { return }
            switch type {
            case .began:
                self.sessionReady = false
                self.pause()
            case .ended:
                // Become audible again, but never start reading by itself: the
                // reader may have put the book down when the call arrived.
                self.reactivateAudioSession()
            @unknown default:
                break
            }
        }

        routeObserver = center.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] note in
            guard let self,
                  let raw = note.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
                  let reason = AVAudioSession.RouteChangeReason(rawValue: raw)
            else { return }
            // Headphones pulled out: stop reading into the room.
            if reason == .oldDeviceUnavailable { self.pause() }
        }
    }

    // MARK: - Lock screen and Control Centre

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()

        center.playCommand.isEnabled = true
        center.pauseCommand.isEnabled = true
        center.togglePlayPauseCommand.isEnabled = true
        center.nextTrackCommand.isEnabled = true
        center.previousTrackCommand.isEnabled = true
        center.stopCommand.isEnabled = true
        center.changePlaybackPositionCommand.isEnabled = false
        center.skipForwardCommand.isEnabled = false
        center.skipBackwardCommand.isEnabled = false

        center.playCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async {
                guard let self else { return }
                switch self.state {
                case .paused:
                    self.resume()
                case .idle:
                    if !self.chapters.isEmpty {
                        self.speak(
                            chapter: self.currentChapter,
                            fromChapterOffset: self.lastSpokenOffset
                        )
                    }
                case .playing:
                    break
                }
            }
            return .success
        }

        center.pauseCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async { self?.pause() }
            return .success
        }

        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async { self?.togglePlayPause() }
            return .success
        }

        center.nextTrackCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async { self?.skipChapter(by: 1) }
            return .success
        }

        center.previousTrackCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async { self?.skipChapter(by: -1) }
            return .success
        }

        center.stopCommand.addTarget { [weak self] _ in
            DispatchQueue.main.async { self?.stop() }
            return .success
        }
    }

    /// How the Now Playing bar measures the book. A spoken book has no fixed
    /// runtime, so duration and elapsed count characters at a nominal pace; the
    /// system scales them by `rateFactor`, which matches the voice's rate.
    private static let charactersPerSecond: Double = 15

    private var elapsedNarrationSeconds: Double {
        let total = chapters.reduce(0) { $0 + $1.text.length }
        let before = chapters.prefix(max(currentChapter, 0))
            .reduce(0) { $0 + $1.text.length }
        let offset = min(max(lastSpokenOffset, 0), max(total - before, 0))
        return Double(before + offset) / Self.charactersPerSecond
    }

    private var narrationSeconds: Double {
        let total = chapters.reduce(0) { $0 + $1.text.length }
        return max(Double(total) / Self.charactersPerSecond, 1)
    }

    /// Puts the book on the lock screen: its cover, title and chapter, plus the
    /// three controls (previous chapter, play/pause, next chapter).
    private func updateNowPlaying() {
        let center = MPNowPlayingInfoCenter.default()

        guard let book, state != .idle else {
            center.nowPlayingInfo = nil
            center.playbackState = .stopped
            if UserDefaults.standard.bool(forKey: "demoDebug") {
                NSLog("[nowplaying] cleared")
            }
            return
        }

        var info: [String: Any] = [
            MPMediaItemPropertyTitle: book.title,
            MPMediaItemPropertyArtist: book.author,
            MPMediaItemPropertyAlbumTitle: chapterTitle(currentChapter),
            MPMediaItemPropertyPlaybackDuration: narrationSeconds,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: elapsedNarrationSeconds,
            MPNowPlayingInfoPropertyMediaType: MPNowPlayingInfoMediaType.audio.rawValue,
            MPNowPlayingInfoPropertyPlaybackRate: state == .playing ? rateFactor : 0,
            MPNowPlayingInfoPropertyDefaultPlaybackRate: rateFactor,
            MPNowPlayingInfoPropertyExternalContentIdentifier: book.id
        ]
        if let artwork {
            info[MPMediaItemPropertyArtwork] = artwork
        }

        center.nowPlayingInfo = info
        center.playbackState = state == .playing ? .playing : .paused

        if UserDefaults.standard.bool(forKey: "demoDebug") {
            NSLog(
                "[nowplaying] \(state) chapter \(currentChapter) "
                + "title \(book.title) album \(chapterTitle(currentChapter)) "
                + "artwork \(artwork != nil)"
            )
        }
    }

    private func chapterTitle(_ index: Int) -> String {
        guard chapterTitles.indices.contains(index) else { return book?.title ?? "" }
        let title = chapterTitles[index]
        return title.isEmpty ? (book?.title ?? "") : title
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
            if sentence != self.highlighted {
                self.highlighted = sentence
                if UserDefaults.standard.bool(forKey: "demoDebug") {
                    NSLog("[speech] sounding \(sentence.location)+\(sentence.length)")
                }
            }
        }
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async { [weak self] in
            guard let self, utterance === self.activeUtterance else {
                NSLog("[speech] didFinish ignored (stopped or replaced)")
                return
            }
            self.activeUtterance = nil
            // Step over chapters that carry no words: a cover page must not stop
            // the reading, and neither must a run of picture-only pages.
            guard let next = self.playableChapter(from: self.currentChapter, by: 1) else {
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
            self.updateNowPlaying()
        }
    }
}
