"use server";

import type { Types, UpdateQuery } from "mongoose";
import { runAction, formError } from "@/actions/types";
import { requireAdmin } from "@/lib/auth-guard";
import { authorNameKey } from "@/lib/authors";
import { isObjectId, uniqueSlug } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { getT } from "@/lib/i18n/server";
import { slugify } from "@/lib/slug";
import { makeAuthorSchema, type AuthorInput } from "@/lib/validation";
import { Author, type AuthorDoc } from "@/models/Author";
import { Book, type ContributorRole } from "@/models/Book";

type AuthorNames = { name: string; aliases: string[] };

const nameKeys = (author: AuthorNames) => [authorNameKey(author.name), ...(author.aliases ?? []).map(authorNameKey)];

export async function saveAuthor(id: string | null, input: AuthorInput) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const data = makeAuthorSchema(t).parse(input);
    await connectDB();
    const current = id !== null && isObjectId(id) ? await Author.findById(id).lean<AuthorDoc>() : null;
    if (id !== null && !current) return formError(t("action.authorNotFound"));

    const name = data.name.normalize("NFC").replace(/\s+/g, " ");
    const nameKey = authorNameKey(name);
    // Aliases equal to the display name, or duplicates of each other, add nothing.
    const aliasesByKey = new Map<string, string>();
    for (const alias of data.aliases) {
      const key = authorNameKey(alias);
      if (key !== nameKey && !aliasesByKey.has(key)) {
        aliasesByKey.set(key, alias.normalize("NFC").trim().replace(/\s+/g, " "));
      }
    }
    const aliases = [...aliasesByKey.values()];

    const others = await Author.find(id ? { _id: { $ne: id } } : {}, { name: 1, aliases: 1 }).lean<AuthorNames[]>();
    const taken = new Set(others.flatMap(nameKeys));
    if (taken.has(nameKey)) return { ok: false, errors: { name: [t("action.authorNameTaken")] } };
    const badAlias = aliases.find((alias) => taken.has(authorNameKey(alias)));
    if (badAlias) return { ok: false, errors: { aliases: [t("action.authorAliasTaken", { name: badAlias })] } };

    const slug = await uniqueSlug(slugify(name), "Author", id ?? undefined);
    const fields = { name, nameKey, aliases, lifespan: data.lifespan, bio: data.bio, slug };
    if (id !== null) {
      await Author.updateOne({ _id: id }, fields);
      // Linked books keep the public author text in step with the renamed profile.
      if (current && current.name !== name) {
        await Book.updateMany({ authorId: id }, { author: name }, { timestamps: false });
      }
      return { ok: true, id };
    }
    const author = await Author.create(fields);
    return { ok: true, id: author.id };
  });
}

export async function deleteAuthor(id: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(id)) return formError(t("action.authorNotFound"));
    await connectDB();
    if (!(await Author.exists({ _id: id }))) return formError(t("action.authorNotFound"));
    if (await Book.exists({ $or: [{ authorId: id }, { "contributors.authorId": id }] })) {
      return formError(t("action.authorHasBooks"));
    }
    await Author.updateMany({ notDuplicateOf: id }, { $pull: { notDuplicateOf: id } });
    await Author.deleteOne({ _id: id });
    return { ok: true };
  });
}

export async function mergeAuthors(keepId: string, removeId: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(keepId) || !isObjectId(removeId) || keepId === removeId) {
      return formError(t("action.authorMergeInvalid"));
    }
    await connectDB();
    const [keep, remove] = await Promise.all([
      Author.findById(keepId).lean<AuthorDoc>(),
      Author.findById(removeId).lean<AuthorDoc>(),
    ]);
    if (!keep || !remove) return formError(t("action.authorNotFound"));

    // Order matters: books first, then the kept profile, then the delete, so a failure never loses data.
    await Book.updateMany({ authorId: removeId }, { authorId: keepId, author: keep.name }, { timestamps: false });

    // Contributor links move to the kept profile too, then duplicates of the same author and role collapse.
    const linked = await Book.find(
      { "contributors.authorId": removeId },
      { contributors: 1 },
    ).lean<{ _id: Types.ObjectId; contributors: { authorId: Types.ObjectId; role: ContributorRole }[] }[]>();
    for (const book of linked) {
      const seen = new Set<string>();
      const contributors: { authorId: string; role: ContributorRole }[] = [];
      for (const contributor of book.contributors ?? []) {
        const authorId = String(contributor.authorId) === removeId ? keepId : String(contributor.authorId);
        const key = `${authorId}:${contributor.role}`;
        if (seen.has(key)) continue;
        seen.add(key);
        contributors.push({ authorId, role: contributor.role });
      }
      await Book.updateOne({ _id: book._id }, { $set: { contributors } }, { timestamps: false });
    }

    const update: UpdateQuery<AuthorDoc> = {};
    const aliases = [...new Set([remove.name, ...(remove.aliases ?? [])])].filter(
      (alias) => authorNameKey(alias) !== authorNameKey(keep.name),
    );
    if (aliases.length > 0) update.$addToSet = { aliases: { $each: aliases } };
    if (!(keep.bio ?? "").trim() && (remove.bio ?? "").trim()) update.$set = { ...update.$set, bio: remove.bio };
    if (!(keep.lifespan ?? "").trim() && (remove.lifespan ?? "").trim()) {
      update.$set = { ...update.$set, lifespan: remove.lifespan };
    }
    if (Object.keys(update).length > 0) await Author.updateOne({ _id: keepId }, update);

    // No other author may keep a link to the profile that disappears.
    await Author.updateMany({ notDuplicateOf: removeId }, { $pull: { notDuplicateOf: removeId } });
    await Author.deleteOne({ _id: removeId });
    return { ok: true };
  });
}

export async function markNotDuplicate(aId: string, bId: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(aId) || !isObjectId(bId) || aId === bId) return formError(t("action.authorMergeInvalid"));
    await connectDB();
    if (!(await Author.exists({ _id: aId })) || !(await Author.exists({ _id: bId }))) {
      return formError(t("action.authorNotFound"));
    }
    await Promise.all([
      Author.updateOne({ _id: aId }, { $addToSet: { notDuplicateOf: bId } }),
      Author.updateOne({ _id: bId }, { $addToSet: { notDuplicateOf: aId } }),
    ]);
    return { ok: true };
  });
}
