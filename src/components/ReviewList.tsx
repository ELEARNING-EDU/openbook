import type { ReviewDTO } from "@/lib/queries";
import { Stars } from "@/components/Stars";

export function ReviewList({ reviews }: { reviews: ReviewDTO[] }) {
  if (reviews.length === 0) return <p className="text-ink/60">Chưa có đánh giá nào. Hãy là người đầu tiên!</p>;
  return (
    <ul className="space-y-4">
      {reviews.map((r) => (
        <li key={r.id} className="rounded-2xl bg-white/60 p-4 ring-1 ring-ink/5">
          <div className="flex items-center gap-3">
            {r.userImage ? (
              // eslint-disable-next-line @next/next/no-img-element -- Google avatar URL
              <img src={r.userImage} alt="" referrerPolicy="no-referrer" className="size-9 rounded-full" />
            ) : (
              <span className="grid size-9 place-items-center rounded-full bg-orange-200 font-bold">{r.userName[0]}</span>
            )}
            <div className="min-w-0">
              <p className="truncate font-semibold">{r.userName}</p>
              <p className="text-xs text-ink/60">{new Date(r.createdAt).toLocaleDateString("vi-VN")}</p>
            </div>
            <div className="ml-auto">
              <Stars value={r.rating} />
            </div>
          </div>
          {r.comment && <p className="mt-3 whitespace-pre-line text-ink/85">{r.comment}</p>}
        </li>
      ))}
    </ul>
  );
}
