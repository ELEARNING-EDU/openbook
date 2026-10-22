import SwiftUI

/// The book's cover shrunk to a disc and spun like a record on a turntable.
///
/// The spin is driven by a timer rather than a repeating animation so that pausing
/// freezes the disc exactly where it was. The gradient runs corner to corner and a
/// soft highlight sits off-centre, which is what makes the rotation readable — a
/// symmetric disc would look static.
struct VinylCover: View {

    let title: String
    let spinning: Bool
    var size: CGFloat = 38

    @State private var angle: Double = 0
    @State private var timer: Timer?

    var body: some View {
        ZStack {
            Circle().fill(
                LinearGradient(
                    colors: BookCover.palette(for: title),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )

            ForEach([0.74, 0.56, 0.40], id: \.self) { ratio in
                Circle()
                    .strokeBorder(.black.opacity(0.20), lineWidth: 1)
                    .frame(width: size * ratio, height: size * ratio)
            }

            // The off-centre glint: the only asymmetric element, so it is what tells
            // the eye the disc is turning.
            Circle()
                .fill(.white.opacity(0.55))
                .frame(width: size * 0.17, height: size * 0.17)
                .blur(radius: 2.5)
                .offset(y: -size * 0.31)

            // Centre label and spindle hole.
            Circle()
                .fill(ReaderTheme.swiftPaper.opacity(0.9))
                .frame(width: size * 0.30, height: size * 0.30)
            Circle()
                .fill(.black.opacity(0.72))
                .frame(width: size * 0.075, height: size * 0.075)
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .rotationEffect(.degrees(angle))
        .onAppear { syncSpin() }
        .onChange(of: spinning) { _, _ in syncSpin() }
        .onDisappear { timer?.invalidate(); timer = nil }
    }

    private func syncSpin() {
        timer?.invalidate()
        timer = nil
        guard spinning else { return }

        // Roughly 30°/s: fast enough to read as spinning, slow enough to look calm.
        timer = Timer.scheduledTimer(withTimeInterval: 1.0 / 30.0, repeats: true) { _ in
            angle += 1.0
            if angle >= 360 { angle -= 360 }
        }
    }
}
