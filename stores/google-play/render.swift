// OpenBook — Google Play compositions (1080×1920) + feature graphic (1024×500).
// Inputs: captures/{en,vi}-{home,library,reader,listen,curl}-raw.png (real device captures).
// Run: swift render.swift <dir>
import AppKit

let root = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : FileManager.default.currentDirectoryPath)

func hex(_ v: UInt32) -> NSColor {
    NSColor(srgbRed: CGFloat((v >> 16) & 255) / 255, green: CGFloat((v >> 8) & 255) / 255, blue: CGFloat(v & 255) / 255, alpha: 1)
}
let paper = hex(0xFBF7EE)
let ink = hex(0x2A241D)
let rust = hex(0x9E5932)
let muted = hex(0x6E6459)
let hairline = hex(0xE3D9C8)

func box(_ r: NSRect, _ c: NSColor, _ radius: CGFloat = 0) {
    c.setFill(); NSBezierPath(roundedRect: r, xRadius: radius, yRadius: radius).fill()
}

func text(_ s: String, _ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ size: CGFloat,
          serif: Bool = false, color: NSColor = ink, tracking: CGFloat = 0, leading: CGFloat = 8) {
    let p = NSMutableParagraphStyle(); p.lineSpacing = leading
    let font = NSFont(name: serif ? "Georgia-Bold" : "ArialMT", size: size)!
    var attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color, .paragraphStyle: p]
    if tracking != 0 { attrs[.kern] = tracking }
    (s as NSString).draw(in: NSRect(x: x, y: y, width: w, height: 400), withAttributes: attrs)
}

func capture(_ name: String) -> NSImage {
    NSImage(contentsOf: root.appendingPathComponent("captures/\(name)-raw.png"))!
}

func canvas(_ w: Int, _ h: Int, _ file: String, _ body: () -> Void) {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: w, pixelsHigh: h, bitsPerSample: 8,
                               samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
                               colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    let cg = NSGraphicsContext.current!.cgContext
    cg.translateBy(x: 0, y: CGFloat(h)); cg.scaleBy(x: 1, y: -1)
    NSGraphicsContext.current = NSGraphicsContext(cgContext: cg, flipped: true)
    body()
    NSGraphicsContext.restoreGraphicsState()
    try! rep.representation(using: .png, properties: [:])!.write(to: root.appendingPathComponent(file))
}

struct Slide {
    let key: String
    let vi: (String, String)
    let en: (String, String)
}

let slides: [Slide] = [
    Slide(key: "home",
          vi: ("Mở ra là đọc tiếp", "Giữ sách đang đọc, phút hôm nay, chuỗi ngày."),
          en: ("Pick up where you stopped", "Current books, today's minutes and your streak.")),
    Slide(key: "library",
          vi: ("Nhập EPUB, xem cả tủ", "Bìa thật, tiến độ từng cuốn, bố cục lưới."),
          en: ("Your shelf in one grid", "Import EPUB files; real covers and progress.")),
    Slide(key: "reader",
          vi: ("Một trang đọc yên", "Chữ serif trên nền giấy, không gì chen vào."),
          en: ("A quiet page, tuned", "Serif type on paper, nothing in the way.")),
    Slide(key: "listen",
          vi: ("Để sách đọc thành tiếng", "Đọc to từ trang đang mở, đổi tốc độ tại chỗ."),
          en: ("Let the book speak", "Read aloud from this page; change speed inline.")),
    Slide(key: "curl",
          vi: ("Lật trang như sách giấy", "Trang cong theo tay, mặt sau in xuyên nhẹ."),
          en: ("Pages that turn like paper", "The sheet follows your finger, print showing.")),
]

let footer = ["vi": "ĐỌC  ·  NGHE  ·  LẬT TRANG", "en": "READ  ·  LISTEN  ·  TURN PAGES"]

func compose(_ slide: Slide, _ lang: String, _ index: Int, _ file: String) {
    let title = lang == "vi" ? slide.vi.0 : slide.en.0
    let sub = lang == "vi" ? slide.vi.1 : slide.en.1
    canvas(1080, 1920, file) {
        box(NSRect(x: 0, y: 0, width: 1080, height: 1920), paper)
        box(NSRect(x: 72, y: 62, width: 34, height: 5), rust, 2)
        text(String(format: "%02d / %02d", index, slides.count), 828, 44, 180, 22, color: muted)

        text(title, 72, 106, 936, 60, serif: true, leading: 8)
        text(sub, 74, 292, 900, 27, color: muted, leading: 6)

        let shot = capture("\(lang)-\(slide.key)")
        let w: CGFloat = 620
        let h = w * shot.size.height / shot.size.width
        let x = (1080 - w) / 2
        let y: CGFloat = 404
        box(NSRect(x: x - 6, y: y - 6, width: w + 12, height: h + 12), ink, 24)
        NSGraphicsContext.saveGraphicsState()
        NSBezierPath(roundedRect: NSRect(x: x, y: y, width: w, height: h), xRadius: 20, yRadius: 20).addClip()
        shot.draw(in: NSRect(x: x, y: y, width: w, height: h), from: .zero, operation: .sourceOver,
                  fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
        NSGraphicsContext.restoreGraphicsState()

        box(NSRect(x: 72, y: 1846, width: 1080 - 144, height: 1), hairline)
        text(footer[lang]!, 72, 1866, 800, 21, color: rust, tracking: 2)
    }
}

for lang in ["vi", "en"] {
    for (i, slide) in slides.enumerated() {
        compose(slide, lang, i + 1, "exports/screenshots-\(lang)-\(i + 1)-\(slide.key).png")
    }
}

// Feature graphic: name, one honest line, the icon.
let taglines = [
    "vi": ("Một trang sách.\nMột khoảng riêng.", "Đọc EPUB  ·  Nghe đọc  ·  Lật trang như giấy"),
    "en": ("One page at a time.\nA quiet place to read.", "EPUB reading  ·  Read aloud  ·  Paper page turns"),
]
for lang in ["vi", "en"] {
    let t = taglines[lang]!
    canvas(1024, 500, "exports/feature-graphic-\(lang).png") {
        box(NSRect(x: 0, y: 0, width: 1024, height: 500), paper)
        let icon = NSImage(contentsOf: root.appendingPathComponent("exports/icon-512.png"))!
        icon.draw(in: NSRect(x: 744, y: 130, width: 220, height: 220), from: .zero, operation: .sourceOver,
                  fraction: 1, respectFlipped: true, hints: [.interpolation: NSImageInterpolation.high])
        text("OpenBook", 64, 108, 640, 72, serif: true)
        text(t.0, 64, 214, 640, 31, color: muted, leading: 8)
        box(NSRect(x: 64, y: 392, width: 34, height: 4), rust, 2)
        text(t.1, 112, 380, 660, 19, color: rust, tracking: 1)
    }
}
print("Rendered \(slides.count * 2) screenshots + 2 feature graphics.")
