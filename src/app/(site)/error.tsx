"use client";

import { useT } from "@/components/i18n/LocaleProvider";

export default function ErrorPage({ reset }: { error: Error; reset: () => void }) {
  const t = useT();
  return (
    <div className="mx-auto max-w-lg py-24 text-center">
      <p className="text-5xl">📚</p>
      <h1 className="mt-4 text-2xl font-bold">{t("error.title")}</h1>
      <p className="mt-2 text-ink/70">{t("error.body")}</p>
      <button type="button" onClick={reset} className="mt-6 rounded-full bg-brick px-6 py-3 font-semibold text-white hover:bg-orange-700">
        {t("error.retry")}
      </button>
    </div>
  );
}
