import { Router, type IRouter } from "express";
import { eq } from "drizzle-orm";
import { db, usersTable } from "../lib/db/index";
import { getOrCreateUser } from "./profile";

const router: IRouter = Router();

router.get("/auth/login", async (req, res): Promise<void> => {
  const user = await getOrCreateUser();
  await db
    .update(usersTable)
    .set({ googleConnected: true })
    .where(eq(usersTable.id, user.id));

  req.log.info("Google OAuth simulated - user connected");
  res.redirect("/");
});

export default router;
