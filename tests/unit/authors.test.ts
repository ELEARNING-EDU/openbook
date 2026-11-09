import { describe, expect, it } from "vitest";
import { duplicateNameKey, suggestDuplicateAuthors, type AuthorRef } from "@/lib/authors";

const author = (id: string, name: string, aliases: string[] = [], notDuplicateOf: string[] = []): AuthorRef => ({
  id,
  name,
  aliases,
  notDuplicateOf,
});

describe("suggestDuplicateAuthors với tên khác của chính mình", () => {
  it("không gợi ý gộp một tác giả với chính họ", () => {
    const self = { id: "a", name: "Nguyễn Du", aliases: ["Nguyen Du"], notDuplicateOf: [] };
    expect(suggestDuplicateAuthors([self])).toEqual([]);
  });
});

describe("duplicateNameKey", () => {
  it("bỏ dấu, hoa thường, dấu câu và thứ tự từ", () => {
    const expected = duplicateNameKey("Nguyễn Du");
    expect(duplicateNameKey("Du Nguyen")).toBe(expected);
    expect(duplicateNameKey("NGUYEN DU")).toBe(expected);
    expect(duplicateNameKey("nguyễn-du")).toBe(expected);
    expect(duplicateNameKey("  Nguyễn   Du  ")).toBe(expected);
  });

  it("đ chuyển thành d", () => {
    expect(duplicateNameKey("Đặng Trần Côn")).toBe(duplicateNameKey("Dang Tran Con"));
  });
});

describe("suggestDuplicateAuthors", () => {
  it("gợi ý cặp tên hoặc tên khác trùng sau khi chuẩn hoá", () => {
    const lao = author("1", "Lão Tử");
    const du = author("2", "Nguyễn Du");
    const duAlias = author("3", "Tố Như", ["Du Nguyen"]);
    const pairs = suggestDuplicateAuthors([lao, du, duAlias]);
    expect(pairs).toHaveLength(1);
    expect([pairs[0].a.id, pairs[0].b.id].sort()).toEqual(["2", "3"]);
  });

  it("không gợi ý cặp đã đánh dấu là hai người khác nhau", () => {
    const a = author("1", "Nguyễn Du", [], ["2"]);
    const b = author("2", "Du Nguyen");
    expect(suggestDuplicateAuthors([a, b])).toEqual([]);
    expect(suggestDuplicateAuthors([{ ...a, notDuplicateOf: [] }, { ...b, notDuplicateOf: ["1"] }])).toEqual([]);
  });

  it("không gộp Lão Tử với Laozi", () => {
    expect(suggestDuplicateAuthors([author("1", "Lão Tử"), author("2", "Laozi")])).toEqual([]);
  });

  it("một cặp chỉ xuất hiện một lần dù trùng nhiều tên khác", () => {
    const a = author("1", "Nguyễn Du", ["Tố Như"]);
    const b = author("2", "Du Nguyen", ["To Nhu"]);
    expect(suggestDuplicateAuthors([a, b])).toHaveLength(1);
  });
});
