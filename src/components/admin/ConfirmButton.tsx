"use client";

import { useTransition, type ReactNode } from "react";
import type { ActionResult } from "@/actions/types";
import { useToast } from "@/components/admin/Toast";

export function ConfirmButton({
  action,
  label,
  confirmText,
  successText = "Đã cập nhật",
  className = "",
}: {
  action: () => Promise<ActionResult>;
  label: ReactNode;
  confirmText?: string;
  successText?: string;
  className?: string;
}) {
  const [pending, start] = useTransition();
  const toast = useToast();
  return (
    <button
      type="button"
      disabled={pending}
      className={`rounded-full px-3 py-1.5 text-sm font-semibold transition disabled:opacity-50 ${className}`}
      onClick={() => {
        if (confirmText && !window.confirm(confirmText)) return;
        start(async () => {
          const r = await action();
          toast(r.ok ? successText : (r.errors._form?.[0] ?? "Thao tác thất bại"), r.ok ? "ok" : "error");
        });
      }}
    >
      {pending ? "…" : label}
    </button>
  );
}
