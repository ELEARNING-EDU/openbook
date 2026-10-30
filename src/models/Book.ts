import mongoose, { Schema, type InferSchemaType } from "mongoose";

const bookSchema = new Schema(
  {
    title: { type: String, required: true, trim: true },
    slug: { type: String, required: true, unique: true },
    author: { type: String, required: true, trim: true },
    description: { type: String, default: "" },
    coverUrl: { type: String, default: "" },
    links: {
      type: [{ _id: false, label: { type: String, required: true }, url: { type: String, required: true } }],
      default: [],
    },
    categoryIds: { type: [{ type: Schema.Types.ObjectId, ref: "Category" }], default: [] },
    featured: { type: Boolean, default: false },
    ratingAvg: { type: Number, default: 0 },
    ratingCount: { type: Number, default: 0 },
    downloadCount: { type: Number, default: 0 },
  },
  { timestamps: true },
);

bookSchema.index({ createdAt: -1 });
bookSchema.index({ categoryIds: 1, downloadCount: -1 });

export type BookDoc = InferSchemaType<typeof bookSchema>;
export const Book = (mongoose.models.Book as mongoose.Model<BookDoc>) ?? mongoose.model("Book", bookSchema);
