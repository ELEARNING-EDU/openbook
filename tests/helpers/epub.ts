import { strToU8, zipSync } from "fflate";

type Opts = { chapters: { name: string; html: string; title?: string }[]; nav?: boolean; ncx?: boolean; images?: Record<string, Uint8Array> };

const xhtml = (body: string) =>
  `<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>c</title></head><body>${body}</body></html>`;

export function makeEpub({ chapters, nav, ncx, images = {} }: Opts): Uint8Array {
  const items = chapters.map((c, i) => `<item id="c${i}" href="Text/${c.name}" media-type="application/xhtml+xml"/>`);
  if (nav) items.push(`<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>`);
  if (ncx) items.push(`<item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>`);
  Object.keys(images).forEach((p, i) => items.push(`<item id="img${i}" href="${p}" media-type="image/png"/>`));
  const opf = `<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Sách thử</dc:title></metadata><manifest>${items.join("")}</manifest><spine${ncx ? ' toc="ncx"' : ""}>${chapters.map((_, i) => `<itemref idref="c${i}"/>`).join("")}</spine></package>`;
  const files: Record<string, Uint8Array> = {
    mimetype: strToU8("application/epub+zip"),
    "META-INF/container.xml": strToU8(
      `<?xml version="1.0"?><container xmlns="urn:oasis:names:tc:opendocument:xmlns:container" version="1.0"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>`,
    ),
    "OEBPS/content.opf": strToU8(opf),
  };
  chapters.forEach((c) => (files[`OEBPS/Text/${c.name}`] = strToU8(xhtml(c.html))));
  for (const [p, data] of Object.entries(images)) files[`OEBPS/${p}`] = data;
  const label = (c: Opts["chapters"][number], i: number) => c.title ?? `Mục ${i + 1}`;
  if (nav)
    files["OEBPS/nav.xhtml"] = strToU8(
      xhtml(`<nav xmlns:epub="http://www.idpf.org/2007/ops" epub:type="toc"><ol>${chapters.map((c, i) => `<li><a href="Text/${c.name}">${label(c, i)}</a></li>`).join("")}</ol></nav>`),
    );
  if (ncx)
    files["OEBPS/toc.ncx"] = strToU8(
      `<?xml version="1.0"?><ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1"><navMap>${chapters.map((c, i) => `<navPoint id="n${i}"><navLabel><text>${label(c, i)}</text></navLabel><content src="Text/${c.name}"/></navPoint>`).join("")}</navMap></ncx>`,
    );
  return zipSync(files);
}
