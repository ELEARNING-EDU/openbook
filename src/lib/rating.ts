export function computeRating(ratings: number[]): { ratingAvg: number; ratingCount: number } {
  if (ratings.length === 0) return { ratingAvg: 0, ratingCount: 0 };
  const sum = ratings.reduce((a, b) => a + b, 0);
  return { ratingAvg: Math.round((sum / ratings.length) * 10) / 10, ratingCount: ratings.length };
}
