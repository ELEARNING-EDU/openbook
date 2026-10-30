import { z } from "zod";

const httpUrl = z
  .string()
  .trim()
  .pipe(z.url({ protocol: /^https?$/, hostname: z.regexes.domain, error: "Cần link http/https hợp lệ" }));

export const bookSchema = z.object({
  title: z.string().trim().min(1, "Nhập tên sách").max(200),
  author: z.string().trim().min(1, "Nhập tác giả").max(200),
  description: z.string().trim().max(5000).default(""),
  coverUrl: z.union([z.literal(""), httpUrl]).default(""),
  epubUrl: z.union([z.literal(""), httpUrl]).default(""),
  links: z
    .array(z.object({ label: z.string().trim().min(1, "Nhập nhãn").max(50), url: httpUrl }))
    .min(1, "Cần ít nhất 1 link")
    .max(10),
  categoryIds: z.array(z.string().regex(/^[a-f\d]{24}$/i, "Thể loại không hợp lệ")).default([]),
  featured: z.boolean().default(false),
});

export const categorySchema = z.object({
  name: z.string().trim().min(1, "Nhập tên thể loại").max(80),
});

export const reviewSchema = z.object({
  rating: z.number().int().min(1, "Chọn từ 1 đến 5 sao").max(5),
  comment: z.string().trim().max(2000, "Tối đa 2000 ký tự").default(""),
});

export type BookInput = z.input<typeof bookSchema>;
export type CategoryInput = z.input<typeof categorySchema>;
export type ReviewInput = z.input<typeof reviewSchema>;

export function parsePage(raw: string | string[] | undefined): number {
  const n = Number(Array.isArray(raw) ? raw[0] : raw);
  return Number.isInteger(n) && n >= 1 ? n : 1;
}
