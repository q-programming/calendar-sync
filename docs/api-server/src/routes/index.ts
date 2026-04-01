import { Router, type IRouter } from "express";
import healthRouter from "./health";
import authRouter from "./auth";
import profileRouter from "./profile";
import outlookRouter from "./outlook";
import googleRouter from "./google";
import settingsRouter from "./settings";
import syncRouter from "./sync";
import logsRouter from "./logs";

const router: IRouter = Router();

router.use(healthRouter);
router.use(authRouter);
router.use(profileRouter);
router.use(outlookRouter);
router.use(googleRouter);
router.use(settingsRouter);
router.use(syncRouter);
router.use(logsRouter);

export default router;
