import Link from "next/link";
import { Suspense } from "react";
import { deleteReview, setReviewHidden } from "@/actions/reviews";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { Stars } from "@/components/Stars";
import { assertAdminPage } from "@/lib/auth-guard";
import { listAllReviews } from "@/lib/queries";

export const metadata = { title: "Bình luận" };

export default function AdminReviewsPage() {
  return (
    <div>
      <h1 className="mb-2 text-3xl font-bold">Bình luận</h1>
      <p className="mb-6 text-sm text-ink/60">100 đánh giá mới nhất</p>
      <Suspense fallback={<p className="text-ink/60">Đang tải…</p>}>
        <Reviews />
      </Suspense>
    </div>
  );
}

async function Reviews() {
  await assertAdminPage();
  const reviews = await listAllReviews();
  return (
    <>
      {reviews.length === 0 ? (
        <p className="text-ink/60">Chưa có đánh giá nào.</p>
      ) : (
        <ul className="space-y-3">
          {reviews.map((r) => (
            <li key={r.id} className={`rounded-2xl p-4 ring-1 ring-ink/5 ${r.hidden ? "bg-ink/5 opacity-75" : "bg-white/70"}`}>
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
                <span className="font-semibold">{r.userName}</span>
                <Stars value={r.rating} />
                {r.bookSlug ? (
                  <Link href={`/books/${r.bookSlug}`} className="text-brick hover:underline">{r.bookTitle}</Link>
                ) : (
                  <span className="text-ink/50">(sách đã xoá)</span>
                )}
                <span className="text-xs text-ink/50">{new Date(r.createdAt).toLocaleString("vi-VN")}</span>
                {r.hidden && <span className="rounded-full bg-ink/10 px-2 py-0.5 text-xs font-semibold">Đã ẩn</span>}
              </div>
              {r.comment && <p className="mt-2 whitespace-pre-line text-ink/85">{r.comment}</p>}
              <div className="mt-3 flex gap-1">
                <ConfirmButton
                  action={setReviewHidden.bind(null, r.id, !r.hidden)}
                  label={r.hidden ? "Hiện" : "Ẩn"}
                  successText={r.hidden ? "Đã hiện bình luận" : "Đã ẩn bình luận"}
                  className="hover:bg-orange-100"
                />
                <ConfirmButton action={deleteReview.bind(null, r.id)} label="Xoá" confirmText="Xoá vĩnh viễn đánh giá này?" successText="Đã xoá" className="text-red-700 hover:bg-red-50" />
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
