import { notFound, redirect } from "next/navigation";
import { auth } from "@/auth";
import { isAdmin } from "@/lib/admin";

export type SessionUser = { id: string; name: string; email: string; image?: string; isAdmin: boolean };

export async function getSessionUser(): Promise<SessionUser | null> {
  const u = (await auth())?.user;
  if (!u?.id || !u.email) return null;
  return { id: u.id, name: u.name ?? u.email, email: u.email, image: u.image ?? undefined, isAdmin: isAdmin(u.email) };
}

export async function requireUser(): Promise<SessionUser> {
  const user = await getSessionUser();
  if (!user) throw new Error("UNAUTHORIZED");
  return user;
}

export async function requireAdmin(): Promise<SessionUser> {
  const user = await requireUser();
  if (!user.isAdmin) throw new Error("FORBIDDEN");
  return user;
}

export async function assertAdminPage(): Promise<SessionUser> {
  const user = await getSessionUser();
  if (!user) redirect("/api/auth/signin?callbackUrl=/admin");
  if (!user.isAdmin) notFound();
  return user;
}
