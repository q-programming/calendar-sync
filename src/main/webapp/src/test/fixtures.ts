import type {
    CalendarRef,
    LogEntry,
    PagedSyncRuns,
    Profile,
    SyncRun,
    SyncRunDetails,
    SyncSettings,
} from '@api';
import { LogLevel, SyncRunStatus } from '@api';

export const makeProfile = (overrides: Partial<Profile> = {}): Profile => ({
    googleConnected: false,
    outlookConnected: false,
    syncRunning: false,
    ...overrides,
});

export const makeSyncRun = (overrides: Partial<SyncRun> = {}): SyncRun => ({
    id: 'run-1',
    startedAt: '2024-01-15T10:00:00Z',
    status: SyncRunStatus.Success,
    created: 5,
    updated: 3,
    deleted: 1,
    message: 'Sync completed successfully',
    ...overrides,
});

export const makePagedSyncRuns = (
    runs: SyncRun[] = [makeSyncRun()],
    overrides: Partial<PagedSyncRuns> = {},
): PagedSyncRuns => ({
    content: runs,
    page: 0,
    size: 20,
    totalElements: runs.length,
    totalPages: Math.ceil(runs.length / 20) || 1,
    ...overrides,
});

export const makeSettings = (overrides: Partial<SyncSettings> = {}): SyncSettings => ({
    frequencyMinutes: 15,
    daysPast: 7,
    daysFuture: 30,
    debugLogging: false,
    syncColorLabels: true,
    ...overrides,
});

export const makeCalendarRef = (overrides: Partial<CalendarRef> = {}): CalendarRef => ({
    id: 'cal-1',
    name: 'My Calendar',
    ...overrides,
});

export const makeLogEntry = (overrides: Partial<LogEntry> = {}): LogEntry => ({
    timestamp: '2024-01-15T10:00:01Z',
    level: LogLevel.Info,
    message: 'Test log entry',
    ...overrides,
});

export const makeSyncRunDetails = (overrides: Partial<SyncRunDetails> = {}): SyncRunDetails => ({
    run: makeSyncRun(),
    entries: [
        makeLogEntry({ level: LogLevel.Info, message: 'Starting sync' }),
        makeLogEntry({ level: LogLevel.Info, message: 'Sync complete' }),
    ],
    ...overrides,
});
