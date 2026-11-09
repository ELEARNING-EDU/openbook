"use server";

import { deleteNamedTerm, saveNamedTerm } from "@/lib/taxonomy";
import type { TermInput } from "@/lib/validation";

export async function saveSeries(id: string | null, input: TermInput) {
  return saveNamedTerm("series", id, input);
}

export async function deleteSeries(id: string, moveToId?: string) {
  return deleteNamedTerm("series", id, moveToId);
}
