import mongoose, { Schema, type InferSchemaType } from "mongoose";

const seriesSchema = new Schema(
  {
    name: { type: String, required: true, trim: true },
    nameKey: { type: String, required: true, unique: true },
    slug: { type: String, required: true, unique: true },
  },
  { timestamps: true },
);

export type SeriesDoc = InferSchemaType<typeof seriesSchema>;
export const Series = (mongoose.models.Series as mongoose.Model<SeriesDoc>) ?? mongoose.model("Series", seriesSchema);
