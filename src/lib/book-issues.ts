// One place for "needs attention" rules: the overview and the books page issue tab share these.

export const BOOK_ISSUE_FILTER = [
  { coverUrl: "" },
  { epubUrl: "", links: { $size: 0 } },
  { categoryIds: { $size: 0 } },
] as const;

export type BookIssue = "admin.issueCover" | "admin.issueNoFile" | "admin.issueCategory";

type IssueFields = {
  coverUrl?: string | null;
  epubUrl?: string | null;
  links?: unknown[] | null;
  categoryIds?: unknown[] | null;
};

export function bookIssues(book: IssueFields): BookIssue[] {
  const issues: BookIssue[] = [];
  if (!book.coverUrl) issues.push("admin.issueCover");
  if (!book.epubUrl && (book.links?.length ?? 0) === 0) issues.push("admin.issueNoFile");
  if ((book.categoryIds?.length ?? 0) === 0) issues.push("admin.issueCategory");
  return issues;
}
