import { LogLevel, SyncRunStatus } from '@api';

export const getStatusChipSx = (status: SyncRunStatus) =>
    status === SyncRunStatus.Success || status === SyncRunStatus.Running
        ? { color: '#fff', '& .MuiChip-label': { color: '#fff' } }
        : undefined;

export const getLevelColor = (level: LogLevel): 'error' | 'warning' | 'info' | 'default' => {
    if (level === LogLevel.Error) return 'error';
    if (level === LogLevel.Warn) return 'warning';
    if (level === LogLevel.Debug) return 'default';
    return 'info';
};

export const getLevelBg = (level: LogLevel) =>
    ({
        [LogLevel.Error]: { bgcolor: '#fef2f2', borderColor: '#fecaca', color: '#dc2626' },
        [LogLevel.Warn]: { bgcolor: '#fffbeb', borderColor: '#fde68a', color: '#d97706' },
        [LogLevel.Debug]: { bgcolor: '#f9fafb', borderColor: '#e5e7eb', color: '#6b7280' },
        [LogLevel.Info]: { bgcolor: '#eff6ff', borderColor: '#bfdbfe', color: '#2563eb' },
    })[level] ?? { bgcolor: '#eff6ff', borderColor: '#bfdbfe', color: '#2563eb' };
