import SwiftUI

/// A store that is honestly a preview: no server, no purchases and no catalogue
/// behind it. It exists to show where the store will sit in the app.
struct StoreView: View {

    @Environment(\.colorScheme) private var colorScheme

    private var palette: ReaderPalette { .shell(dark: colorScheme == .dark) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text("store.demo")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .tracking(1.2)
                        .foregroundStyle(palette.swiftAccent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(palette.swiftAccent.opacity(0.12)))

                    Text("store.notConnected")
                        .font(.system(size: 22, weight: .semibold, design: .serif))
                        .foregroundStyle(palette.swiftInk)

                    Text("store.body")
                        .font(.callout)
                        .foregroundStyle(palette.swiftInkSoft)
                        .fixedSize(horizontal: false, vertical: true)

                    VStack(alignment: .leading, spacing: 14) {
                        unavailableRow("wifi.slash", key: "store.noServer")
                        unavailableRow("creditcard", key: "store.noPayment")
                        unavailableRow("lock", key: "store.noContent")
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(palette.cardFill)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(palette.swiftInk.opacity(0.10))
                    )

                    Text("store.footer")
                        .font(.footnote)
                        .foregroundStyle(palette.swiftInkSoft)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 20)
                .padding(.top, 6)
                .padding(.bottom, MainTabBarMetrics.contentReserve)
            }
            .background(palette.swiftPaper.ignoresSafeArea())
            .navigationTitle("store.title")
        }
    }

    private func unavailableRow(_ symbol: String, key: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(palette.swiftInkSoft)
                .frame(width: 24)
                .accessibilityHidden(true)
            Text(LocalizedStringKey(key))
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(palette.swiftInk)
            Spacer(minLength: 0)
            Text("store.none")
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: L10n.format("store.rowA11y", L10n.string(key))))
    }
}
