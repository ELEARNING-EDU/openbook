import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto flex max-w-lg flex-1 flex-col items-center justify-center px-4 py-24 text-center">
      <p className="font-display text-7xl font-bold text-brick">404</p>
      <h1 className="mt-4 text-2xl font-bold">Không tìm thấy trang</h1>
      <p className="mt-2 text-ink/70">Cuốn sách hoặc trang bạn tìm có thể đã bị gỡ hoặc chưa từng tồn tại.</p>
      <Link href="/" className="mt-6 rounded-full bg-brick px-6 py-3 font-semibold text-white hover:bg-orange-700">
        Về trang chủ
      </Link>
    </main>
  );
}
