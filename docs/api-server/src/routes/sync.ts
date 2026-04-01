import { Router, type IRouter } from "express";
import { db, syncRunsTable, logEntriesTable } from "../lib/db/index";
import { TriggerSyncResponse } from "../lib/api-zod/index";

const router: IRouter = Router();

router.post("/sync/run", async (req, res): Promise<void> => {
  const startedAt = new Date();

  const created = Math.floor(Math.random() * 10);
  const updated = Math.floor(Math.random() * 5);
  const deleted = Math.floor(Math.random() * 3);
  const success = Math.random() > 0.2;

  const finishedAt = new Date(startedAt.getTime() + Math.floor(Math.random() * 15000) + 2000);

  const [run] = await db
    .insert(syncRunsTable)
    .values({
      startedAt,
      finishedAt,
      status: success ? "SUCCESS" : "FAILED",
      created,
      updated,
      deleted,
      message: success
        ? `Synced ${created} created, ${updated} updated, ${deleted} deleted`
        : "Sync failed: simulated error",
    })
    .returning();

  const logEntries = [];
  const baseTime = startedAt.getTime();

  logEntries.push({
    runId: run.id,
    timestamp: new Date(baseTime),
    level: "INFO" as const,
    message: "Starting calendar sync...",
  });

  logEntries.push({
    runId: run.id,
    timestamp: new Date(baseTime + 500),
    level: "INFO" as const,
    message: "Connecting to Microsoft Graph API",
  });

  logEntries.push({
    runId: run.id,
    timestamp: new Date(baseTime + 1200),
    level: "INFO" as const,
    message: `Fetched ${created + updated + deleted + Math.floor(Math.random() * 5)} events from Outlook`,
  });

  logEntries.push({
    runId: run.id,
    timestamp: new Date(baseTime + 2000),
    level: "INFO" as const,
    message: "Connecting to Google Calendar API",
  });

  if (created > 0) {
    logEntries.push({
      runId: run.id,
      timestamp: new Date(baseTime + 3000),
      level: "INFO" as const,
      message: `Creating ${created} new event(s) in Google Calendar`,
    });
  }

  if (updated > 0) {
    logEntries.push({
      runId: run.id,
      timestamp: new Date(baseTime + 4500),
      level: "INFO" as const,
      message: `Updating ${updated} event(s) in Google Calendar`,
    });
  }

  if (deleted > 0) {
    logEntries.push({
      runId: run.id,
      timestamp: new Date(baseTime + 6000),
      level: "WARN" as const,
      message: `Deleting ${deleted} event(s) removed from Outlook`,
    });
  }

  if (!success) {
    logEntries.push({
      runId: run.id,
      timestamp: new Date(baseTime + 7000),
      level: "ERROR" as const,
      message: "Failed to write to Google Calendar: connection timeout after 10s",
    });
    logEntries.push({
      runId: run.id,
      timestamp: new Date(baseTime + 7500),
      level: "ERROR" as const,
      message: "Sync aborted due to error. No changes were committed.",
    });
  } else {
    logEntries.push({
      runId: run.id,
      timestamp: new Date(finishedAt.getTime() - 200),
      level: "INFO" as const,
      message: `Sync completed successfully: ${created} created, ${updated} updated, ${deleted} deleted`,
    });
  }

  await db.insert(logEntriesTable).values(logEntries);

  res.json(
    TriggerSyncResponse.parse({
      id: run.id,
      startedAt: run.startedAt.toISOString(),
      finishedAt: run.finishedAt?.toISOString(),
      status: run.status,
      created: run.created,
      updated: run.updated,
      deleted: run.deleted,
      message: run.message,
    }),
  );
});

export default router;
