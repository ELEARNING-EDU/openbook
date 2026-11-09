import mongoose, { Schema, type InferSchemaType } from "mongoose";

const authorSchema = new Schema(
  {
    name: { type: String, required: true, trim: true },
    nameKey: { type: String, required: true, unique: true },
    slug: { type: String, required: true, unique: true },
    aliases: { type: [String], default: [] },
    lifespan: { type: String, default: "" },
    bio: { type: String, default: "" },
    notDuplicateOf: { type: [{ type: Schema.Types.ObjectId, ref: "Author" }], default: [] },
  },
  { timestamps: true },
);

export type AuthorDoc = InferSchemaType<typeof authorSchema>;
export const Author =
  (mongoose.models.Author as mongoose.Model<AuthorDoc>) ?? mongoose.model("Author", authorSchema);
