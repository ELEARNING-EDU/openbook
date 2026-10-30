"use client";

export default function ErrorPage({ reset }: { error: Error; reset: () => void }) {
  return (
    <div className="mx-auto max-w-lg py-24 text-center">
      <p className="text-5xl">📚</p>
      <h1 className="mt-4 text-2xl font-bold">Đã có lỗi xảy ra</h1>
      <p className="mt-2 text-ink/70">Không tải được dữ liệu. Vui lòng thử lại sau giây lát.</p>
      <button type="button" onClick={reset} className="mt-6 rounded-full bg-brick px-6 py-3 font-semibold text-white hover:bg-orange-700">
        Thử lại
      </button>
    </div>
  );
}
