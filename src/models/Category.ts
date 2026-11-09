import mongoose, { Schema, type InferSchemaType } from "mongoose";

const categorySchema = new Schema(
  {
    name: { type: String, required: true, trim: true },
    slug: { type: String, required: true, unique: true },
    showInMenu: { type: Boolean, default: true },
    menuOrder: { type: Number, default: 0 },
  },
  { timestamps: true },
);

export type CategoryDoc = InferSchemaType<typeof categorySchema>;
export const Category =
  (mongoose.models.Category as mongoose.Model<CategoryDoc>) ?? mongoose.model("Category", categorySchema);
