import mongoose, { Schema, type InferSchemaType } from "mongoose";

const bookDailyStatSchema = new Schema({
  bookId: { type: Schema.Types.ObjectId, ref: "Book", required: true },
  date: { type: String, required: true },
  reads: { type: Number, default: 0 },
  downloads: { type: Number, default: 0 },
});

bookDailyStatSchema.index({ bookId: 1, date: 1 }, { unique: true });
bookDailyStatSchema.index({ date: 1 });

export type BookDailyStatDoc = InferSchemaType<typeof bookDailyStatSchema>;
export const BookDailyStat =
  (mongoose.models.BookDailyStat as mongoose.Model<BookDailyStatDoc>) ??
  mongoose.model("BookDailyStat", bookDailyStatSchema);
