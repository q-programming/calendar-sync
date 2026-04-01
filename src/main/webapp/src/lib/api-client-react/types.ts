export interface Profile {
  googleConnected: boolean;
  outlookConnected: boolean;
  outlookProfilePath?: string;
  outlookCalendarId?: string;
  outlookCalendarName?: string;
  googleCalendarId?: string;
  googleCalendarName?: string;
}

export interface OutlookConnection {
  profilePath: string;
}

export interface CalendarRef {
  id: string;
  name: string;
  timeZone?: string;
  color?: string;
}

export interface CalendarSelection {
  calendarId: string;
}

export interface SyncSettings {
  frequencyMinutes: number;
  daysPast: number;
  daysFuture: number;
  debugLogging: boolean;
}

export type SyncRunStatus = (typeof SyncRunStatus)[keyof typeof SyncRunStatus];
export const SyncRunStatus = {
  SUCCESS: 'SUCCESS' as const,
  FAILED: 'FAILED' as const,
};

export interface SyncRun {
  id: string;
  startedAt: string;
  finishedAt?: string;
  status: SyncRunStatus;
  created?: number;
  updated?: number;
  deleted?: number;
  message?: string;
}

export interface PagedSyncRuns {
  content: SyncRun[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type LogEntryLevel = (typeof LogEntryLevel)[keyof typeof LogEntryLevel];
export const LogEntryLevel = {
  INFO: 'INFO' as const,
  DEBUG: 'DEBUG' as const,
  WARN: 'WARN' as const,
  ERROR: 'ERROR' as const,
};

export interface LogEntry {
  timestamp: string;
  level: LogEntryLevel;
  message: string;
}

export interface SyncRunDetails {
  run: SyncRun;
  entries: LogEntry[];
}

export type GetLogsStatus = (typeof GetLogsStatus)[keyof typeof GetLogsStatus];
export const GetLogsStatus = {
  SUCCESS: 'SUCCESS' as const,
  FAILED: 'FAILED' as const,
};

export interface GetLogsParams {
  page?: number;
  size?: number;
  status?: GetLogsStatus;
}
