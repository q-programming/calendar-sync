import { Router, type IRouter } from "express";
import { eq } from "drizzle-orm";
import { db, usersTable } from "../lib/db/index";
import { GetProfileResponse } from "../lib/api-zod/index";

const router: IRouter = Router();

async function getOrCreateUser() {
  const users = await db.select().from(usersTable);
  if (users.length > 0) return users[0];
  const [user] = await db.insert(usersTable).values({}).returning();
  return user;
}

router.get("/profile", async (req, res): Promise<void> => {
  const user = await getOrCreateUser();
  const profileData: Record<string, unknown> = {
    googleConnected: user.googleConnected,
    outlookConnected: user.outlookConnected,
  };
  if (user.outlookProfilePath) profileData.outlookProfilePath = user.outlookProfilePath;
  if (user.outlookCalendarId) profileData.outlookCalendarId = user.outlookCalendarId;
  if (user.outlookCalendarName) profileData.outlookCalendarName = user.outlookCalendarName;
  if (user.googleCalendarId) profileData.googleCalendarId = user.googleCalendarId;
  if (user.googleCalendarName) profileData.googleCalendarName = user.googleCalendarName;

  res.json(GetProfileResponse.parse(profileData));
});

export { getOrCreateUser };
export default router;
