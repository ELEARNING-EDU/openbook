import SwiftUI

/// The screen the Tuỳ chỉnh button opens: the book's type, its spacing and its
/// page layout, previewed live on the text the reader is actually looking at.
///
/// X throws the changes away, ✓ keeps them — the two buttons in the reference
/// screen, and the two behaviours a reader expects from them.
struct CustomizeThemeSheet: View {

    @ObservedObject var settings: ReaderSettings
    /// A slice of the page behind the sheet, so the preview is the book itself
    /// rather than a sample sentence.
    let sample: String
    var onClose: () -> Void

    @State private var opening = ReaderCustom()
    @State private var showFonts = false

    private var custom: ReaderCustom { settings.custom }
    private var palette: ReaderPalette { settings.palette }

    var body: some View {
        NavigationStack {
            ZStack {
                palette.swiftPaper.ignoresSafeArea()

                VStack(spacing: 0) {
                    topBar
                    Divider().overlay(palette.swiftInk.opacity(0.12))

                    ScrollViewReader { proxy in
                        ScrollView {
                            VStack(alignment: .leading, spacing: 22) {
                                preview
                                textSection
                                layoutSection
                                resetButton
                            }
                            .padding(.horizontal, 16)
                            .padding(.top, 18)
                            .padding(.bottom, 28)
                        }
                        .onAppear {
                            // The harness cannot scroll, so it can ask for the foot
                            // of the screen instead.
                            guard UserDefaults.standard.bool(forKey: "demoCustomizeBottom") else { return }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                                withAnimation { proxy.scrollTo("foot", anchor: .bottom) }
                            }
                        }
                    }
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(isPresented: $showFonts) {
                fontList
            }
        }
        .preferredColorScheme(palette.isDark ? .dark : .light)
        .onAppear {
            opening = settings.custom
            if UserDefaults.standard.bool(forKey: "demoFontList") {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) { showFonts = true }
            }
        }
    }

    // MARK: - Pieces

    private var topBar: some View {
        ZStack {
            Text("Tuỳ chỉnh giao diện")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(palette.swiftInk)

            HStack {
                Button {
                    settings.restore(opening)
                    onClose()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(palette.swiftInk)
                        .frame(width: 38, height: 38)
                        .background(palette.cardFill, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Huỷ thay đổi")

                Spacer()

                Button {
                    onClose()
                } label: {
                    Image(systemName: "checkmark")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(palette.isDark ? Color.black : Color.white)
                        .frame(width: 38, height: 38)
                        .background(palette.isDark ? Color.white : Color.black, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Giữ thay đổi")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    /// The book's own words, set the way the settings say.
    private var preview: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Aa")
                .font(previewFont(size: custom.bodySize * 1.9, weight: custom.bold ? .bold : .semibold))
                .foregroundStyle(palette.swiftInk)

            // A text view rather than a `Text`: SwiftUI cannot justify, and the
            // preview has to show justification, leading, letter and word spacing
            // and the margins honestly.
            ReaderPreview(text: sample, style: custom.applied, palette: palette)
                .frame(maxWidth: .infinity, minHeight: 40, alignment: .leading)
        }
    }

    private func previewFont(size: CGFloat, weight: UIFont.Weight) -> Font {
        Font(ReaderFonts.font(id: custom.fontID, size: size, weight: weight, italic: false))
    }

    private var textSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle("Chữ")

            VStack(spacing: 0) {
                Button {
                    showFonts = true
                } label: {
                    row(icon: "textformat.size", title: "Phông chữ", value: custom.fontName, chevron: true)
                }
                .buttonStyle(.plain)

                Divider().overlay(palette.swiftInk.opacity(0.1)).padding(.leading, 52)

                HStack(spacing: 14) {
                    rowIcon("bold")
                    Text("Chữ đậm")
                        .font(.system(size: 16))
                        .foregroundStyle(palette.swiftInk)
                    Spacer(minLength: 8)
                    Toggle("", isOn: binding(\.bold))
                        .labelsHidden()
                        .tint(palette.swiftAccent)
                }
                .padding(.horizontal, 14)
                .frame(height: 48)
            }
            .background(card)
        }
    }

    private var layoutSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle("Khuyến nghị & tuỳ chọn bố cục")

            VStack(spacing: 0) {
                HStack(spacing: 14) {
                    rowIcon("slider.horizontal.3")
                    Text("Tuỳ chỉnh")
                        .font(.system(size: 16))
                        .foregroundStyle(palette.swiftInk)
                    Spacer(minLength: 8)
                    Toggle("", isOn: binding(\.enabled))
                        .labelsHidden()
                        .tint(palette.swiftAccent)
                }
                .padding(.horizontal, 14)
                .frame(height: 48)

                Divider().overlay(palette.swiftInk.opacity(0.1)).padding(.leading, 14)

                sliderRow(
                    caption: "GIÃN DÒNG",
                    icon: "text.alignleft",
                    value: binding(\.lineSpacing),
                    range: ReaderCustom.lineSpacingRange,
                    step: 0.05,
                    label: custom.lineSpacingLabel
                )
                sliderRow(
                    caption: "GIÃN CHỮ",
                    icon: "character",
                    value: binding(\.characterSpacing),
                    range: ReaderCustom.characterSpacingRange,
                    step: 0.05,
                    label: custom.characterSpacingLabel
                )
                sliderRow(
                    caption: "GIÃN TỪ",
                    icon: "textformat.abc",
                    value: binding(\.wordSpacing),
                    range: ReaderCustom.wordSpacingRange,
                    step: 0.5,
                    label: custom.wordSpacingLabel
                )
                sliderRow(
                    caption: "LỀ",
                    icon: "rectangle.portrait",
                    value: binding(\.margin),
                    range: ReaderCustom.marginRange,
                    step: 2,
                    label: custom.marginLabel
                )
            }
            .background(card)

            VStack(spacing: 0) {
                HStack(spacing: 14) {
                    rowIcon("rectangle.split.3x1")
                    Text("Cột")
                        .font(.system(size: 16))
                        .foregroundStyle(palette.swiftInk)
                    Spacer(minLength: 8)
                    Menu {
                        ForEach(ColumnSetting.allCases) { setting in
                            Button(setting.label) {
                                settings.update { $0.columns = setting.rawValue }
                            }
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Text(custom.columnSetting.label)
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.system(size: 11, weight: .semibold))
                        }
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(palette.swiftInk)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(palette.cardFill, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                    }
                }
                .padding(.horizontal, 14)
                .frame(height: 48)

                Divider().overlay(palette.swiftInk.opacity(0.1)).padding(.leading, 14)

                HStack(spacing: 14) {
                    rowIcon("text.justify")
                    Text("Căn đều hai bên")
                        .font(.system(size: 16))
                        .foregroundStyle(palette.swiftInk)
                    Spacer(minLength: 8)
                    Toggle("", isOn: binding(\.justify))
                        .labelsHidden()
                        .tint(palette.swiftAccent)
                }
                .padding(.horizontal, 14)
                .frame(height: 48)
            }
            .background(card)
        }
    }

    private var resetButton: some View {
        Button {
            withAnimation(.easeOut(duration: 0.2)) { settings.resetCurrentTheme() }
            opening = ReaderCustom()
        } label: {
            Text("Đặt lại giao diện")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color.red)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(card)
        }
        .buttonStyle(.plain)
        .disabled(!settings.isChanged)
        .opacity(settings.isChanged ? 1 : 0.4)
        .id("foot")
    }

    private var fontList: some View {
        List {
            ForEach(ReaderFonts.all) { font in
                Button {
                    settings.update { $0.fontID = font.id }
                    showFonts = false
                } label: {
                    HStack {
                        Text(font.name)
                            .font(Font(ReaderFonts.font(
                                id: font.id,
                                size: 18,
                                weight: .regular,
                                italic: false
                            )))
                            .foregroundStyle(palette.swiftInk)
                        Spacer()
                        if font.id == custom.fontID {
                            Image(systemName: "checkmark")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(palette.swiftAccent)
                        }
                    }
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(palette.sheetPaper)
        .navigationTitle("Phông chữ")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Small parts

    private func sectionTitle(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.system(size: 12, weight: .semibold))
            .kerning(0.8)
            .foregroundStyle(palette.swiftInkSoft)
            .padding(.leading, 4)
    }

    private var card: some View {
        RoundedRectangle(cornerRadius: 16, style: .continuous).fill(palette.cardFill)
    }

    private func rowIcon(_ symbol: String) -> some View {
        Image(systemName: symbol)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(palette.swiftInk)
            .frame(width: 24)
    }

    private func row(icon: String, title: String, value: String, chevron: Bool) -> some View {
        HStack(spacing: 14) {
            rowIcon(icon)
            Text(title)
                .font(.system(size: 16))
                .foregroundStyle(palette.swiftInk)
            Spacer(minLength: 8)
            Text(value)
                .font(.system(size: 16))
                .foregroundStyle(palette.swiftInkSoft)
            if chevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(palette.swiftInkSoft)
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 48)
        .contentShape(Rectangle())
    }

    private func sliderRow(
        caption: String,
        icon: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        step: Double,
        label: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(caption)
                .font(.system(size: 11, weight: .semibold))
                .kerning(0.6)
                .foregroundStyle(palette.swiftInkSoft)
                .padding(.leading, 52)

            HStack(spacing: 12) {
                rowIcon(icon)
                Slider(value: value, in: range, step: step)
                    .tint(palette.swiftAccent)
                Text(label)
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(palette.swiftInk)
                    .frame(width: 54, alignment: .trailing)
            }
            .padding(.horizontal, 14)
            .frame(height: 40)
            .opacity(custom.enabled ? 1 : 0.35)
            .disabled(!custom.enabled)
        }
        .padding(.top, 6)
    }

    private func binding(_ key: WritableKeyPath<ReaderCustom, Bool>) -> Binding<Bool> {
        Binding(
            get: { settings.custom[keyPath: key] },
            set: { new in settings.update { $0[keyPath: key] = new } }
        )
    }

    private func binding(_ key: WritableKeyPath<ReaderCustom, Double>) -> Binding<Double> {
        Binding(
            get: { settings.custom[keyPath: key] },
            set: { new in settings.update { $0[keyPath: key] = new } }
        )
    }
}


/// The customise screen's preview: a non-scrolling text view holding the page's
/// own words, set the way the settings say.
private struct ReaderPreview: UIViewRepresentable {

    let text: String
    let style: ReaderCustom
    let palette: ReaderPalette

    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.isEditable = false
        view.isScrollEnabled = false
        view.isSelectable = false
        view.backgroundColor = .clear
        view.textContainer.lineFragmentPadding = 0
        view.setContentCompressionResistancePriority(.required, for: .vertical)
        return view
    }

    func updateUIView(_ view: UITextView, context: Context) {
        view.attributedText = AttributedBuilder.preview(text, style: style, palette: palette)
        let margin = CGFloat(style.margin)
        view.textContainerInset = UIEdgeInsets(top: 0, left: margin, bottom: 0, right: margin)
    }

    func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiView: UITextView,
        context: Context
    ) -> CGSize? {
        guard let width = proposal.width, width > 0 else { return nil }
        let fitted = uiView.sizeThatFits(
            CGSize(width: width, height: .greatestFiniteMagnitude)
        )
        return CGSize(width: width, height: fitted.height)
    }
}
