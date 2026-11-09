"use client";

import { useState, useTransition } from "react";
import { submitReview } from "@/actions/reviews";
import { useT } from "@/components/i18n/LocaleProvider";

export function ReviewForm({ bookId, initial }: { bookId: string; initial?: { rating: number; comment: string } }) {
  const t = useT();
  const [rating, setRating] = useState(initial?.rating ?? 0);
  const [comment, setComment] = useState(initial?.comment ?? "");
  const [errors, setErrors] = useState<Record<string, string[]>>({});
  const [done, setDone] = useState(false);
  const [pending, start] = useTransition();

  return (
    <form
      className="space-y-4 rounded-2xl bg-surface/70 p-5 ring-1 ring-ink/5"
      onSubmit={(e) => {
        e.preventDefault();
        setDone(false);
        start(async () => {
          const r = await submitReview(bookId, { rating, comment });
          setErrors(r.ok ? {} : r.errors);
          setDone(r.ok);
        });
      }}
    >
      <p className="font-display text-lg font-semibold">{initial ? t("review.editTitle") : t("review.writeTitle")}</p>
      {errors._form && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">{errors._form[0]}</p>}
      <fieldset>
        <legend className="mb-1 text-sm font-medium">{t("review.ratingLegend")}</legend>
        <div className="flex flex-row-reverse justify-end gap-1">
          {[5, 4, 3, 2, 1].map((n) => (
            <label key={n} className="cursor-pointer text-3xl">
              <input type="radio" name="rating" value={n} checked={rating === n} onChange={() => setRating(n)} className="peer sr-only" />
              <span className={`transition peer-focus-visible:outline-2 peer-focus-visible:outline-brick ${n <= rating ? "text-amber-500" : "text-ink/20 hover:text-amber-300"}`}>★</span>
              <span className="sr-only">{t("review.starN", { n })}</span>
            </label>
          ))}
        </div>
        {errors.rating && <p className="mt-1 text-sm text-red-700 dark:text-red-300">{errors.rating[0]}</p>}
      </fieldset>
      <div>
        <label htmlFor="comment" className="mb-1 block text-sm font-medium">
          {t("review.commentLabel")}
        </label>
        <textarea
          id="comment"
          value={comment}
          maxLength={2000}
          rows={4}
          onChange={(e) => setComment(e.target.value)}
          className="w-full rounded-xl border border-ink/10 bg-surface p-3 outline-none focus:border-brick focus:ring-4 focus:ring-brick/15"
        />
        <div className="flex justify-between text-xs text-ink/60">
          <span className="text-red-700 dark:text-red-300">{errors.comment?.[0]}</span>
          <span>{comment.length}/2000</span>
        </div>
      </div>
      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={pending || rating === 0}
          className="rounded-full bg-brick px-5 py-2.5 font-semibold text-white transition hover:bg-orange-700 disabled:opacity-50"
        >
          {pending ? t("review.submitting") : t("review.submit")}
        </button>
        {done && <p role="status" className="text-sm text-moss">{t("review.thanks")}</p>}
      </div>
    </form>
  );
}
