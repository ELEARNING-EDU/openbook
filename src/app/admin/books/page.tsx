import Form from "next/form";
import Link from "next/link";
import { Suspense } from "react";
import { deleteBook, toggleFeatured } from "@/actions/books";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { Cover } from "@/components/Cover";
import { Pagination } from "@/components/Pagination";
import { Stars } from "@/components/Stars";
import { assertAdminPage } from "@/lib/auth-guard";
import { listBooks, type BookDTO } from "@/lib/queries";
import { parsePage } from "@/lib/validation";

export const metadata = { title: "Sách" };

function RowActions({ book }: { book: BookDTO }) {
  return (
    <div className="flex flex-wrap items-center gap-1">
      <ConfirmButton
        action={toggleFeatured.bind(null, book.id)}
        label={book.featured ? "★ Nổi bật" : "☆ Nổi bật"}
        className={book.featured ? "bg-amber-100 text-amber-800" : "hover:bg-orange-100"}
      />
      <Link href={`/admin/books/${book.id}`} className="rounded-full px-3 py-1.5 text-sm font-semibold text-brick hover:bg-orange-100">
        Sửa
      </Link>
      <ConfirmButton
        action={deleteBook.bind(null, book.id)}
        label="Xoá"
        confirmText={`Xoá sách “${book.title}”? Mọi đánh giá của sách cũng bị xoá.`}
        successText="Đã xoá sách"
        className="text-red-700 hover:bg-red-50"
      />
    </div>
  );
}

async function BookTable({ searchParams }: { searchParams: PageProps<"/admin/books">["searchParams"] }) {
  await assertAdminPage();
  const sp = await searchParams;
  const q = typeof sp.q === "string" ? sp.q.trim() : "";
  const page = parsePage(sp.page);
  const { books, total, totalPages } = await listBooks({ q, page });
  return (
    <>
      <p className="mb-3 text-sm text-ink/60">{total} cuốn sách</p>
      {books.length === 0 ? (
        <p className="rounded-2xl bg-white/60 p-10 text-center text-ink/70">Không tìm thấy sách</p>
      ) : (
        <>
          <table className="hidden w-full overflow-hidden rounded-2xl bg-white/70 text-left text-sm ring-1 ring-ink/5 md:table">
            <thead className="bg-orange-50 text-xs tracking-wider text-ink/60 uppercase">
              <tr>
                <th className="p-3">Sách</th>
                <th className="p-3">Thể loại</th>
                <th className="p-3">Đánh giá</th>
                <th className="p-3">Lượt tải</th>
                <th className="p-3"><span className="sr-only">Thao tác</span></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ink/5">
              {books.map((b) => (
                <tr key={b.id} className="hover:bg-orange-50/50">
                  <td className="p-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 shrink-0"><Cover src={b.coverUrl} alt="" className="rounded-md" /></div>
                      <div className="min-w-0">
                        <Link href={`/books/${b.slug}`} className="font-semibold hover:text-brick">{b.title}</Link>
                        <p className="text-ink/60">{b.author}</p>
                      </div>
                    </div>
                  </td>
                  <td className="p-3 text-ink/70">{b.categories.map((c) => c.name).join(", ") || "—"}</td>
                  <td className="p-3"><Stars value={b.ratingAvg} /> <span className="text-xs text-ink/60">({b.ratingCount})</span></td>
                  <td className="p-3">{b.downloadCount.toLocaleString("vi-VN")}</td>
                  <td className="p-3"><RowActions book={b} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          <ul className="space-y-3 md:hidden">
            {books.map((b) => (
              <li key={b.id} className="flex gap-3 rounded-2xl bg-white/70 p-3 ring-1 ring-ink/5">
                <div className="w-16 shrink-0"><Cover src={b.coverUrl} alt="" className="rounded-lg" /></div>
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">{b.title}</p>
                  <p className="text-sm text-ink/60">{b.author}</p>
                  <p className="text-xs text-ink/60">🔥 {b.downloadCount.toLocaleString("vi-VN")} · ★ {b.ratingAvg.toFixed(1)}</p>
                  <div className="mt-2"><RowActions book={b} /></div>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
      <Pagination page={page} totalPages={totalPages} baseHref={q ? `/admin/books?q=${encodeURIComponent(q)}` : "/admin/books"} />
    </>
  );
}

export default function AdminBooksPage(props: PageProps<"/admin/books">) {
  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold">Sách</h1>
        <Link href="/admin/books/new" className="rounded-full bg-brick px-5 py-2.5 font-semibold text-white shadow hover:bg-orange-700">
          + Thêm sách
        </Link>
      </div>
      <Form action="/admin/books" className="mb-4 flex gap-2">
        <label htmlFor="admin-q" className="sr-only">Tìm sách</label>
        <input id="admin-q" name="q" type="search" placeholder="Tìm theo tên, tác giả…" className="h-10 w-full max-w-sm rounded-full border border-ink/15 bg-white px-4 text-sm outline-none focus:border-brick" />
        <button type="submit" className="rounded-full bg-ink px-4 text-sm font-semibold text-cream">Tìm</button>
      </Form>
      <Suspense fallback={<p className="text-ink/60">Đang tải…</p>}>
        <BookTable searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}
