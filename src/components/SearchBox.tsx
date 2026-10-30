import Form from "next/form";

export function SearchBox({ className = "", size = "md" }: { className?: string; size?: "md" | "lg" }) {
  const lg = size === "lg";
  return (
    <Form action="/books" role="search" className={`relative ${className}`}>
      <label htmlFor={`q-${size}`} className="sr-only">
        Tìm sách
      </label>
      <input
        id={`q-${size}`}
        name="q"
        type="search"
        placeholder="Tìm theo tên sách, tác giả…"
        className={`w-full rounded-full border border-ink/10 bg-white/80 pr-12 pl-5 shadow-sm outline-none transition placeholder:text-ink/45 focus:border-brick focus:ring-4 focus:ring-brick/15 ${lg ? "h-14 text-base" : "h-10 text-sm"}`}
      />
      <button
        type="submit"
        aria-label="Tìm"
        className={`absolute top-1/2 right-1.5 grid -translate-y-1/2 place-items-center rounded-full bg-brick text-white transition hover:bg-orange-700 ${lg ? "size-11" : "size-7"}`}
      >
        <svg viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden>
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
      </button>
    </Form>
  );
}
