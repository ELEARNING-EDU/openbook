import mongoose, { Schema, type InferSchemaType } from "mongoose";

const reviewSchema = new Schema(
  {
    bookId: { type: Schema.Types.ObjectId, ref: "Book", required: true },
    userId: { type: String, required: true },
    userName: { type: String, required: true },
    userImage: { type: String },
    rating: { type: Number, required: true, min: 1, max: 5 },
    comment: { type: String, default: "" },
    hidden: { type: Boolean, default: false },
  },
  { timestamps: true },
);

reviewSchema.index({ bookId: 1, userId: 1 }, { unique: true });
reviewSchema.index({ createdAt: -1 });

export type ReviewDoc = InferSchemaType<typeof reviewSchema>;
export const Review =
  (mongoose.models.Review as mongoose.Model<ReviewDoc>) ?? mongoose.model("Review", reviewSchema);
