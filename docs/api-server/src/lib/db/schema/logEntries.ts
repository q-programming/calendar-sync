import { pgTable, serial, text, timestamp } from "drizzle-orm/pg-core";
import { syncRunsTable } from "./syncRuns";

export const logEntriesTable = pgTable("log_entries", {
  id: serial("id").primaryKey(),
  runId: text("run_id").notNull().references(() => syncRunsTable.id),
  timestamp: timestamp("timestamp").notNull().defaultNow(),
  level: text("level", { enum: ["INFO", "DEBUG", "WARN", "ERROR"] }).notNull(),
  message: text("message").notNull(),
});

export type LogEntry = typeof logEntriesTable.$inferSelect;
