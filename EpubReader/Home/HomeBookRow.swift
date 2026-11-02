import SwiftUI
import UIKit

/// One secondary shelf on Home: a heading and a continuous run of books.
/// The covers sit on the page with no card around them, and a title too long for
/// the column scrolls rather than being cut off. Tapping a book opens it;
/// managing the book stays in the Library.
struct HomeBookRow: View {

    let titleKey: String
    let books: [BookItem]
    let palette: ReaderPalette
    let subtitle: (BookItem) -> String

    @ObservedObject private var covers = BookCoverStore.shared

    private let tileWidth: CGFloat = 150
    private let coverWidth: CGFloat = 96

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(verbatim: L10n.string(titleKey))
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .tracking(1.0)
                .textCase(.uppercase)
                .foregroundStyle(palette.swiftInkSoft)
                .padding(.horizontal, 20)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 16) {
                    ForEach(books) { book in
                        NavigationLink(value: book) {
                            shelfItem(book)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(
                            Text(verbatim: L10n.format("home.book.a11y", book.title, book.author))
                        )
                    }
                }
                .padding(.vertical, 12)
            }
            .contentMargins(.horizontal, 14, for: .scrollContent)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(palette.cardFill)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(palette.swiftInk.opacity(0.10))
            )
            // The card stays on the page's left margin and runs off the right
            // edge, so the shelf reads as continuing past the screen.
            .padding(.leading, 20)
            .padding(.trailing, -20)
        }
    }

    private func shelfItem(_ book: BookItem) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            BookCover(book: book)
                .frame(width: coverWidth)

            // The title of a generated cover is printed on the cover itself; a
            // book with a real cover relies on that image. Only the generated
            // cover repeats the author underneath it.
            if covers.state(for: book) != .image, !book.author.isEmpty {
                Text(verbatim: book.author)
                    .font(.system(size: 12))
                    .foregroundStyle(palette.swiftInkSoft)
                    .lineLimit(1)
            }

            Text(verbatim: subtitle(book))
                .font(.system(size: 11, weight: .medium, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
                .lineLimit(1)
        }
        .frame(width: tileWidth, alignment: .leading)
        .contentShape(Rectangle())
    }
}
