import Foundation

/// What the app remembers about one imported book besides the EPUB itself.
///
/// The file name is the book's identity; the title and author are only display
/// metadata the reader may edit. The import date is stamped once, when the file
/// enters the app, so sorting by "recently imported" never moves a book because
/// its title was corrected.
struct LibraryMetadata: Codable, Equatable {
    var title: String?
    var author: String?
    var importedAt: Date
    /// Set when the reader marks the book finished; nil means not finished.
    /// Optional so metadata written before this field still decodes.
    var finishedAt: Date?
}

/// How the shelf orders its books.
enum LibrarySort: String, CaseIterable, Identifiable {
    case recent
    case title
    case author

    var id: String { rawValue }

    var labelKey: String {
        switch self {
        case .recent: return "library.sort.recent"
        case .title: return "library.sort.title"
        case .author: return "library.sort.author"
        }
    }
}

/// The shelf's order as a pure function, so the rules can be tested without a
/// shelf: locale-aware text comparison, a deterministic tie-break, and an import
/// date that is independent of the current title.
enum LibraryOrder {

    static func sorted(
        books: [BookItem],
        metadata: [String: LibraryMetadata],
        by sort: LibrarySort,
        locale: Locale = .autoupdatingCurrent
    ) -> [BookItem] {
        books.sorted { left, right in
            switch sort {
            case .recent:
                let leftDate = metadata[left.id]?.importedAt ?? .distantPast
                let rightDate = metadata[right.id]?.importedAt ?? .distantPast
                if leftDate != rightDate { return leftDate > rightDate }
            case .title:
                let order = compare(left.title, right.title, locale: locale)
                if order != .orderedSame { return order == .orderedAscending }
            case .author:
                let order = compare(left.author, right.author, locale: locale)
                if order != .orderedSame { return order == .orderedAscending }
                let titleOrder = compare(left.title, right.title, locale: locale)
                if titleOrder != .orderedSame { return titleOrder == .orderedAscending }
            }
            return left.id < right.id
        }
    }

    /// `localizedStandardCompare` is locale-aware but always uses the process
    /// locale. Sorting follows the app's own language, so the comparison is done
    /// with the locale passed in instead.
    static func compare(_ left: String, _ right: String, locale: Locale) -> ComparisonResult {
        let result = left.compare(
            right,
            options: [.caseInsensitive, .numeric, .widthInsensitive],
            range: nil,
            locale: locale
        )
        if result != .orderedSame { return result }
        return left.compare(right, options: [.literal], range: nil, locale: locale)
    }
}
