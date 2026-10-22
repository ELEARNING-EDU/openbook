import SwiftUI

/// The panel the gear opens: the looks on offer, the type size, and the way in to
/// the finer controls.
///
/// The sheet is painted in the theme being chosen, so every press shows its own
/// result immediately — there is no separate swatch to interpret. Each card also
/// shows the face the theme is wearing at the moment, so day and night are visible
/// in the grid itself rather than hidden behind the theme's name.
struct ThemesSettingsSheet: View {

    @ObservedObject var settings: ReaderSettings
    /// A slice of the page behind the sheet, so the customise screen previews the
    /// book itself.
    let sample: String

    /// Opens the customise screen straight away for the screenshot harness, which
    /// cannot tap.
    @State private var showCustomize = false

    private var palette: ReaderPalette { settings.palette }

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider().overlay(palette.swiftInk.opacity(0.15))
            controls
            grid
            customize
            Spacer(minLength: 0)
        }
        .background(palette.sheetPaper)
        .overlay(alignment: .top) {
            // The sheet's own edge: a hairline, so a panel on a night page is not
            // asked to be found by its shadow alone.
            Rectangle()
                .fill(palette.sheetEdge)
                .frame(height: 0.5)
                .offset(y: -0.25)
        }
        .preferredColorScheme(palette.isDark ? .dark : .light)
        .onAppear {
            guard UserDefaults.standard.bool(forKey: "demoCustomize") else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) { showCustomize = true }
        }
        .sheet(isPresented: $showCustomize) {
            CustomizeThemeSheet(settings: settings, sample: sample) {
                showCustomize = false
            }
        }
    }

    // MARK: - Pieces

    /// Just the panel's name: it is closed by pulling it down, the way a sheet is,
    /// rather than by a button that competes with the looks in the grid.
    private var header: some View {
        Text("Giao diện & Cài đặt")
            .font(.system(size: 17, weight: .bold))
            .foregroundStyle(palette.swiftInk)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 14)
    }

    /// Type size on the left, day / night on the right — the two things a reader
    /// reaches for most. The size is the open theme's own, so Original and Focus
    /// can sit at different sizes.
    private var controls: some View {
        HStack(spacing: 12) {
            HStack(spacing: 0) {
                sizeButton(symbol: "A", size: 17, weight: .medium,
                           enabled: settings.canShrinkText, action: settings.shrinkText)
                Rectangle()
                    .fill(palette.swiftInk.opacity(0.18))
                    .frame(width: 1, height: 34)
                sizeButton(symbol: "A", size: 26, weight: .bold,
                           enabled: settings.canGrowText, action: settings.growText)
            }
            .frame(height: 66)
            .background(panelFill, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

            Button(action: settings.toggleAppearance) {
                Image(systemName: palette.isDark ? "sun.max.fill" : "moon.stars.fill")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(palette.swiftInk)
                    .frame(width: 84, height: 66)
                    .background(panelFill, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(palette.isDark ? "Chuyển sang nền sáng" : "Chuyển sang nền tối")
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
    }

    private var grid: some View {
        LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3),
            spacing: 12
        ) {
            ForEach(ReaderTheme.themes) { spec in
                ThemeCard(
                    palette: spec.palette(dark: settings.isDarkAppearance),
                    selected: spec.id == settings.themeID,
                    customised: settings.isCustomised(spec.id),
                    ring: palette.selectionRing
                ) {
                    withAnimation(.easeOut(duration: 0.16)) { settings.setTheme(spec.id) }
                }
            }
        }
        .padding(.horizontal, 20)
    }

    private var customize: some View {
        Button {
            showCustomize = true
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "textformat")
                    .font(.system(size: 15, weight: .semibold))
                Text("Tuỳ chỉnh")
                    .font(.system(size: 17, weight: .semibold))
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .bold))
                    .opacity(0.55)
            }
            .foregroundStyle(palette.swiftInk)
            .padding(.horizontal, 20)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(panelFill, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 20)
        .padding(.top, 18)
        .accessibilityHint("Mở màn tuỳ chỉnh phông chữ, giãn dòng và bố cục")
    }

    private var panelFill: Color { palette.isDark ? Color.white.opacity(0.13) : Color.black.opacity(0.05) }

    private func sizeButton(
        symbol: String,
        size: CGFloat,
        weight: Font.Weight,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(symbol)
                .font(.system(size: size, weight: weight, design: .serif))
                .foregroundStyle(palette.swiftInk)
                .frame(maxWidth: .infinity)
                .frame(height: 66)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.3)
    }
}

/// One look in the grid: the theme's own paper and ink, so choosing it is just
/// picking the swatch that already looks right. The asterisk marks a theme that
/// has been changed away from the way it was designed.
private struct ThemeCard: View {

    let palette: ReaderPalette
    let selected: Bool
    let customised: Bool
    let ring: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Text("Aa")
                    .font(.system(size: 30, weight: palette.bold ? .bold : .semibold, design: .serif))
                    .foregroundStyle(palette.swiftInk)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                Text(palette.name)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(palette.swiftInk)
            }
            .padding(.vertical, 10)
            .frame(height: 104)
            .background(palette.swiftPaper, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(alignment: .topTrailing) {
                if customised {
                    Text("*")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(palette.swiftInk)
                        .padding(.top, 4)
                        .padding(.trailing, 8)
                        .accessibilityLabel("Đã tuỳ chỉnh")
                }
            }
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(ring, lineWidth: selected ? 3 : 0)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(palette.swiftInk.opacity(0.14), lineWidth: selected ? 0 : 1)
            )
            .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(customised ? "\(palette.name), đã tuỳ chỉnh" : palette.name)
        .accessibilityAddTraits(selected ? [.isSelected] : [])
    }
}
