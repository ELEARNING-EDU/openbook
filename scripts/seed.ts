import mongoose from "mongoose";
import { connectDB } from "../src/lib/db";
import { slugify } from "../src/lib/slug";
import { Book } from "../src/models/Book";
import { Category } from "../src/models/Category";
import { Review } from "../src/models/Review";

try {
  process.loadEnvFile(".env.local");
} catch {
  // no .env.local: rely on MONGODB_URI from the environment
}

const cover = (isbn: string) => `https://covers.openlibrary.org/b/isbn/${isbn}-L.jpg?default=false`;
const gutenberg = (id: number) => [
  { label: "Đọc online", url: `https://www.gutenberg.org/ebooks/${id}.html.images` },
  { label: "Tải EPUB", url: `https://www.gutenberg.org/ebooks/${id}.epub3.images` },
];
const gutenbergEpub = (id: number) => `https://www.gutenberg.org/ebooks/${id}.epub3.images`;
const wikisourceEpub = (title: string) =>
  `https://ws-export.wmcloud.org/?lang=vi&format=epub&page=${encodeURIComponent(title.replace(/ /g, "_"))}`;
const wikisource = (title: string) => [{ label: "Đọc trên Wikisource", url: `https://vi.wikisource.org/wiki/${encodeURIComponent(title.replace(/ /g, "_"))}` }];

const CATEGORIES = ["Văn học Việt Nam", "Văn học nước ngoài", "Triết học", "Khoa học"] as const;
type Cat = (typeof CATEGORIES)[number];

const BOOKS: { title: string; author: string; description: string; coverUrl: string; epubUrl: string; links: { label: string; url: string }[]; cats: Cat[]; featured?: boolean }[] = [
  { title: "Truyện Kiều", author: "Nguyễn Du", cats: ["Văn học Việt Nam"], featured: true, coverUrl: "", links: wikisource("Truyện Kiều"), epubUrl: wikisourceEpub("Truyện Kiều"),
    description: "Kiệt tác thơ lục bát 3.254 câu kể về cuộc đời mười lăm năm lưu lạc của Thúy Kiều — đỉnh cao của văn học trung đại Việt Nam." },
  { title: "Lục Vân Tiên", author: "Nguyễn Đình Chiểu", cats: ["Văn học Việt Nam"], coverUrl: "", links: wikisource("Lục Vân Tiên"), epubUrl: wikisourceEpub("Lục Vân Tiên"),
    description: "Truyện thơ Nôm đề cao nhân nghĩa, trung hiếu, tiết hạnh qua hình tượng chàng Lục Vân Tiên trọng nghĩa khinh tài." },
  { title: "Số đỏ", author: "Vũ Trọng Phụng", cats: ["Văn học Việt Nam"], featured: true, coverUrl: "", links: wikisource("Số đỏ"), epubUrl: wikisourceEpub("Số đỏ"),
    description: "Tiểu thuyết trào phúng bậc thầy về xã hội thành thị Hà Nội những năm 1930 qua hành trình thăng tiến của Xuân Tóc Đỏ." },
  { title: "Chinh phụ ngâm", author: "Đặng Trần Côn", cats: ["Văn học Việt Nam"], coverUrl: "", links: wikisource("Chinh phụ ngâm"), epubUrl: wikisourceEpub("Chinh phụ ngâm"),
    description: "Khúc ngâm nỗi lòng người vợ có chồng ra trận, bản dịch Nôm nổi tiếng theo thể song thất lục bát." },
  { title: "Pride and Prejudice", author: "Jane Austen", cats: ["Văn học nước ngoài"], featured: true, coverUrl: cover("9780141439518"), links: gutenberg(1342), epubUrl: gutenbergEpub(1342),
    description: "Câu chuyện tình yêu dí dỏm giữa Elizabeth Bennet và ngài Darcy, bức tranh sắc sảo về xã hội Anh đầu thế kỷ 19." },
  { title: "The Adventures of Sherlock Holmes", author: "Arthur Conan Doyle", cats: ["Văn học nước ngoài"], coverUrl: cover("9780140437713"), links: gutenberg(1661), epubUrl: gutenbergEpub(1661),
    description: "Mười hai vụ án kinh điển của thám tử lừng danh Sherlock Holmes và người bạn đồng hành bác sĩ Watson." },
  { title: "Frankenstein", author: "Mary Shelley", cats: ["Văn học nước ngoài", "Khoa học"], coverUrl: cover("9780141439471"), links: gutenberg(84), epubUrl: gutenbergEpub(84),
    description: "Victor Frankenstein tạo ra sự sống — và phải đối mặt với hậu quả. Tiểu thuyết khoa học viễn tưởng đầu tiên của nhân loại." },
  { title: "Alice's Adventures in Wonderland", author: "Lewis Carroll", cats: ["Văn học nước ngoài"], coverUrl: cover("9780141439761"), links: gutenberg(11), epubUrl: gutenbergEpub(11),
    description: "Alice rơi xuống hang thỏ và bước vào thế giới kỳ diệu đầy những nhân vật kỳ quặc và logic phi lý." },
  { title: "The Art of War", author: "Tôn Tử", cats: ["Triết học"], featured: true, coverUrl: cover("9781590302255"), links: gutenberg(132), epubUrl: gutenbergEpub(132),
    description: "Binh pháp Tôn Tử — tác phẩm chiến lược quân sự cổ đại vẫn được ứng dụng trong kinh doanh và cuộc sống ngày nay." },
  { title: "Meditations", author: "Marcus Aurelius", cats: ["Triết học"], coverUrl: cover("9780812968255"), links: gutenberg(2680), epubUrl: gutenbergEpub(2680),
    description: "Suy tưởng của vị hoàng đế La Mã theo trường phái Khắc kỷ: về bổn phận, sự bình thản và cách sống tốt." },
  { title: "Tao Te Ching", author: "Lão Tử", cats: ["Triết học"], coverUrl: "", links: gutenberg(216), epubUrl: gutenbergEpub(216),
    description: "Đạo Đức Kinh — 81 chương ngắn gọn về Đạo, vô vi và sự hài hoà với tự nhiên." },
  { title: "On the Origin of Species", author: "Charles Darwin", cats: ["Khoa học"], coverUrl: cover("9780451529060"), links: gutenberg(1228), epubUrl: gutenbergEpub(1228),
    description: "Nguồn gốc các loài — công trình đặt nền móng cho thuyết tiến hoá bằng chọn lọc tự nhiên." },
];

async function main() {
  await connectDB();
  await Promise.all([Book.deleteMany({}), Category.deleteMany({}), Review.deleteMany({})]);
  await Promise.all([Book.syncIndexes(), Category.syncIndexes(), Review.syncIndexes()]);

  const cats = await Category.insertMany(CATEGORIES.map((name) => ({ name, slug: slugify(name) })));
  const catId = new Map(cats.map((c) => [c.name, c._id]));

  await Book.insertMany(
    BOOKS.map(({ cats: names, ...b }, i) => ({
      ...b,
      slug: slugify(b.title),
      categoryIds: names.map((n) => catId.get(n)),
      downloadCount: Math.floor(Math.random() * 5000),
      createdAt: new Date(Date.now() - i * 86_400_000),
    })),
  );
  console.log(`Seeded ${cats.length} categories, ${BOOKS.length} books.`);
}

main()
  .catch((err) => {
    console.error(err);
    process.exitCode = 1;
  })
  .finally(() => mongoose.disconnect());
