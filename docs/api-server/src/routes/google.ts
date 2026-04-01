import { Router, type IRouter } from "express";
import { eq } from "drizzle-orm";
import { db, usersTable } from "../lib/db/index";
import {
  GetGoogleCalendarsResponse,
  SetGoogleCalendarBody,
} from "../lib/api-zod/index";
import { getOrCreateUser } from "./profile";

const router: IRouter = Router();

const MOCK_GOOGLE_CALENDARS = [
  { id: "google-cal-primary", name: "Primary", timeZone: "America/New_York", color: "#4285F4" },
  { id: "google-cal-work", name: "Work Calendar", timeZone: "America/New_York", color: "#0B8043" },
  { id: "google-cal-personal", name: "Personal", timeZone: "America/New_York", color: "#E67C73" },
];

router.get("/profile/google/calendars", async (req, res): Promise<void> => {
  const user = await getOrCreateUser();
  if (!user.googleConnected) {
    res.status(401).json({ error: "Google not connected" });
    return;
  }
  res.json(GetGoogleCalendarsResponse.parse(MOCK_GOOGLE_CALENDARS));
});

router.put("/profile/google/calendar", async (req, res): Promise<void> => {
  const parsed = SetGoogleCalendarBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const user = await getOrCreateUser();
  if (!user.googleConnected) {
    res.status(401).json({ error: "Google not connected" });
    return;
  }

  const calendar = MOCK_GOOGLE_CALENDARS.find((c) => c.id === parsed.data.calendarId);
  const calendarName = calendar ? calendar.name : parsed.data.calendarId;

  await db
    .update(usersTable)
    .set({
      googleCalendarId: parsed.data.calendarId,
      googleCalendarName: calendarName,
    })
    .where(eq(usersTable.id, user.id));

  res.sendStatus(204);
});

export default router;
