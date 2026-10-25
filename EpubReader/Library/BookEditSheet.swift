import SwiftUI

/// Edits only what the library calls the book — title and author. The EPUB, the
/// saved reading place, bookmarks and the file name that identifies the book do
/// not change.
struct BookEditSheet: View {

    let book: BookItem
    let onSave: (String, String) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @State private var title: String
    @State private var author: String

    init(book: BookItem, onSave: @escaping (String, String) -> Void) {
        self.book = book
        self.onSave = onSave
        _title = State(initialValue: book.title)
        _author = State(initialValue: book.author)
    }

    private var palette: ReaderPalette { .shell(dark: colorScheme == .dark) }

    private var canSave: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("library.edit.titleField", text: $title)
                        .textInputAutocapitalization(.words)
                        .accessibilityLabel(Text("library.edit.titleField"))
                    TextField("library.edit.authorField", text: $author)
                        .textInputAutocapitalization(.words)
                        .accessibilityLabel(Text("library.edit.authorField"))
                } footer: {
                    Text("library.edit.footer")
                }
            }
            .scrollContentBackground(.hidden)
            .background(palette.swiftPaper)
            .navigationTitle("library.edit.title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("library.edit.save") {
                        onSave(title, author)
                        dismiss()
                    }
                    .disabled(!canSave)
                }
            }
        }
        .tint(palette.swiftAccent)
    }
}
