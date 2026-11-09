import { describe, expect, it } from "vitest";
import { link, setupTestDB } from "../helpers/mongo";
import { Book } from "@/models/Book";
import { Author } from "@/models/Author";
import { migrateAuthors } from "../../scripts/migrate-authors";

setupTestDB();

const mk = (author: string, title = "Sách") =>
  Book.create({ title, slug: `sach-${Math.random().toString(36).slice(2)}`, author, links: link() });

describe("migrateAuthors", () => {
  it("dry run không ghi gì; apply liên kết sách, giữ author và chạy lại không đổi gì", async () => {
    const a = await mk("Nguyễn Du");
    const b = await mk("Nguyễn Du", "Truyện Kiều");
    const before = (await Book.findById(a._id).lean())?.updatedAt;

    const dry = await migrateAuthors({ apply: false, log: () => {} });
    expect(dry).toMatchObject({ missing: 2, linked: 0, createdAuthors: 0 });
    expect(await Author.countDocuments()).toBe(0);
    expect(await Book.countDocuments({ authorId: null })).toBe(2);

    await new Promise((resolve) => setTimeout(resolve, 5));
    const applied = await migrateAuthors({ apply: true, log: () => {} });
    expect(applied).toMatchObject({ missing: 2, linked: 2, createdAuthors: 1 });
    const authors = await Author.find().lean();
    expect(authors).toHaveLength(1);
    expect(authors[0].name).toBe("Nguyễn Du");
    for (const id of [a._id, b._id]) {
      const book = await Book.findById(id).lean();
      expect(String(book?.authorId)).toBe(String(authors[0]._id));
      expect(book?.author).toBe("Nguyễn Du");
    }
    expect((await Book.findById(a._id).lean())?.updatedAt).toEqual(before);

    const again = await migrateAuthors({ apply: true, log: () => {} });
    expect(again).toMatchObject({ missing: 0, linked: 0, createdAuthors: 0 });
    expect(await Author.countDocuments()).toBe(1);
  });
});
