import mongoose, { Schema, type InferSchemaType } from "mongoose";

const publisherSchema = new Schema(
  {
    name: { type: String, required: true, trim: true },
    nameKey: { type: String, required: true, unique: true },
    slug: { type: String, required: true, unique: true },
  },
  { timestamps: true },
);

export type PublisherDoc = InferSchemaType<typeof publisherSchema>;
export const Publisher =
  (mongoose.models.Publisher as mongoose.Model<PublisherDoc>) ?? mongoose.model("Publisher", publisherSchema);
