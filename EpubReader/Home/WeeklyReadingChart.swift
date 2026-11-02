import SwiftUI

/// Home's week strip: seven columns, Monday to Sunday, with the daily goal as a
/// thin line across them. It only appears when the week has any reading in it.
struct WeeklyReadingChart: View {

    let points: [ReadingWeekPoint]
    let goalMinutes: Int
    let palette: ReaderPalette

    private var ceiling: Double {
        max(Double(goalMinutes), points.map(\.minutes).max() ?? 0, 1)
    }

    private var totalMinutes: Int {
        Int(points.map(\.minutes).reduce(0, +).rounded())
    }

    private let barHeight: CGFloat = 88

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(verbatim: L10n.string("home.weekly.title"))
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .tracking(1.0)
                    .textCase(.uppercase)
                    .foregroundStyle(palette.swiftInkSoft)
                Spacer()
                Text(verbatim: ReadingStatsText.duration(minutes: totalMinutes))
                    .font(.system(size: 11, weight: .semibold, design: .rounded))
                    .foregroundStyle(palette.swiftInkSoft)
            }

            chart
            labels
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text(verbatim: L10n.string("home.weekly.title")))
    }

    private var chart: some View {
        ZStack(alignment: .bottom) {
            Rectangle()
                .fill(palette.swiftInk.opacity(0.08))
                .frame(height: 1)

            Rectangle()
                .fill(palette.swiftAccent.opacity(0.35))
                .frame(height: 1)
                .padding(.bottom, height(for: Double(goalMinutes)))
                .accessibilityHidden(true)

            HStack(alignment: .bottom, spacing: 8) {
                ForEach(points, id: \.date) { point in
                    VStack(spacing: 0) {
                        Spacer(minLength: 0)
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(point.isToday ? palette.swiftAccent : palette.swiftInkSoft.opacity(0.45))
                            .frame(width: 18, height: max(2, height(for: point.minutes)))
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(
                        Text(verbatim: L10n.format(
                            "home.weekly.a11y",
                            weekday(point.date),
                            ReadingStatsText.duration(minutes: Int(point.minutes.rounded()))
                        ))
                    )
                }
            }
        }
        .frame(height: barHeight, alignment: .bottom)
    }

    private var labels: some View {
        HStack(spacing: 8) {
            ForEach(points, id: \.date) { point in
                Text(verbatim: weekday(point.date))
                    .font(.system(size: 11, weight: point.isToday ? .semibold : .regular, design: .rounded))
                    .foregroundStyle(point.isToday ? palette.swiftAccent : palette.swiftInkSoft)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity)
                    .accessibilityHidden(true)
            }
        }
    }

    private func height(for minutes: Double) -> CGFloat {
        guard minutes > 0 else { return 0 }
        return max(2, CGFloat(minutes / ceiling) * barHeight)
    }

    private func weekday(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = L10n.locale
        formatter.setLocalizedDateFormatFromTemplate("EEE")
        return formatter.string(from: date)
    }
}
