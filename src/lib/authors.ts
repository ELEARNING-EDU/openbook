import { connectDB } from "@/lib/db";
import { slugify } from "@/lib/slug";
import { uniqueSlug } from "@/lib/book-service";
import { Author } from "@/models/Author";

export type AuthorRef = { id: string; name: string; aliases: string[]; notDuplicateOf: string[] };

// Canonical key for names and aliases: lower case, accents and punctuation removed, words sorted.
export function duplicateNameKey(value: string): string {
  return value
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[đĐ]/g, "d")
    .toLowerCase()
    .replace(/[^\p{Letter}\p{Number}]+/gu, " ")
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .sort()
    .join(" ");
}

export const authorNameKey = (name: string) => name.normalize("NFC").trim().toLowerCase().replace(/\s+/g, " ");

export function suggestDuplicateAuthors(authors: AuthorRef[]): { a: AuthorRef; b: AuthorRef }[] {
  const keys = new Map<string, string[]>();
  for (const author of authors) {
    for (const key of [duplicateNameKey(author.name), ...author.aliases.map(duplicateNameKey)]) {
      if (!key) continue;
      // One author's name and alias can share a key (after a merge); that is not a duplicate.
      const ids = keys.get(key) ?? [];
      if (!ids.includes(author.id)) keys.set(key, [...ids, author.id]);
    }
  }
  const pairs = new Map<string, { a: AuthorRef; b: AuthorRef }>();
  for (const ids of keys.values()) {
    for (let i = 0; i < ids.length; i++) {
      for (let j = i + 1; j < ids.length; j++) {
        const a = authors.find((x) => x.id === ids[i]);
        const b = authors.find((x) => x.id === ids[j]);
        if (!a || !b) continue;
        if (a.notDuplicateOf.includes(b.id) || b.notDuplicateOf.includes(a.id)) continue;
        const [first, second] = a.name.localeCompare(b.name) <= 0 ? [a, b] : [b, a];
        pairs.set(`${first.id}:${second.id}`, { a: first, b: second });
      }
    }
  }
  return [...pairs.values()];
}

const escapeRegex = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

// Finds an author by its canonical name key or by a case-insensitive alias, else creates one.
export async function findOrCreateAuthor(rawName: string): Promise<{ id: string; name: string }> {
  const name = rawName.normalize("NFC").trim();
  const nameKey = authorNameKey(name);
  await connectDB();
  const find = () =>
    Author.findOne({ $or: [{ nameKey }, { aliases: { $regex: `^${escapeRegex(name)}$`, $options: "i" } }] }).lean();
  const found = await find();
  if (found) return { id: String(found._id), name: found.name };
  try {
    const created = await Author.create({ name, nameKey, slug: await uniqueSlug(slugify(name), "Author") });
    return { id: created.id, name: created.name };
  } catch (err) {
    // A concurrent form save can win the unique nameKey race; retry the lookup once.
    if ((err as { code?: number }).code !== 11000) throw err;
    const concurrent = await find();
    if (!concurrent) throw err;
    return { id: String(concurrent._id), name: concurrent.name };
  }
}
