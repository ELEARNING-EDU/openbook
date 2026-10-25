import SwiftUI

/// Full-text search over the open book. Every hit already knows the page it lives
/// on, so picking one is a jump rather than a second lookup.
struct SearchSheet: View {

    let palette: ReaderPalette
    @Binding var query: String
    let hits: [ReaderViewModel.Hit]
    var onPick: (ReaderViewModel.Hit) -> Void
    var onDone: () -> Void

    private var trimmed: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                field
                results
            }
            .background(palette.swiftPaper)
            .navigationTitle("reader.search")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("common.done", action: onDone)
                }
            }
            .preferredColorScheme(palette.isDark ? .dark : .light)
        }
    }

    private var field: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(palette.swiftInkSoft)

            TextField("reader.search", text: $query)
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
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 46)
        .background(
            palette.swiftPaperEdge,
            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
        )
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    @ViewBuilder
    private var results: some View {
        if trimmed.count < 2 {
            note("search.note.min")
        } else if hits.isEmpty {
            note("search.note.none \(trimmed)")
        } else {
            List(hits) { hit in
                Button {
                    onPick(hit)
                } label: {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(hit.chapterTitle)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(palette.swiftAccent)
                        Text(hit.snippet)
                            .font(.system(size: 14))
                            .foregroundStyle(.primary)
                            .lineLimit(3)
                        Text(verbatim: L10n.format("reader.pageRef", hit.pageIndex + 1))
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 3)
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
        }
    }

    private func note(_ key: LocalizedStringKey) -> some View {
        Text(key)
            .font(.system(size: 14))
            .foregroundStyle(palette.swiftInkSoft)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 32)
            .padding(.top, 40)
            .frame(maxWidth: .infinity, alignment: .top)
    }
}
