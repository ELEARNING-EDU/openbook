// Generates public/test-fixtures/sample.epub (used by e2e and local demos): 3 long chapters with an EPUB3 nav.
import { writeFileSync } from "node:fs";
import { makeEpub } from "../tests/helpers/epub";

const para = (ch: number, i: number) =>
  `<p>Chương ${ch}, đoạn ${i}. Đây là đoạn văn mẫu của OpenBook dùng để kiểm thử trình đọc: phân trang, lật trang cong, lật trang trượt, đổi cỡ chữ và đổi nền đọc. Ngày xửa ngày xưa, ở một thư viện nhỏ ven sông, có những cuốn sách biết kể chuyện cho người đọc vào mỗi buổi tối.</p>`;
const chapter = (n: number) => ({
  name: `ch${n}.xhtml`,
  title: `Chương ${n}`,
  html: `<h1>Chương ${n}</h1>${Array.from({ length: 30 }, (_, i) => para(n, i + 1)).join("")}`,
});

writeFileSync("public/test-fixtures/sample.epub", makeEpub({ chapters: [1, 2, 3].map(chapter), nav: true }));
console.log("Wrote public/test-fixtures/sample.epub");
