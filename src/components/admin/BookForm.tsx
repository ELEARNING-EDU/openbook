"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { saveBook } from "@/actions/books";
import { Cover } from "@/components/Cover";
import { useToast } from "@/components/admin/Toast";
import type { BookDTO, CategoryDTO } from "@/lib/queries";

const input =
  "w-full rounded-xl border border-ink/15 bg-white px-3.5 py-2.5 outline-none transition focus:border-brick focus:ring-4 focus:ring-brick/15";

function FieldError({ errors }: { errors?: string[] }) {
  return errors?.[0] ? <p className="mt-1 text-sm text-red-700">{errors[0]}</p> : null;
}

export function BookForm({ book, categories }: { book?: BookDTO; categories: CategoryDTO[] }) {
  const router = useRouter();
  const toast = useToast();
  const [pending, start] = useTransition();
  const [errors, setErrors] = useState<Record<string, string[]>>({});
  const [title, setTitle] = useState(book?.title ?? "");
  const [author, setAuthor] = useState(book?.author ?? "");
  const [description, setDescription] = useState(book?.description ?? "");
  const [coverUrl, setCoverUrl] = useState(book?.coverUrl ?? "");
  const [links, setLinks] = useState(book?.links.length ? book.links : [{ label: "Đọc online", url: "" }]);
  const [categoryIds, setCategoryIds] = useState<string[]>(book?.categories.map((c) => c.id) ?? []);
  const [featured, setFeatured] = useState(book?.featured ?? false);

  const setLink = (i: number, key: "label" | "url", value: string) =>
    setLinks((ls) => ls.map((l, j) => (j === i ? { ...l, [key]: value } : l)));

  return (
    <form
      noValidate
      className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_14rem]"
      onSubmit={(e) => {
        e.preventDefault();
        start(async () => {
          const r = await saveBook(book?.id ?? null, { title, author, description, coverUrl, links, categoryIds, featured });
          if (r.ok) {
            toast("Đã lưu");
            router.push("/admin/books");
          } else {
            setErrors(r.errors);
            toast(r.errors._form?.[0] ?? "Vui lòng kiểm tra lại các trường", "error");
          }
        });
      }}
    >
      <div className="space-y-5">
        {errors._form && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{errors._form[0]}</p>}
        <div>
          <label htmlFor="title" className="mb-1 block text-sm font-semibold">Tên sách *</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} className={input} />
          <FieldError errors={errors.title} />
        </div>
        <div>
          <label htmlFor="author" className="mb-1 block text-sm font-semibold">Tác giả *</label>
          <input id="author" value={author} onChange={(e) => setAuthor(e.target.value)} className={input} />
          <FieldError errors={errors.author} />
        </div>
        <div>
          <label htmlFor="description" className="mb-1 block text-sm font-semibold">Mô tả</label>
          <textarea id="description" rows={5} value={description} onChange={(e) => setDescription(e.target.value)} className={input} />
          <FieldError errors={errors.description} />
        </div>
        <div>
          <label htmlFor="coverUrl" className="mb-1 block text-sm font-semibold">URL ảnh bìa</label>
          <input id="coverUrl" type="url" placeholder="https://…" value={coverUrl} onChange={(e) => setCoverUrl(e.target.value)} className={input} />
          <FieldError errors={errors.coverUrl} />
        </div>
        <fieldset>
          <legend className="mb-1 text-sm font-semibold">Link đọc / tải *</legend>
          <div className="space-y-2">
            {links.map((l, i) => (
              <div key={i} className="flex flex-col gap-2 rounded-xl bg-white/60 p-2 sm:flex-row">
                <input aria-label={`Nhãn link ${i + 1}`} placeholder="Nhãn (vd: Tải PDF)" value={l.label} onChange={(e) => setLink(i, "label", e.target.value)} className={`${input} sm:w-44`} />
                <input aria-label={`URL link ${i + 1}`} type="url" placeholder="https://…" value={l.url} onChange={(e) => setLink(i, "url", e.target.value)} className={input} />
                {links.length > 1 && (
                  <button type="button" aria-label={`Xoá link ${i + 1}`} onClick={() => setLinks((ls) => ls.filter((_, j) => j !== i))} className="rounded-xl px-3 text-red-700 hover:bg-red-50">
                    Xoá
                  </button>
                )}
              </div>
            ))}
          </div>
          <FieldError errors={errors.links} />
          {links.length < 10 && (
            <button type="button" onClick={() => setLinks((ls) => [...ls, { label: "", url: "" }])} className="mt-2 text-sm font-semibold text-brick hover:underline">
              + Thêm link
            </button>
          )}
        </fieldset>
        <fieldset>
          <legend className="mb-2 text-sm font-semibold">Thể loại</legend>
          {categories.length === 0 && <p className="text-sm text-ink/60">Chưa có thể loại nào.</p>}
          <div className="flex flex-wrap gap-2">
            {categories.map((c) => {
              const checked = categoryIds.includes(c.id);
              return (
                <label key={c.id} className={`cursor-pointer rounded-full border px-3 py-1.5 text-sm transition has-focus-visible:ring-4 has-focus-visible:ring-brick/20 ${checked ? "border-moss bg-moss text-white" : "border-ink/15 bg-white hover:border-moss"}`}>
                  <input
                    type="checkbox"
                    className="sr-only"
                    checked={checked}
                    onChange={() => setCategoryIds((ids) => (checked ? ids.filter((x) => x !== c.id) : [...ids, c.id]))}
                  />
                  {c.name}
                </label>
              );
            })}
          </div>
        </fieldset>
        <label className="flex items-center gap-2 text-sm font-semibold">
          <input type="checkbox" checked={featured} onChange={(e) => setFeatured(e.target.checked)} className="size-4 accent-brick" />
          Sách nổi bật
        </label>
        <div className="flex gap-3">
          <button type="submit" disabled={pending} className="rounded-full bg-brick px-6 py-3 font-semibold text-white shadow transition hover:bg-orange-700 disabled:opacity-50">
            {pending ? "Đang lưu…" : "Lưu sách"}
          </button>
          <button type="button" onClick={() => router.back()} className="rounded-full px-5 py-3 font-semibold hover:bg-orange-100">
            Huỷ
          </button>
        </div>
      </div>
      <aside className="order-first lg:order-none">
        <p className="mb-2 text-sm font-semibold">Xem trước bìa</p>
        <div className="w-40 lg:w-full">
          <Cover key={coverUrl} src={coverUrl} alt="Xem trước ảnh bìa" className="rounded-2xl shadow-lg" />
        </div>
      </aside>
    </form>
  );
}
