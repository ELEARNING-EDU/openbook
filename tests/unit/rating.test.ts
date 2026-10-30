import { describe, expect, it } from "vitest";
import { computeRating } from "@/lib/rating";

describe("computeRating", () => {
  it("rỗng → 0", () => {
    expect(computeRating([])).toEqual({ ratingAvg: 0, ratingCount: 0 });
  });
  it("trung bình làm tròn 1 chữ số", () => {
    expect(computeRating([5, 4, 4])).toEqual({ ratingAvg: 4.3, ratingCount: 3 });
  });
});
