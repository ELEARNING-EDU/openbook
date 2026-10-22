import CoreText
import UIKit

/// Turns a chapter's XHTML into an `NSAttributedString` styled for reading.
///
/// The HTML importer is main-thread only, which is why this sits apart from
/// `EpubArchiveReader`.
///
/// Every value that changes how the text measures — face, size, leading,
/// letter and word spacing, margins — is a parameter, not a constant: the
/// paginator measures the attributed string and the page renders it, so both
/// must be handed the very same values or lines overflow the page.
enum AttributedBuilder {

    static let defaultBodySize = CGFloat(ReaderMetrics.baseBodySize)
    static let defaultLineSpacing = 1.14

    static func build(
        html: Data,
        style: ReaderCustom = ReaderCustom(),
        palette: ReaderPalette,
        images: [String: Data] = [:],
        columnWidth: CGFloat = 0,
        maxImageHeight: CGFloat = 0
    ) -> NSAttributedString {
        // The markup cannot be handed over as-is: the importer has no base URL, so
        // an `<img>` would vanish. Each tag leaves a token behind instead, and the
        // picture is put back as a text attachment below, once the string has its
        // final fonts and the column it will be measured in is known.
        let rewritten = ChapterImages.replacingTags(in: String(decoding: html, as: UTF8.self))

        let options: [NSAttributedString.DocumentReadingOptionKey: Any] = [
            .documentType: NSAttributedString.DocumentType.html,
            .characterEncoding: String.Encoding.utf8.rawValue
        ]

        guard let imported = try? NSAttributedString(
            data: Data(rewritten.html.utf8),
            options: options,
            documentAttributes: nil
        ) else {
            return NSAttributedString(string: "")
        }

        let result = NSMutableAttributedString(attributedString: imported)
        let full = NSRange(location: 0, length: result.length)
        guard full.length > 0 else { return result }

        let bodySize = CGFloat(style.bodySize)
        let letterSpacing = CGFloat(style.characterSpacing)

        result.addAttributes(
            [
                .foregroundColor: palette.ink,
                .paragraphStyle: paragraphStyle(bodySize: bodySize, style: style),
                .kern: letterSpacing
            ],
            range: full
        )

        // The importer brings its own fonts. Re-map them onto the theme's reading
        // face, preserving bold/italic and pulling oversized headings down to a
        // sane scale.
        var replacements: [(NSRange, UIFont)] = []
        result.enumerateAttribute(.font, in: full, options: []) { value, range, _ in
            let original = (value as? UIFont) ?? UIFont.systemFont(ofSize: bodySize)
            replacements.append(
                (range, readerFont(matching: original, bodySize: bodySize, style: style, palette: palette))
            )
        }
        for (range, font) in replacements {
            result.addAttribute(.font, value: font, range: range)
        }

        // Word spacing: extra air after every space. UIKit has no word-spacing
        // attribute, so the space runs carry it on top of the letter spacing.
        if style.wordSpacing > 0 {
            let spaces = (result.string as NSString).ranges(of: .whitespacesAndNewlines)
            for range in spaces {
                result.addAttribute(
                    .kern,
                    value: letterSpacing + CGFloat(style.wordSpacing),
                    range: range
                )
            }
        }

        placePictures(
            rewritten.references,
            in: result,
            images: images,
            style: style,
            palette: palette,
            columnWidth: columnWidth,
            maxImageHeight: maxImageHeight
        )

        return result
    }

    /// The settings screen's preview: the reader's own attributes applied to a
    /// plain slice of the page. It goes through the very same face, leading,
    /// spacing and justification as the book, so what the switches and sliders
    /// show is what the page will do.
    static func preview(
        _ text: String,
        style: ReaderCustom,
        palette: ReaderPalette
    ) -> NSAttributedString {
        let result = NSMutableAttributedString(string: text)
        let full = NSRange(location: 0, length: result.length)
        guard full.length > 0 else { return result }

        let bodySize = CGFloat(style.bodySize)
        let letterSpacing = CGFloat(style.characterSpacing)

        result.addAttributes(
            [
                .foregroundColor: palette.ink,
                .font: ReaderFonts.font(
                    id: style.fontID,
                    size: bodySize,
                    weight: (style.bold || palette.bold) ? .semibold : .regular,
                    italic: false
                ),
                .paragraphStyle: paragraphStyle(bodySize: bodySize, style: style),
                .kern: letterSpacing
            ],
            range: full
        )

        if style.wordSpacing > 0 {
            let spaces = (result.string as NSString).ranges(of: .whitespacesAndNewlines)
            for range in spaces {
                result.addAttribute(
                    .kern,
                    value: letterSpacing + CGFloat(style.wordSpacing),
                    range: range
                )
            }
        }

        return result
    }

    // MARK: - Pictures

    /// Pictures already cut to a column, so a re-typeset does not decode them all
    /// over again. Keyed by the bytes and the size they were cut to.
    private static let pictureCache = NSCache<NSString, UIImage>()

    /// Puts every token back as either the picture or a line of alt text.
    ///
    /// Replacing in place keeps the text either side of the picture exactly where
    /// the importer left it; only the token itself changes length.
    private static func placePictures(
        _ references: [ChapterImageReference],
        in text: NSMutableAttributedString,
        images: [String: Data],
        style: ReaderCustom,
        palette: ReaderPalette,
        columnWidth: CGFloat,
        maxImageHeight: CGFloat
    ) {
        guard !references.isEmpty else { return }
        let bodySize = CGFloat(style.bodySize)

        for (index, reference) in references.enumerated() {
            let token = ChapterImages.token(for: index)
            guard let found = text.string.range(of: token) else { continue }
            let range = NSRange(found, in: text.string)

            // Width is not known before the page geometry is; the reader re-typesets
            // once it is, so an empty column here only means "not yet".
            let picture = columnWidth > 40
                ? pictureImage(
                    for: reference.src,
                    images: images,
                    columnWidth: columnWidth,
                    maxImageHeight: maxImageHeight
                )
                : nil

            if UserDefaults.standard.bool(forKey: "demoDebug") {
                print("[img] token=\(index) src=\(reference.src) column=\(Int(columnWidth))x\(Int(maxImageHeight)) data=\(images[reference.src]?.count ?? 0) picture=\(picture.map { "\(Int($0.size.width))x\(Int($0.size.height))" } ?? "nil")")
            }

            let piece: NSAttributedString
            if let picture {
                let attachment = NSTextAttachment()
                attachment.image = picture
                // Bottom edge on the baseline; the paragraph's own spacing carries
                // the air above and below.
                attachment.bounds = CGRect(
                    x: 0,
                    y: 0,
                    width: picture.size.width,
                    height: picture.size.height
                )
                let image = NSMutableAttributedString(attachment: attachment)
                // CoreText measures the placeholder character, not the attachment:
                // without a run delegate that carries the picture's height, the
                // paginator believes a tall picture fits in the space left over and
                // the text view then clips it. The delegate makes both agree.
                if let delegate = runDelegate(for: picture.size) {
                    image.addAttribute(
                        NSAttributedString.Key(kCTRunDelegateAttributeName as String),
                        value: delegate,
                        range: NSRange(location: 0, length: image.length)
                    )
                }
                piece = image
            } else {
                let label = reference.alt.isEmpty ? "[Ảnh]" : "[Ảnh: \(reference.alt)]"
                piece = NSAttributedString(
                    string: label,
                    attributes: [
                        .font: ReaderFonts.font(
                            id: style.fontID,
                            size: bodySize * 0.85,
                            weight: .regular,
                            italic: true
                        ),
                        .foregroundColor: palette.inkSoft
                    ]
                )
            }

            text.replaceCharacters(in: range, with: piece)
            centrePicture(
                at: NSRange(location: range.location, length: piece.length),
                in: text,
                bodySize: bodySize
            )
        }
    }

    /// The picture cut down to the column, or nil when it cannot be read — an SVG,
    /// a corrupt file, a `src` that was not in the archive. The caller then shows
    /// the alt text, which is better than a hole.
    private static func pictureImage(
        for src: String,
        images: [String: Data],
        columnWidth: CGFloat,
        maxImageHeight: CGFloat
    ) -> UIImage? {
        guard let data = images[src] else { return nil }
        let limit = maxImageHeight > 80 ? maxImageHeight : columnWidth * 1.6
        let key = "\(data.hashValue)-\(Int(columnWidth))x\(Int(limit))" as NSString
        if let cached = pictureCache.object(forKey: key) { return cached }

        guard let image = UIImage(data: data), image.size.width > 0, image.size.height > 0 else {
            return nil
        }

        var scale = min(1, columnWidth / image.size.width)
        if image.size.height * scale > limit {
            scale = limit / image.size.height
        }
        let target = CGSize(
            width: max(1, (image.size.width * scale).rounded(.down)),
            height: max(1, (image.size.height * scale).rounded(.down))
        )

        let drawn: UIImage
        if target.width >= image.size.width - 1, target.height >= image.size.height - 1 {
            drawn = image
        } else {
            let format = UIGraphicsImageRendererFormat.default()
            format.scale = image.scale
            format.opaque = false
            drawn = UIGraphicsImageRenderer(size: target, format: format).image { _ in
                image.draw(in: CGRect(origin: .zero, size: target))
            }
        }
        pictureCache.setObject(drawn, forKey: key)
        return drawn
    }

    /// A CoreText delegate that reports one picture's size to the paginator.
    ///
    /// The size travels in a boxed `NSValue` that the delegate owns and releases in
    /// its own `dealloc`, so nothing outlives the attributed string it belongs to.
    private static func runDelegate(for size: CGSize) -> CTRunDelegate? {
        let box = NSValue(cgSize: size)
        let pointer = Unmanaged.passRetained(box).toOpaque()
        var callbacks = CTRunDelegateCallbacks(
            version: kCTRunDelegateVersion1,
            dealloc: { reference in
                Unmanaged<NSValue>.fromOpaque(reference).release()
            },
            getAscent: { reference in
                Unmanaged<NSValue>.fromOpaque(reference).takeUnretainedValue().cgSizeValue.height
            },
            getDescent: { _ in 0 },
            getWidth: { reference in
                Unmanaged<NSValue>.fromOpaque(reference).takeUnretainedValue().cgSizeValue.width
            }
        )
        return CTRunDelegateCreate(&callbacks, pointer)
    }

    /// A picture stands alone: no justification, and a little air above and below.
    private static func centrePicture(at range: NSRange, in text: NSMutableAttributedString, bodySize: CGFloat) {
        guard text.length > 0, range.location < text.length else { return }
        let paragraph = NSMutableParagraphStyle()
        paragraph.alignment = .center
        paragraph.lineHeightMultiple = 1.0
        paragraph.paragraphSpacingBefore = bodySize * 0.45
        paragraph.paragraphSpacing = bodySize * 0.75
        let whole = (text.string as NSString).paragraphRange(
            for: NSRange(location: range.location, length: 0)
        )
        text.addAttribute(.paragraphStyle, value: paragraph, range: whole)
    }

    private static func paragraphStyle(bodySize: CGFloat, style: ReaderCustom) -> NSParagraphStyle {
        let paragraph = NSMutableParagraphStyle()
        paragraph.lineHeightMultiple = CGFloat(style.lineSpacing)
        // Keeps the break between paragraphs proportional to the text size.
        paragraph.paragraphSpacing = bodySize * 0.58
        paragraph.alignment = style.justify ? .justified : .natural
        paragraph.hyphenationFactor = 0.9
        return paragraph
    }

    private static func readerFont(
        matching original: UIFont,
        bodySize: CGFloat,
        style: ReaderCustom,
        palette: ReaderPalette
    ) -> UIFont {
        let traits = original.fontDescriptor.symbolicTraits
        let isHeading = original.pointSize >= 22
        let size = isHeading ? bodySize * 1.45 : bodySize

        var weight: UIFont.Weight = (isHeading || traits.contains(.traitBold)) ? .semibold : .regular
        if style.bold || palette.bold {
            weight = weight == .regular ? .semibold : .bold
        }

        return ReaderFonts.font(
            id: style.fontID,
            size: size,
            weight: weight,
            italic: traits.contains(.traitItalic)
        )
    }
}

private extension NSString {

    /// Every run of whitespace in the string.
    func ranges(of set: CharacterSet) -> [NSRange] {
        var found: [NSRange] = []
        var index = 0
        while index < length {
            let range = self.rangeOfCharacter(from: set, options: [], range: NSRange(location: index, length: length - index))
            guard range.location != NSNotFound else { break }
            var end = range.location + range.length
            while end < length, character(at: end).isSpace { end += 1 }
            found.append(NSRange(location: range.location, length: end - range.location))
            index = end
        }
        return found
    }
}

private extension unichar {
    var isSpace: Bool {
        guard let scalar = Unicode.Scalar(self) else { return false }
        return CharacterSet.whitespacesAndNewlines.contains(scalar)
    }
}
