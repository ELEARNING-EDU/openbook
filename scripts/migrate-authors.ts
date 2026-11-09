import { pathToFileURL } from "node:url";
import mongoose from "mongoose";
import { authorNameKey, findOrCreateAuthor } from "../src/lib/authors";
import { connectDB } from "../src/lib/db";
import { Author } from "../src/models/Author";
import { Book } from "../src/models/Book";

export type MigrationResult = { missing: number; linked: number; createdAuthors: number };

type Options = { apply: boolean; log?: (line: string) => void };

// Dry run by default: the caller must pass apply to write anything.
export async function migrateAuthors({ apply, log = console.log }: Options): Promise<MigrationResult> {
  const uri = process.env.MONGODB_URI;
  if (!uri) throw new Error("Thiếu biến môi trường MONGODB_URI");
  // Not new URL(): replica-set URIs list several hosts. Dropping up to "@" keeps credentials out of the log.
  const [host, path = ""] = uri.slice(uri.lastIndexOf("@") + 1).replace(/^mongodb(\+srv)?:\/\//, "").split("/");
  log(`Database host: ${host}`);
  log(`Database name: ${path.split("?")[0] || "(default)"}`);

  await connectDB();
  const [books, existing] = await Promise.all([
    Book.find({ authorId: null }, { author: 1 }).sort({ _id: 1 }).lean<{ _id: mongoose.Types.ObjectId; author: string }[]>(),
    Author.find({}, { name: 1, aliases: 1 }).lean<{ _id: mongoose.Types.ObjectId; name: string; aliases?: string[] }[]>(),
  ]);

  const idByKey = new Map<string, mongoose.Types.ObjectId>();
  for (const author of existing) {
    const id = author._id as mongoose.Types.ObjectId;
    for (const key of [authorNameKey(author.name), ...(author.aliases ?? []).map(authorNameKey)]) {
      idByKey.set(key, id);
    }
  }
  // Names that a dry run would create, so repeated books reuse one planned author.
  const planned = new Map<string, string>();
  for (const book of books) {
    const key = authorNameKey(book.author);
    if (!idByKey.has(key) && !planned.has(key)) planned.set(key, book.author);
  }

  log(`Books without authorId: ${books.length}`);
  log(`Authors to create: ${planned.size}`);
  log(apply ? "Applying changes." : "Dry run: nothing was written. Pass --apply to write.");

  if (!apply) return { missing: books.length, linked: 0, createdAuthors: 0 };

  const created = new Map<string, mongoose.Types.ObjectId>();
  for (const [key, name] of planned) {
    const author = await findOrCreateAuthor(name);
    created.set(key, new mongoose.Types.ObjectId(author.id));
  }
  let linked = 0;
  for (const book of books) {
    const key = authorNameKey(book.author);
    const authorId = idByKey.get(key) ?? created.get(key);
    if (!authorId) throw new Error(`No author resolved for “${book.author}”`);
    // Only the link changes: Book.author stays exactly as it is today.
    await Book.updateOne({ _id: book._id }, { authorId }, { timestamps: false });
    linked++;
  }
  log(`Linked ${linked} books.`);
  return { missing: books.length, linked, createdAuthors: planned.size };
}

async function main() {
  try {
    process.loadEnvFile(".env.local");
  } catch {
    // no .env.local: rely on MONGODB_URI from the environment
  }
  try {
    await migrateAuthors({ apply: process.argv.includes("--apply") });
  } finally {
    await mongoose.disconnect();
  }
}

const entry = process.argv[1] ? pathToFileURL(process.argv[1]).href : "";
if (entry && import.meta.url === entry) {
  main().catch((err) => {
    console.error(err);
    process.exitCode = 1;
  });
}
