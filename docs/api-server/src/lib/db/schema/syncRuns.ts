import { pgTable, text, integer, timestamp } from "drizzle-orm/pg-core";

export const syncRunsTable = pgTable("sync_runs", {
  id: text("id").primaryKey().$defaultFn(() => crypto.randomUUID()),
  startedAt: timestamp("started_at").notNull().defaultNow(),
  finishedAt: timestamp("finished_at"),
  status: text("status", { enum: ["SUCCESS", "FAILED"] }).notNull(),
  created: integer("created").notNull().default(0),
  updated: integer("updated").notNull().default(0),
  deleted: integer("deleted").notNull().default(0),
  message: text("message"),
});

export type SyncRun = typeof syncRunsTable.$inferSelect;
