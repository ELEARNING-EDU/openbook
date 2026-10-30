"use client";

import { useState } from "react";

const PLACEHOLDER = "/cover-placeholder.svg";

export function Cover({ src, alt, className = "" }: { src: string; alt: string; className?: string }) {
  const [failed, setFailed] = useState(false);
  return (
    // eslint-disable-next-line @next/next/no-img-element -- covers come from arbitrary domains
    <img
      src={!src || failed ? PLACEHOLDER : src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
      className={`aspect-[2/3] w-full object-cover bg-orange-100 ${className}`}
    />
  );
}
