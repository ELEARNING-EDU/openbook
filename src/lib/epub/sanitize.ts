import DOMPurify from "dompurify";
import { resolvePath } from "@/lib/epub/parse";

type Opts = { baseHref: string; imageUrl(path: string): string | undefined; chapterIndexOf(href: string): number };

const FORBID_TAGS = ["style", "link", "iframe", "object", "embed", "form", "input", "button", "textarea", "select", "svg", "math", "video", "audio"];

/** Returns safe body HTML for one EPUB chapter: no scripts/styles, images as blob URLs, links rewritten. */
export function sanitizeChapter(xhtml: string, { baseHref, imageUrl, chapterIndexOf }: Opts): string {
  let doc = new DOMParser().parseFromString(xhtml, "application/xhtml+xml");
  if (doc.getElementsByTagName("parsererror").length) doc = new DOMParser().parseFromString(xhtml, "text/html");
  const body = doc.getElementsByTagName("body")[0];
  if (!body) return "";

  const clean = DOMPurify.sanitize(body.innerHTML, {
    FORBID_TAGS,
    FORBID_ATTR: ["style", "class", "id", "xmlns"],
    ALLOW_DATA_ATTR: false,
    RETURN_DOM_FRAGMENT: true,
  });

  clean.querySelectorAll("img").forEach((img) => {
    const src = img.getAttribute("src");
    const url = src && !/^[a-z]+:/i.test(src) ? imageUrl(resolvePath(baseHref, src)) : undefined;
    if (url) img.setAttribute("src", url);
    else img.remove();
  });
  clean.querySelectorAll("a").forEach((a) => {
    const href = a.getAttribute("href") ?? "";
    a.removeAttribute("href");
    if (/^https?:\/\//i.test(href)) {
      a.setAttribute("href", href);
      a.setAttribute("target", "_blank");
      a.setAttribute("rel", "noopener noreferrer");
    } else if (href && !/^[a-z]+:/i.test(href)) {
      const i = href.startsWith("#") ? chapterIndexOf(baseHref) : chapterIndexOf(resolvePath(baseHref, href));
      if (i >= 0) a.setAttribute("data-chapter", String(i));
    }
  });

  const out = document.createElement("div");
  out.append(clean);
  return out.innerHTML;
}
