import { afterAll, afterEach, beforeAll } from "vitest";
import { MongoMemoryServer } from "mongodb-memory-server";
import mongoose from "mongoose";
import { connectDB } from "@/lib/db";

export function setupTestDB() {
  let server: MongoMemoryServer;
  beforeAll(async () => {
    server = await MongoMemoryServer.create();
    process.env.MONGODB_URI = server.getUri();
    await connectDB();
    await Promise.all(Object.values(mongoose.models).map((m) => m.syncIndexes()));
  });
  afterEach(async () => {
    await Promise.all(Object.values(mongoose.connection.collections).map((c) => c.deleteMany({})));
  });
  afterAll(async () => {
    await mongoose.disconnect();
    await server?.stop();
  });
}

export const link = (url = "https://archive.org/x") => [{ label: "Đọc", url }];
