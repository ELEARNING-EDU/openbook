import { strFromU8, unzipSync } from "fflate";

export type TocItem = { label: string; chapter: number; children: TocItem[] };

export type EpubBook = {
  title: string;
  chapters: { href: string }[];
  toc: TocItem[];
  chapterHtml(i: number): string;
  resource(path: string): Uint8Array | undefined;
  chapterIndexOf(href: string): number;
};

const invalid = () => new Error("EPUB_INVALID");
const OPS_NS = "http://www.idpf.org/2007/ops";

/** Resolves `relative` against the zip path of `baseHref`; drops the #fragment and URL-decodes. */
export function resolvePath(baseHref: string, relative: string): string {
  const parts = baseHref.split("/").slice(0, -1);
  for (const seg of decodeURIComponent(relative.split("#")[0]).split("/")) {
    if (seg === "..") parts.pop();
    else if (seg && seg !== ".") parts.push(seg);
  }
  return parts.join("/");
}

const xml = (text: string) => {
  const doc = new DOMParser().parseFromString(text, "application/xml");
  if (doc.getElementsByTagName("parsererror").length) throw invalid();
  return doc;
};
const tags = (root: Document | Element, name: string) => Array.from(root.getElementsByTagNameNS("*", name));
const childTags = (el: Element, name: string) => Array.from(el.children).filter((c) => c.localName === name);

export function openEpub(data: ArrayBuffer | Uint8Array): EpubBook {
  let files: Record<string, Uint8Array>;
  try {
    files = unzipSync(data instanceof Uint8Array ? data : new Uint8Array(data));
  } catch {
    throw invalid();
  }
  const text = (path: string) => {
    const f = files[path];
    if (!f) throw invalid();
    return strFromU8(f);
  };

  const opfPath = tags(xml(text("META-INF/container.xml")), "rootfile")[0]?.getAttribute("full-path");
  if (!opfPath) throw invalid();
  const opf = xml(text(opfPath));

  const manifest = new Map(
    tags(opf, "item").map((i) => [
      i.getAttribute("id") ?? "",
      { href: resolvePath(opfPath, i.getAttribute("href") ?? ""), props: i.getAttribute("properties") ?? "", type: i.getAttribute("media-type") ?? "" },
    ]),
  );
  const chapters = tags(opf, "itemref")
    .map((r) => manifest.get(r.getAttribute("idref") ?? "")?.href)
    .filter((h): h is string => !!h && h in files)
    .map((href) => ({ href }));
  if (chapters.length === 0) throw invalid();

  const chapterIndexOf = (href: string) => chapters.findIndex((c) => c.href === href.split("#")[0]);
  const entry = (label: string, base: string, href: string, children: TocItem[]): TocItem => ({
    label: label.trim().replace(/\s+/g, " "),
    chapter: chapterIndexOf(resolvePath(base, href)),
    children,
  });

  const fromNav = (navPath: string): TocItem[] => {
    const doc = new DOMParser().parseFromString(text(navPath), "application/xhtml+xml");
    const nav = tags(doc, "nav").find((n) => (n.getAttributeNS(OPS_NS, "type") ?? n.getAttribute("epub:type")) === "toc");
    const walk = (ol: Element | undefined): TocItem[] =>
      ol
        ? childTags(ol, "li").flatMap((li) => {
            const a = childTags(li, "a")[0] ?? childTags(li, "span")[0];
            return a ? [entry(a.textContent ?? "", navPath, a.getAttribute("href") ?? "", walk(childTags(li, "ol")[0]))] : [];
          })
        : [];
    return nav ? walk(childTags(nav, "ol")[0]) : [];
  };
  const fromNcx = (ncxPath: string): TocItem[] => {
    const walk = (parent: Element): TocItem[] =>
      childTags(parent, "navPoint").map((p) =>
        entry(tags(p, "text")[0]?.textContent ?? "", ncxPath, tags(p, "content")[0]?.getAttribute("src") ?? "", walk(p)),
      );
    const map = tags(xml(text(ncxPath)), "navMap")[0];
    return map ? walk(map) : [];
  };

  const items = [...manifest.values()];
  const navItem = items.find((i) => i.props.split(/\s+/).includes("nav"));
  const ncxItem = items.find((i) => i.type === "application/x-dtbncx+xml");
  let toc: TocItem[] = [];
  try {
    toc = navItem ? fromNav(navItem.href) : ncxItem ? fromNcx(ncxItem.href) : [];
  } catch {
    toc = [];
  }
  const prune = (list: TocItem[]): TocItem[] =>
    list.filter((t) => t.chapter >= 0 || t.children.length).map((t) => ({ ...t, children: prune(t.children) }));
  toc = prune(toc);
  if (toc.length === 0) toc = chapters.map((_, i) => ({ label: `Chương ${i + 1}`, chapter: i, children: [] }));

  return {
    title: tags(opf, "title")[0]?.textContent?.trim() ?? "",
    chapters,
    toc,
    chapterHtml: (i) => text(chapters[i].href),
    resource: (path) => files[path],
    chapterIndexOf,
  };
}
