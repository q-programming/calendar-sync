import { Router, type IRouter } from "express";
import { eq } from "drizzle-orm";
import { db, syncSettingsTable } from "../lib/db/index";
import { GetSettingsResponse, UpdateSettingsBody } from "../lib/api-zod/index";

const router: IRouter = Router();

async function getOrCreateSettings() {
  const rows = await db.select().from(syncSettingsTable);
  if (rows.length > 0) return rows[0];
  const [settings] = await db
    .insert(syncSettingsTable)
    .values({})
    .returning();
  return settings;
}

router.get("/settings", async (req, res): Promise<void> => {
  const settings = await getOrCreateSettings();
  res.json(
    GetSettingsResponse.parse({
      frequencyMinutes: settings.frequencyMinutes,
      daysPast: settings.daysPast,
      daysFuture: settings.daysFuture,
      debugLogging: settings.debugLogging,
    }),
  );
});

router.put("/settings", async (req, res): Promise<void> => {
  const parsed = UpdateSettingsBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const settings = await getOrCreateSettings();
  await db
    .update(syncSettingsTable)
    .set({
      frequencyMinutes: parsed.data.frequencyMinutes,
      daysPast: parsed.data.daysPast,
      daysFuture: parsed.data.daysFuture,
      debugLogging: parsed.data.debugLogging,
    })
    .where(eq(syncSettingsTable.id, settings.id));

  res.sendStatus(204);
});

export { getOrCreateSettings };
export default router;
