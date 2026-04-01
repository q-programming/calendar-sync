import { Router, type IRouter } from "express";
import { eq } from "drizzle-orm";
import { db, usersTable } from "../lib/db/index";
import {
  GetOutlookCalendarsResponse,
  SetOutlookCalendarBody,
  ConnectOutlookBody,
} from "../lib/api-zod/index";
import { getOrCreateUser } from "./profile";

const router: IRouter = Router();

const MOCK_OUTLOOK_CALENDARS = [
  { id: "outlook-cal-1", name: "Calendar", timeZone: "America/New_York", color: "#0078D4" },
  { id: "outlook-cal-2", name: "Work", timeZone: "America/New_York", color: "#00A4EF" },
  { id: "outlook-cal-3", name: "Personal", timeZone: "America/New_York", color: "#7FBA00" },
];

router.post("/profile/outlook/connect", async (req, res): Promise<void> => {
  const parsed = ConnectOutlookBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const user = await getOrCreateUser();
  await db
    .update(usersTable)
    .set({
      outlookConnected: true,
      outlookProfilePath: parsed.data.profilePath,
    })
    .where(eq(usersTable.id, user.id));

  req.log.info({ profilePath: parsed.data.profilePath }, "Outlook connected via profile path");
  res.sendStatus(204);
});

router.get("/profile/outlook/calendars", async (req, res): Promise<void> => {
  res.json(GetOutlookCalendarsResponse.parse(MOCK_OUTLOOK_CALENDARS));
});

router.put("/profile/outlook/calendar", async (req, res): Promise<void> => {
  const parsed = SetOutlookCalendarBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const user = await getOrCreateUser();
  const calendar = MOCK_OUTLOOK_CALENDARS.find((c) => c.id === parsed.data.calendarId);
  const calendarName = calendar ? calendar.name : parsed.data.calendarId;

  await db
    .update(usersTable)
    .set({
      outlookCalendarId: parsed.data.calendarId,
      outlookCalendarName: calendarName,
    })
    .where(eq(usersTable.id, user.id));

  res.sendStatus(204);
});

export default router;
