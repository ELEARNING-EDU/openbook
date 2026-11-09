"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { useState, useTransition } from "react";
import { deleteAuthor, saveAuthor } from "@/actions/authors";
import { useToast } from "@/components/admin/Toast";
import { useT } from "@/components/i18n/LocaleProvider";
import type { AdminAuthorDetail, AuthorBookRole, AuthorOption } from "@/lib/queries";

const ROLE_KEYS = {
  author: "form.roleAuthor",
  translator: "form.roleTranslator",
  editor: "form.roleEditor",
  illustrator: "form.roleIllustrator",
} as const satisfies Record<AuthorBookRole, string>;

const input =
  "h-11 w-full rounded-xl border border-ink/15 bg-surface px-3.5 text-sm outline-none transition focus:border-brick focus:ring-4 focus:ring-brick/15";
const primaryButton =
  "min-h-11 rounded-full bg-brick px-5 text-sm font-semibold text-white transition hover:bg-orange-700 disabled:opacity-50";
const secondaryButton =
  "min-h-11 rounded-full border border-ink/15 bg-surface px-4 text-sm font-semibold transition hover:bg-tint disabled:opacity-50";

function FieldError({ errors }: { errors?: string[] }) {
  return errors?.[0] ? <p className="mt-1 text-sm text-red-700 dark:text-red-300">{errors[0]}</p> : null;
}

export function AuthorForm({ author, options }: { author?: AdminAuthorDetail; options: AuthorOption[] }) {
  const t = useT();
  const router = useRouter();
  const toast = useToast();
  const [pending, start] = useTransition();
  const [errors, setErrors] = useState<Record<string, string[]>>({});
  const [name, setName] = useState(author?.name ?? "");
  const [aliases, setAliases] = useState(author?.aliases.join(", ") ?? "");
  const [lifespan, setLifespan] = useState(author?.lifespan ?? "");
  const [bio, setBio] = useState(author?.bio ?? "");
  const [mergeId, setMergeId] = useState("");
  const otherOptions = options.filter((option) => option.id !== author?.id);
  const blocked = !!author && author.bookCount > 0;

  const save = () =>
    start(async () => {
      const r = await saveAuthor(author?.id ?? null, {
        name,
        aliases: aliases
          .split(",")
          .map((alias) => alias.trim())
          .filter(Boolean),
        lifespan,
        bio,
      });
      if (r.ok) {
        toast(t("admin.authorSaved"));
        setErrors({});
        if (author) router.refresh();
        else router.push(`/admin/authors?id=${r.id ?? ""}`);
      } else {
        setErrors(r.errors);
        toast(r.errors._form?.[0] ?? t("form.checkFields"), "error");
      }
    });

  const remove = () => {
    if (!author || !window.confirm(t("admin.authorDeleteConfirm", { name: author.name }))) return;
    start(async () => {
      const r = await deleteAuthor(author.id);
      if (r.ok) {
        toast(t("admin.authorDeleted"));
        router.push("/admin/authors");
      } else toast(r.errors._form?.[0] ?? t("common.actionFailed"), "error");
    });
  };

  return (
    <section className="flex flex-col gap-4 rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5">
      <h2 className="text-2xl">{author?.name ?? t("admin.newAuthor")}</h2>
      <form
        noValidate
        className="flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        {errors._form && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">{errors._form[0]}</p>}
        <div>
          <label htmlFor="author-name" className="mb-1 block text-sm font-semibold">{t("admin.authorName")}</label>
          <input id="author-name" value={name} onChange={(e) => setName(e.target.value)} className={input} />
          <FieldError errors={errors.name} />
        </div>
        <div>
          <label htmlFor="author-aliases" className="mb-1 block text-sm font-semibold">{t("admin.authorAliases")}</label>
          <input id="author-aliases" value={aliases} onChange={(e) => setAliases(e.target.value)} placeholder={t("admin.authorAliasesPlaceholder")} className={input} />
          <FieldError errors={errors.aliases} />
        </div>
        <div>
          <label htmlFor="author-lifespan" className="mb-1 block text-sm font-semibold">{t("admin.authorLifespan")}</label>
          <input id="author-lifespan" value={lifespan} onChange={(e) => setLifespan(e.target.value)} className={input} />
          <FieldError errors={errors.lifespan} />
        </div>
        <div>
          <label htmlFor="author-bio" className="mb-1 block text-sm font-semibold">{t("admin.authorBio")}</label>
          <textarea id="author-bio" rows={3} value={bio} onChange={(e) => setBio(e.target.value)} placeholder={t("admin.authorBioPlaceholder")} className="w-full resize-y rounded-xl border border-ink/15 bg-surface px-3.5 py-2.5 text-sm outline-none transition focus:border-brick focus:ring-4 focus:ring-brick/15" />
          <FieldError errors={errors.bio} />
        </div>

        {author && (
          <div>
            <h3 className="mb-1 text-sm font-semibold">{t("admin.authorBooks")}</h3>
            {author.books.length === 0 ? (
              <p className="text-sm text-ink/60">{t("admin.authorNoBooks")}</p>
            ) : (
              <ul>
                {author.books.map((book) => (
                  <li key={book.id}>
                    <Link href={`/admin/books/${book.id}`} className="inline-flex min-h-11 items-center font-medium text-brick hover:underline dark:text-orange-400">
                      {book.title}
                    </Link>
                    <span className="ml-2 text-sm text-ink/60">{t(ROLE_KEYS[book.role])}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="flex flex-wrap items-center gap-2">
          <button type="submit" disabled={pending} className={primaryButton}>{t("common.save")}</button>
          {author && (
            <button
              type="button"
              disabled={pending || blocked}
              onClick={remove}
              className="min-h-11 rounded-full px-4 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-40 disabled:hover:bg-transparent dark:text-red-300 dark:hover:bg-red-950/40"
            >
              {t("common.delete")}
            </button>
          )}
          {!author && (
            <button type="button" onClick={() => router.push("/admin/authors")} className={secondaryButton}>
              {t("common.cancel")}
            </button>
          )}
        </div>
        {blocked && <p className="text-sm text-ink/60">{t("admin.authorDeleteBlocked")}</p>}
      </form>

      {author && otherOptions.length > 0 && (
        <div className="flex flex-col gap-1 border-t border-ink/5 pt-4">
          <label htmlFor="merge-with" className="text-sm font-semibold">{t("admin.mergeWith")}</label>
          <div className="flex flex-wrap gap-2">
            <select id="merge-with" value={mergeId} onChange={(e) => setMergeId(e.target.value)} className="h-11 min-w-48 rounded-xl border border-ink/15 bg-surface px-3 text-sm">
              <option value="">{t("admin.mergeChoose")}</option>
              {otherOptions.map((option) => (
                <option key={option.id} value={option.id}>{option.name}</option>
              ))}
            </select>
            <button
              type="button"
              disabled={!mergeId}
              onClick={() => router.push(`/admin/authors?id=${author.id}&merge=${mergeId}`)}
              className={secondaryButton}
            >
              {t("admin.mergeWith")}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
