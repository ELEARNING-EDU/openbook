import SwiftUI

/// The detail behind the Home card: the four totals, the runs, the records and
/// the books finished this year. Charts are a later phase; the stored days
/// already carry what they need.
struct ReadingStatsSheet: View {

    let summary: ReadingSummary
    let finished: [LibraryStore.FinishedBook]
    let palette: ReaderPalette
    let onGoalTap: () -> Void

    @Environment(\.dismiss) private var dismiss

    private let columns = [
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10)
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                header
                tiles
                streaks
                records
                finishedBooks
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 32)
        }
        .background(palette.swiftPaper.ignoresSafeArea())
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text("stats.title")
                    .font(.system(size: 22, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.swiftInk)
                Spacer()
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(palette.swiftInkSoft)
                        .frame(width: 30, height: 30)
                        .background(Circle().fill(palette.cardFill))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("stats.close"))
            }

            HStack(spacing: 10) {
                Text(verbatim: L10n.format("stats.goal", summary.goalMinutes))
                    .font(.system(size: 12))
                    .foregroundStyle(palette.swiftInkSoft)
                Button {
                    onGoalTap()
                } label: {
                    Text("stats.changeGoal")
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .foregroundStyle(palette.swiftAccent)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var tiles: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            tile("stats.today", summary.todayMinutes)
            tile("stats.week", summary.weekMinutes)
            tile("stats.month", summary.monthMinutes)
            tile("stats.year", summary.yearMinutes)
        }
    }

    private func tile(_ title: String, _ minutes: Int) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(LocalizedStringKey(title))
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
                .textCase(.uppercase)
                .tracking(0.5)
            Text(verbatim: ReadingStatsText.duration(minutes: minutes))
                .font(.system(size: 19, weight: .semibold, design: .serif))
                .foregroundStyle(palette.swiftInk)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(palette.cardFill)
        )
        .accessibilityElement(children: .combine)
    }

    private var streaks: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("stats.consistency")
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
                .textCase(.uppercase)
                .tracking(0.5)

            HStack(spacing: 10) {
                value("stats.streak.current", days: summary.streak.current)
                value("stats.streak.longest", days: summary.streak.longest)
                value("stats.daysRead", days: summary.streak.daysRead)
            }
        }
    }

    private func value(_ title: String, days: Int) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(LocalizedStringKey(title))
                .font(.system(size: 10))
                .foregroundStyle(palette.swiftInkSoft)
                .lineLimit(1)
            Text(verbatim: ReadingStatsText.days(days))
                .font(.system(size: 15, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInk)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private var records: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("stats.records")
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
                .textCase(.uppercase)
                .tracking(0.5)

            recordRow("stats.longestSession",
                      ReadingStatsText.duration(minutes: summary.longestSessionMinutes))
            if let bestDay = summary.bestDay {
                recordRow(
                    "stats.bestDay",
                    L10n.format(
                        "stats.bestDayValue",
                        dayText(bestDay),
                        ReadingStatsText.duration(minutes: summary.bestDayMinutes)
                    )
                )
            }
        }
    }

    private func recordRow(_ title: String, _ value: String) -> some View {
        HStack {
            Text(LocalizedStringKey(title))
                .font(.system(size: 13))
                .foregroundStyle(palette.swiftInkSoft)
            Spacer(minLength: 12)
            Text(verbatim: value)
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInk)
        }
        .accessibilityElement(children: .combine)
    }

    private var finishedBooks: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("stats.finished")
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
                .textCase(.uppercase)
                .tracking(0.5)

            if finished.isEmpty {
                Text("stats.finished.empty")
                    .font(.footnote)
                    .foregroundStyle(palette.swiftInkSoft)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(alignment: .top, spacing: 12) {
                        ForEach(finished) { item in
                            VStack(alignment: .leading, spacing: 5) {
                                BookCover(book: item.book)
                                    .frame(width: 58)
                                Text(verbatim: item.book.title)
                                    .font(.system(size: 11, weight: .semibold, design: .serif))
                                    .foregroundStyle(palette.swiftInk)
                                    .lineLimit(2)
                                Text(verbatim: monthText(item.finishedAt))
                                    .font(.system(size: 10))
                                    .foregroundStyle(palette.swiftInkSoft)
                            }
                            .frame(width: 92, alignment: .leading)
                            .accessibilityElement(children: .combine)
                        }
                    }
                    .padding(.vertical, 2)
                }
            }
        }
    }

    private func dayText(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = L10n.locale
        formatter.setLocalizedDateFormatFromTemplate("d MMM")
        return formatter.string(from: date)
    }

    private func monthText(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = L10n.locale
        formatter.setLocalizedDateFormatFromTemplate("LLLL")
        return formatter.string(from: date)
    }
}
