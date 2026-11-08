import { headers } from "next/headers";

type Bucket = { count: number; resetAt: number };

const cache = globalThis as unknown as { rateLimitBuckets?: Map<string, Bucket> };
const buckets = (cache.rateLimitBuckets ??= new Map());

// Fixed window per key. Returns false once the key is over its budget.
// shortcut: in-memory per serverless instance, so traffic spread across instances gets more budget;
// move to a shared store (Redis/Vercel KV) once counts must be exact.
export function rateLimit(key: string, limit: number, windowMs: number): boolean {
  if (buckets.size > 10_000) buckets.clear();
  const now = Date.now();
  const bucket = buckets.get(key);
  if (!bucket || bucket.resetAt <= now) {
    buckets.set(key, { count: 1, resetAt: now + windowMs });
    return true;
  }
  if (bucket.count >= limit) return false;
  bucket.count += 1;
  return true;
}

// Vercel sets x-forwarded-for; the first entry is the client.
export function clientKey(headers: Headers): string {
  return headers.get("x-forwarded-for")?.split(",")[0]?.trim() || headers.get("x-real-ip") || "unknown";
}

// Unit tests and scripts call server actions without a request context; no limit applies there.
export async function requestClientKey(): Promise<string | null> {
  try {
    return clientKey(await headers());
  } catch {
    return null;
  }
}
