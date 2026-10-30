export function Stars({ value, size = "sm" }: { value: number; size?: "sm" | "lg" }) {
  const full = Math.round(value);
  return (
    <span
      role="img"
      aria-label={`${value.toFixed(1)} trên 5 sao`}
      className={`inline-flex tracking-tight ${size === "lg" ? "text-2xl" : "text-sm"}`}
    >
      {[1, 2, 3, 4, 5].map((i) => (
        <span key={i} aria-hidden className={i <= full ? "text-amber-500" : "text-ink/20"}>
          ★
        </span>
      ))}
    </span>
  );
}
