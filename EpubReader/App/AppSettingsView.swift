import SwiftUI

/// The app's own settings — the interface language, not the book's appearance.
/// Reading themes, type and day/night stay inside the reader.
struct AppSettingsView: View {

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage(AppLanguage.storageKey) private var languageID = AppLanguage.system.rawValue

    private var palette: ReaderPalette { .shell(dark: colorScheme == .dark) }

    private var language: AppLanguage { AppLanguage(rawValue: languageID) ?? .system }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    ForEach(AppLanguage.allCases) { option in
                        Button {
                            languageID = option.rawValue
                            option.applyOverride()
                        } label: {
                            HStack {
                                Text(verbatim: L10n.string(option.titleKey))
                                    .foregroundStyle(palette.swiftInk)
                                Spacer()
                                if language == option {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundStyle(palette.swiftAccent)
                                }
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(language == option ? .isSelected : [])
                    }
                } header: {
                    Text("settings.language")
                } footer: {
                    Text("settings.language.footer")
                }
            }
            .scrollContentBackground(.hidden)
            .background(palette.swiftPaper)
            .navigationTitle("settings.title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("common.done") { dismiss() }
                }
            }
        }
        .tint(palette.swiftAccent)
    }
}
