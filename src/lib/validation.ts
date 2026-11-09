import { z } from "zod";
import type { Translate } from "@/lib/i18n";
import { BOOK_STATUSES, CONTRIBUTOR_ROLES } from "@/models/Book";

// Also the maximum number of books a single bulk action may touch.
export const PAGE_SIZE = 20;

export const httpUrl = z
  .string()
  .trim()
  .pipe(z.url({ protocol: /^https?$/, hostname: z.regexes.domain, error: "Cần link http/https hợp lệ" }));

const makeHttpUrl = (t: Translate) =>
  z.string().trim().pipe(z.url({ protocol: /^https?$/, hostname: z.regexes.domain, error: t("validation.url") }));

const objectId = (t: Translate) => z.string().regex(/^[a-f\d]{24}$/i, t("validation.book"));

const validIsbn = (value: string) => {
  if (value === "") return true;
  if (!/^[\d-]+$/.test(value)) return false;
  const digits = value.replace(/-/g, "");
  return digits.length === 10 || digits.length === 13;
};

export const makeBookSchema = (t: Translate) => {
  const url = makeHttpUrl(t);
  const nextYear = new Date().getFullYear() + 1;
  return z
    .object({
      title: z.string().trim().min(1, t("validation.title")).max(200),
      author: z.string().trim().min(1, t("validation.author")).max(200),
      description: z.string().trim().max(5000).default(""),
      coverUrl: z.union([z.literal(""), url]).default(""),
      epubUrl: z.union([z.literal(""), url]).default(""),
      links: z
        .array(z.object({ label: z.string().trim().min(1, t("validation.label")).max(50), url }))
        .min(1, t("validation.linksMin"))
        .max(10),
      categoryIds: z.array(z.string().regex(/^[a-f\d]{24}$/i, t("validation.category"))).default([]),
      featured: z.boolean().default(false),
      status: z.enum(BOOK_STATUSES, { error: t("validation.status") }).default("published"),
      language: z.enum(["", "vi", "en"], { error: t("validation.language") }).default(""),
      isbn: z
        .string()
        .trim()
        .max(20)
        .default("")
        .refine(validIsbn, t("validation.isbn")),
      publishYear: z
        .union([z.number().int().min(1, t("validation.publishYear")).max(nextYear, t("validation.publishYear")), z.null()])
        .default(null),
      series: z.string().trim().max(200).default(""),
      seriesNumber: z
        .union([z.number().int().min(1, t("validation.seriesNumber")), z.null()])
        .default(null),
      publisher: z.string().trim().max(200).default(""),
      contributors: z
        .array(
          z.object({
            name: z.string().trim().min(1, t("validation.contributorName")).max(200),
            role: z.enum(CONTRIBUTOR_ROLES, { error: t("validation.contributorRole") }),
          }),
        )
        .max(20)
        .default([]),
    })
    .superRefine((data, ctx) => {
      if (data.seriesNumber !== null && !data.series.trim()) {
        ctx.addIssue({ code: "custom", path: ["seriesNumber"], message: t("validation.seriesNumberSeries") });
      }
    });
};

export const makeBookIdsSchema = (t: Translate) =>
  z
    .array(objectId(t))
    .min(1, t("action.bulkEmpty"))
    .max(PAGE_SIZE, t("validation.bulkMax", { n: PAGE_SIZE }))
    .refine((ids) => new Set(ids).size === ids.length, { message: t("validation.bulkDuplicate") });

export const makeBulkStatusSchema = (t: Translate) =>
  z.object({ ids: makeBookIdsSchema(t), status: z.enum(BOOK_STATUSES, { error: t("validation.status") }) });

export const makeBulkCategorySchema = (t: Translate) =>
  z.object({ ids: makeBookIdsSchema(t), categoryId: objectId(t) });

export const makeTermSchema = (t: Translate) =>
  z.object({
    name: z.string().trim().min(1, t("validation.termName")).max(120),
  });

export const makeAuthorSchema = (t: Translate) =>
  z.object({
    name: z.string().trim().min(1, t("validation.authorName")).max(200),
    aliases: z.array(z.string().trim().min(1, t("validation.authorAlias")).max(200)).max(50).default([]),
    lifespan: z.string().trim().max(100).default(""),
    bio: z.string().trim().max(5000).default(""),
  });

export const makeCategorySchema = (t: Translate) =>
  z.object({
    name: z.string().trim().min(1, t("validation.categoryName")).max(80),
  });

export const makeCategoryMenuSchema = (t: Translate) =>
  z
    .array(
      z.object({
        id: z.string().regex(/^[a-f\d]{24}$/i, t("validation.category")),
        showInMenu: z.boolean(),
      }),
    )
    .max(500)
    .refine((items) => new Set(items.map((i) => i.id)).size === items.length, { message: t("validation.categoryMenuDuplicate") });

export const makeReviewSchema = (t: Translate) =>
  z.object({
    rating: z.number().int().min(1, t("validation.rating")).max(5, t("validation.rating")),
    comment: z.string().trim().max(2000, t("validation.comment")).default(""),
  });

export type BookInput = z.input<ReturnType<typeof makeBookSchema>>;
export type AuthorInput = z.input<ReturnType<typeof makeAuthorSchema>>;
export type CategoryInput = z.input<ReturnType<typeof makeCategorySchema>>;
export type CategoryMenuInput = z.input<ReturnType<typeof makeCategoryMenuSchema>>;
export type ReviewInput = z.input<ReturnType<typeof makeReviewSchema>>;
export type TermInput = z.input<ReturnType<typeof makeTermSchema>>;
export type BookIdsInput = z.input<ReturnType<typeof makeBookIdsSchema>>;

const MAX_PAGE = 10_000;

export function parsePage(raw: string | string[] | undefined): number {
  const n = Number(Array.isArray(raw) ? raw[0] : raw);
  return Number.isInteger(n) && n >= 1 ? Math.min(n, MAX_PAGE) : 1;
}
