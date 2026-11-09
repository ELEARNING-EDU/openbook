"use client";

import { useTransition, type ReactNode } from "react";
import type { ActionResult } from "@/actions/types";
import { useT } from "@/components/i18n/LocaleProvider";
import { useToast } from "@/components/admin/Toast";

export function ConfirmButton({
  action,
  label,
  confirmText,
  successText,
  className = "",
  onSuccess,
}: {
  action: () => Promise<ActionResult>;
  label: ReactNode;
  confirmText?: string;
  successText?: string;
  className?: string;
  onSuccess?: () => void;
}) {
  const t = useT();
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
          toast(r.ok ? (successText ?? t("common.updated")) : (r.errors._form?.[0] ?? t("common.actionFailed")), r.ok ? "ok" : "error");
          if (r.ok) onSuccess?.();
        });
      }}
    >
      {pending ? "…" : label}
    </button>
  );
}
