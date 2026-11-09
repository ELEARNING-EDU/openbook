"use server";

import { deleteNamedTerm, saveNamedTerm } from "@/lib/taxonomy";
import type { TermInput } from "@/lib/validation";

export async function savePublisher(id: string | null, input: TermInput) {
  return saveNamedTerm("publisher", id, input);
}

export async function deletePublisher(id: string, moveToId?: string) {
  return deleteNamedTerm("publisher", id, moveToId);
}
