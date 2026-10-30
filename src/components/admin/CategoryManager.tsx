"use client";

import { useState, useTransition } from "react";
import { deleteCategory, saveCategory } from "@/actions/categories";
import { ConfirmButton } from "@/components/admin/ConfirmButton";
import { useToast } from "@/components/admin/Toast";
import type { CategoryDTO } from "@/lib/queries";

const input = "h-10 w-full rounded-full border border-ink/15 bg-white px-4 text-sm outline-none focus:border-brick focus:ring-4 focus:ring-brick/15";

function NameForm({ id, initial, submitLabel, onDone }: { id: string | null; initial: string; submitLabel: string; onDone?: () => void }) {
  const [name, setName] = useState(initial);
  const [error, setError] = useState<string>();
  const [pending, start] = useTransition();
  const toast = useToast();
  return (
    <form
      className="flex flex-1 flex-wrap items-start gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        start(async () => {
          const r = await saveCategory(id, { name });
          if (r.ok) {
            toast("Đã lưu thể loại");
            setError(undefined);
            if (!id) setName("");
            onDone?.();
          } else setError(r.errors.name?.[0] ?? r.errors._form?.[0]);
        });
      }}
    >
      <div className="min-w-48 flex-1">
        <input aria-label="Tên thể loại" placeholder="Tên thể loại" value={name} onChange={(e) => setName(e.target.value)} className={input} />
        {error && <p className="mt-1 px-4 text-sm text-red-700">{error}</p>}
      </div>
      <button type="submit" disabled={pending} className="h-10 rounded-full bg-brick px-4 text-sm font-semibold text-white disabled:opacity-50">
        {submitLabel}
      </button>
    </form>
  );
}

function Row({ category }: { category: CategoryDTO }) {
  const [editing, setEditing] = useState(false);
  return (
    <li className="flex flex-wrap items-center gap-2 rounded-2xl bg-white/70 p-3 ring-1 ring-ink/5">
      {editing ? (
        <>
          <NameForm id={category.id} initial={category.name} submitLabel="Lưu" onDone={() => setEditing(false)} />
          <button type="button" onClick={() => setEditing(false)} className="rounded-full px-3 py-1.5 text-sm hover:bg-orange-100">Huỷ</button>
        </>
      ) : (
        <>
          <span className="flex-1 font-semibold">{category.name}</span>
          <span className="text-xs text-ink/50">/{category.slug}</span>
          <button type="button" onClick={() => setEditing(true)} className="rounded-full px-3 py-1.5 text-sm font-semibold text-brick hover:bg-orange-100">Sửa</button>
          <ConfirmButton
            action={deleteCategory.bind(null, category.id)}
            label="Xoá"
            confirmText={`Xoá thể loại “${category.name}”? Sách thuộc thể loại này sẽ không bị xoá.`}
            successText="Đã xoá thể loại"
            className="text-red-700 hover:bg-red-50"
          />
        </>
      )}
    </li>
  );
}

export function CategoryManager({ categories }: { categories: CategoryDTO[] }) {
  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-orange-50 p-4">
        <NameForm id={null} initial="" submitLabel="+ Thêm" />
      </div>
      {categories.length === 0 ? (
        <p className="text-ink/60">Chưa có thể loại nào.</p>
      ) : (
        <ul className="space-y-2">
          {categories.map((c) => (
            <Row key={`${c.id}-${c.name}`} category={c} />
          ))}
        </ul>
      )}
    </div>
  );
}
