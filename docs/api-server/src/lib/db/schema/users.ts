import { pgTable, text, boolean, serial } from "drizzle-orm/pg-core";

export const usersTable = pgTable("users", {
  id: serial("id").primaryKey(),
  googleAccessToken: text("google_access_token"),
  googleRefreshToken: text("google_refresh_token"),
  googleConnected: boolean("google_connected").notNull().default(false),
  outlookConnected: boolean("outlook_connected").notNull().default(false),
  outlookProfilePath: text("outlook_profile_path"),
  outlookCalendarId: text("outlook_calendar_id"),
  outlookCalendarName: text("outlook_calendar_name"),
  googleCalendarId: text("google_calendar_id"),
  googleCalendarName: text("google_calendar_name"),
});

export type User = typeof usersTable.$inferSelect;
