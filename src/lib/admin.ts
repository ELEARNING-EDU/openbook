export function isAdmin(
  email: string | null | undefined,
  adminEmails = process.env.ADMIN_EMAILS ?? "",
): boolean {
  if (!email) return false;
  const list = adminEmails
    .split(",")
    .map((e) => e.trim().toLowerCase())
    .filter(Boolean);
  return list.includes(email.trim().toLowerCase());
}
