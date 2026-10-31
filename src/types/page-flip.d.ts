// page-flip ships TS sources but no declarations for its built module.
declare module "page-flip/dist/js/page-flip.module.js" {
  export class PageFlip {
    constructor(el: HTMLElement, settings: Record<string, unknown>);
    loadFromHTML(items: HTMLElement[]): void;
    on(event: "flip" | "changeState" | "init", cb: (e: { data: unknown }) => void): this;
    flipNext(): void;
    flipPrev(): void;
    turnToPage(page: number): void;
    getCurrentPageIndex(): number;
    destroy(): void;
  }
}
