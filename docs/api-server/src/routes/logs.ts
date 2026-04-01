import { Router, type IRouter } from "express";
import { desc, eq, count, and, SQL } from "drizzle-orm";
import { db, syncRunsTable, logEntriesTable } from "../lib/db/index";
import {
  GetLogsResponse,
  GetLogsQueryParams,
  GetLogDetailsParams,
  GetLogDetailsResponse,
} from "../lib/api-zod/index";

const router: IRouter = Router();

router.get("/logs", async (req, res): Promise<void> => {
  const params = GetLogsQueryParams.safeParse(req.query);
  const page = params.success ? (params.data.page ?? 0) : 0;
  const size = params.success ? (params.data.size ?? 20) : 20;
  const statusFilter = params.success ? params.data.status : undefined;

  const conditions: SQL[] = [];
  if (statusFilter) {
    conditions.push(eq(syncRunsTable.status, statusFilter));
  }

  const whereClause = conditions.length > 0 ? and(...conditions) : undefined;

  const [totalResult] = await db
    .select({ count: count() })
    .from(syncRunsTable)
    .where(whereClause);

  const totalElements = totalResult?.count ?? 0;
  const totalPages = Math.ceil(totalElements / size);

  const runs = await db
    .select()
    .from(syncRunsTable)
    .where(whereClause)
    .orderBy(desc(syncRunsTable.startedAt))
    .limit(size)
    .offset(page * size);

  res.json(
    GetLogsResponse.parse({
      content: runs.map((run) => ({
        id: run.id,
        startedAt: run.startedAt.toISOString(),
        finishedAt: run.finishedAt?.toISOString(),
        status: run.status,
        created: run.created,
        updated: run.updated,
        deleted: run.deleted,
        message: run.message,
      })),
      page,
      size,
      totalElements,
      totalPages,
    }),
  );
});

router.get("/logs/:runId", async (req, res): Promise<void> => {
  const params = GetLogDetailsParams.safeParse(req.params);
  if (!params.success) {
    res.status(400).json({ error: params.error.message });
    return;
  }

  const runId = Array.isArray(params.data.runId) ? params.data.runId[0] : params.data.runId;

  const [run] = await db
    .select()
    .from(syncRunsTable)
    .where(eq(syncRunsTable.id, runId));

  if (!run) {
    res.status(404).json({ error: "Sync run not found" });
    return;
  }

  const entries = await db
    .select()
    .from(logEntriesTable)
    .where(eq(logEntriesTable.runId, runId))
    .orderBy(logEntriesTable.timestamp);

  res.json(
    GetLogDetailsResponse.parse({
      run: {
        id: run.id,
        startedAt: run.startedAt.toISOString(),
        finishedAt: run.finishedAt?.toISOString(),
        status: run.status,
        created: run.created,
        updated: run.updated,
        deleted: run.deleted,
        message: run.message,
      },
      entries: entries.map((e) => ({
        timestamp: e.timestamp.toISOString(),
        level: e.level,
        message: e.message,
      })),
    }),
  );
});

export default router;
