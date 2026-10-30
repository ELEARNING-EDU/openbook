import Link from "next/link";
import { signIn, signOut } from "@/auth";
import type { SessionUser } from "@/lib/auth-guard";

export function SignInButton({ label = "Đăng nhập Google", callbackUrl }: { label?: string; callbackUrl?: string }) {
  return (
    <form
      action={async () => {
        "use server";
        await signIn("google", { redirectTo: callbackUrl });
      }}
    >
      <button
        type="submit"
        className="inline-flex h-10 items-center gap-2 rounded-full bg-ink px-4 text-sm font-semibold text-cream transition hover:bg-brick"
      >
        <span aria-hidden className="grid size-5 place-items-center rounded-full bg-white text-xs font-bold text-brick">
          G
        </span>
        {label}
      </button>
    </form>
  );
}

export function UserMenu({ user }: { user: SessionUser | null }) {
  if (!user) return <SignInButton />;
  return (
    <div className="flex flex-wrap items-center gap-3">
      {user.image ? (
        // eslint-disable-next-line @next/next/no-img-element -- Google avatar URL
        <img src={user.image} alt="" referrerPolicy="no-referrer" className="size-9 rounded-full ring-2 ring-brick/30" />
      ) : (
        <span className="grid size-9 place-items-center rounded-full bg-brick font-bold text-white">{user.name[0]}</span>
      )}
      <span className="max-w-32 truncate text-sm font-medium">{user.name}</span>
      {user.isAdmin && (
        <Link href="/admin" className="rounded-full bg-moss px-3 py-1.5 text-xs font-semibold text-white hover:opacity-90">
          Quản trị
        </Link>
      )}
      <form
        action={async () => {
          "use server";
          await signOut({ redirectTo: "/" });
        }}
      >
        <button type="submit" className="text-sm text-ink/70 underline-offset-4 hover:text-brick hover:underline">
          Đăng xuất
        </button>
      </form>
    </div>
  );
}
