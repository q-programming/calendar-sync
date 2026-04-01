import { pgTable, serial, integer, boolean } from "drizzle-orm/pg-core";

export const syncSettingsTable = pgTable("sync_settings", {
  id: serial("id").primaryKey(),
  frequencyMinutes: integer("frequency_minutes").notNull().default(15),
  daysPast: integer("days_past").notNull().default(7),
  daysFuture: integer("days_future").notNull().default(30),
  debugLogging: boolean("debug_logging").notNull().default(false),
});

export type SyncSettings = typeof syncSettingsTable.$inferSelect;
