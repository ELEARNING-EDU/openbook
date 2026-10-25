import SwiftUI

/// The Home tab's reading achievement: today against the daily goal, the run of
/// days that reached it, and the week/month/year totals. One button — the whole
/// card is the way into the detail sheet.
struct ReadingStatsCard: View {

    let summary: ReadingSummary
    let palette: ReaderPalette
    let action: () -> Void

    private var goalSeconds: Double { Double(summary.goalMinutes) * 60 }

    private var progress: Double {
        guard goalSeconds > 0 else { return 0 }
        return min(1, max(0, summary.todaySeconds / goalSeconds))
    }

    private var remainingMinutes: Int {
        guard !summary.todayReachedGoal else { return 0 }
        return Int(ceil(max(0, goalSeconds - summary.todaySeconds) / 60))
    }

    private var todayLine: String {
        L10n.format(
            "home.stats.today",
            ReadingStatsText.duration(minutes: summary.todayMinutes)
        )
    }

    private var goalLine: String {
        if summary.todayReachedGoal {
            return L10n.format("home.stats.goalMet", summary.goalMinutes)
        }
        return L10n.format(
            "home.stats.remaining",
            ReadingStatsText.duration(minutes: remainingMinutes)
        )
    }

    private var streakLine: String {
        summary.streak.current > 0
            ? L10n.format("home.stats.streak", summary.streak.current)
            : L10n.string("home.stats.noStreak")
    }

    private var totalsLine: String {
        [
            L10n.format("home.stats.week", ReadingStatsText.duration(minutes: summary.weekMinutes)),
            L10n.format("home.stats.month", ReadingStatsText.duration(minutes: summary.monthMinutes)),
            L10n.format("home.stats.year", ReadingStatsText.duration(minutes: summary.yearMinutes))
        ].joined(separator: " · ")
    }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .center, spacing: 12) {
                    ring
                    VStack(alignment: .leading, spacing: 3) {
                        Text(verbatim: todayLine)
                            .font(.system(size: 17, weight: .semibold, design: .serif))
                            .foregroundStyle(palette.swiftInk)
                            .lineLimit(1)
                        Text(verbatim: goalLine)
                            .font(.system(size: 12))
                            .foregroundStyle(palette.swiftInkSoft)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 8)
                    streak
                }

                Text(verbatim: totalsLine)
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundStyle(palette.swiftInkSoft)
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(palette.cardFill)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(palette.swiftInk.opacity(0.08))
            )
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: accessibilitySentence))
        .accessibilityAddTraits(.isButton)
    }

    private var ring: some View {
        ZStack {
            Circle()
                .strokeBorder(palette.swiftInk.opacity(0.14), lineWidth: 4)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(palette.swiftAccent, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                .rotationEffect(.degrees(-90))
            if summary.todayReachedGoal {
                Image(systemName: "checkmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(palette.swiftAccent)
            } else {
                Text(verbatim: "\(Int((progress * 100).rounded()))%")
                    .font(.system(size: 10, weight: .semibold, design: .rounded))
                    .foregroundStyle(palette.swiftInkSoft)
                    .monospacedDigit()
            }
        }
        .frame(width: 44, height: 44)
        .accessibilityHidden(true)
    }

    private var streak: some View {
        HStack(spacing: 4) {
            Image(systemName: "flame.fill")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(
                    summary.streak.current > 0
                        ? palette.swiftAccent
                        : palette.swiftInkSoft.opacity(0.5)
                )
            Text(verbatim: streakLine)
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.swiftInkSoft)
                .lineLimit(1)
        }
        .fixedSize()
    }

    private var accessibilitySentence: String {
        [todayLine, goalLine, streakLine, totalsLine].joined(separator: ". ")
    }
}
