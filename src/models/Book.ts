import mongoose, { Schema, type InferSchemaType } from "mongoose";

export const BOOK_STATUSES = ["published", "draft"] as const;
export const CONTRIBUTOR_ROLES = ["translator", "editor", "illustrator"] as const;

const bookSchema = new Schema(
  {
    title: { type: String, required: true, trim: true },
    slug: { type: String, required: true, unique: true },
    author: { type: String, required: true, trim: true },
    authorId: { type: Schema.Types.ObjectId, ref: "Author", default: null },
    description: { type: String, default: "" },
    coverUrl: { type: String, default: "" },
    epubUrl: { type: String, default: "" },
    links: {
      type: [{ _id: false, label: { type: String, required: true }, url: { type: String, required: true } }],
      default: [],
    },
    categoryIds: { type: [{ type: Schema.Types.ObjectId, ref: "Category" }], default: [] },
    featured: { type: Boolean, default: false },
    status: { type: String, enum: BOOK_STATUSES, default: "published" },
    language: { type: String, default: "" },
    isbn: { type: String, default: "" },
    publishYear: { type: Number, default: null },
    seriesId: { type: Schema.Types.ObjectId, ref: "Series", default: null },
    seriesNumber: { type: Number, default: null },
    publisherId: { type: Schema.Types.ObjectId, ref: "Publisher", default: null },
    contributors: {
      type: [
        {
          _id: false,
          authorId: { type: Schema.Types.ObjectId, ref: "Author", required: true },
          role: { type: String, enum: CONTRIBUTOR_ROLES, required: true },
        },
      ],
      default: [],
    },
    ratingAvg: { type: Number, default: 0 },
    ratingCount: { type: Number, default: 0 },
    downloadCount: { type: Number, default: 0 },
  },
  { timestamps: true },
);

bookSchema.index({ createdAt: -1 });
bookSchema.index({ categoryIds: 1, downloadCount: -1 });
bookSchema.index({ authorId: 1 });
bookSchema.index({ status: 1 });
bookSchema.index({ seriesId: 1 });
bookSchema.index({ publisherId: 1 });

export type BookDoc = InferSchemaType<typeof bookSchema>;
export type BookStatus = (typeof BOOK_STATUSES)[number];
export type ContributorRole = (typeof CONTRIBUTOR_ROLES)[number];
export const Book = (mongoose.models.Book as mongoose.Model<BookDoc>) ?? mongoose.model("Book", bookSchema);
