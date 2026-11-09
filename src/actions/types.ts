import { revalidatePath } from "next/cache";
import { z } from "zod";
import { getT } from "@/lib/i18n/server";

export type ActionResult = { ok: true; id?: string } | { ok: false; errors: Record<string, string[]> };

export const formError = (msg: string): ActionResult => ({ ok: false, errors: { _form: [msg] } });

// Wraps a mutation: maps auth/validation failures to ActionResult and refreshes pages on success.
export async function runAction(fn: () => Promise<ActionResult>): Promise<ActionResult> {
  try {
    const result = await fn();
    if (result.ok) revalidatePath("/", "layout");
    return result;
  } catch (err) {
    const t = await getT();
    const msg = err instanceof Error ? err.message : "";
    if (msg === "UNAUTHORIZED" || msg === "FORBIDDEN") return formError(t("action.forbidden"));
    if (err instanceof z.ZodError) return { ok: false, errors: z.flattenError(err).fieldErrors as Record<string, string[]> };
    console.error(err);
    return formError(t("action.generic"));
  }
}
