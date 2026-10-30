import mongoose from "mongoose";

const cache = globalThis as unknown as { mongoosePromise?: Promise<typeof mongoose> };

export function connectDB(): Promise<typeof mongoose> {
  const uri = process.env.MONGODB_URI;
  if (!uri) throw new Error("Thiếu biến môi trường MONGODB_URI");
  // Cache on globalThis so HMR and serverless warm starts reuse one connection.
  cache.mongoosePromise ??= mongoose.connect(uri).catch((err) => {
    cache.mongoosePromise = undefined;
    throw err;
  });
  return cache.mongoosePromise;
}
