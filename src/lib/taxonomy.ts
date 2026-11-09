import type { Model } from "mongoose";
import { runAction, formError } from "@/actions/types";
import { requireAdmin } from "@/lib/auth-guard";
import { authorNameKey } from "@/lib/authors";
import { isObjectId, uniqueSlug } from "@/lib/book-service";
import { connectDB } from "@/lib/db";
import { getT } from "@/lib/i18n/server";
import { slugify } from "@/lib/slug";
import { makeTermSchema, type TermInput } from "@/lib/validation";
import { Book } from "@/models/Book";
import { Publisher } from "@/models/Publisher";
import { Series } from "@/models/Series";

export type TermKind = "series" | "publisher";

type NamedDoc = { name: string; nameKey: string; slug: string };
type NamedModel = Model<NamedDoc>;

const TERMS = {
  series: { model: Series as unknown as NamedModel, notFound: "action.seriesNotFound", label: "Series" },
  publisher: { model: Publisher as unknown as NamedModel, notFound: "action.publisherNotFound", label: "Publisher" },
} as const;

export type NamedTerm = { id: string; name: string };

// Series and publishers are created by name from the book form, the same way authors are.
export async function findOrCreateNamedTerm(kind: TermKind, rawName: string): Promise<NamedTerm> {
  const name = rawName.normalize("NFC").trim().replace(/\s+/g, " ");
  const nameKey = authorNameKey(name);
  const { model, label } = TERMS[kind];
  await connectDB();
  const find = () => model.findOne({ nameKey }).lean<{ _id: unknown; name: string }>();
  const found = await find();
  if (found) return { id: String(found._id), name: found.name };
  try {
    const created = await model.create({ name, nameKey, slug: await uniqueSlug(slugify(name), label) });
    return { id: String(created._id), name: created.name };
  } catch (err) {
    // A concurrent form save can win the unique nameKey race; retry the lookup once.
    if ((err as { code?: number }).code !== 11000) throw err;
    const concurrent = await find();
    if (!concurrent) throw err;
    return { id: String(concurrent._id), name: concurrent.name };
  }
}

export async function saveNamedTerm(kind: TermKind, id: string | null, input: TermInput) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    const data = makeTermSchema(t).parse(input);
    await connectDB();
    const { model, notFound, label } = TERMS[kind];
    if (id !== null && !(isObjectId(id) && (await model.exists({ _id: id })))) return formError(t(notFound));

    const name = data.name.normalize("NFC").replace(/\s+/g, " ");
    const nameKey = authorNameKey(name);
    if (await model.exists({ nameKey, ...(id ? { _id: { $ne: id } } : {}) })) {
      return { ok: false as const, errors: { name: [t("action.termNameTaken")] } };
    }
    const slug = await uniqueSlug(slugify(name), label, id ?? undefined);
    if (id) {
      await model.updateOne({ _id: id }, { name, nameKey, slug });
      return { ok: true as const, id };
    }
    const term = await model.create({ name, nameKey, slug });
    return { ok: true as const, id: String(term._id) };
  });
}

export async function deleteNamedTerm(kind: TermKind, id: string, moveToId?: string) {
  return runAction(async () => {
    const t = await getT();
    await requireAdmin();
    if (!isObjectId(id)) return formError(t(TERMS[kind].notFound));
    await connectDB();
    const { model, notFound } = TERMS[kind];
    if (!(await model.exists({ _id: id }))) return formError(t(notFound));

    // Books are never deleted: they either move to another term or lose the reference.
    let replacement: string | null = null;
    if (moveToId !== undefined) {
      if (!isObjectId(moveToId) || moveToId === id || !(await model.exists({ _id: moveToId }))) {
        return formError(t("action.termMoveInvalid"));
      }
      replacement = moveToId;
    }
    const set =
      kind === "series"
        ? replacement
          ? { seriesId: replacement }
          : { seriesId: null, seriesNumber: null }
        : { publisherId: replacement };
    await Book.updateMany({ [kind === "series" ? "seriesId" : "publisherId"]: id }, { $set: set });
    await model.deleteOne({ _id: id });
    return { ok: true as const };
  });
}
