import {SyncRunStatus} from '@api';

export const getStatusChipSx = (status: SyncRunStatus) =>
    status === SyncRunStatus.Success || status === SyncRunStatus.Running
        ? {color: '#fff', '& .MuiChip-label': {color: '#fff'}}
        : undefined;

export const getLevelColor = (level: string): 'error' | 'warning' | 'info' | 'default' => {
    if (level === 'ERROR') return 'error';
    if (level === 'WARN') return 'warning';
    if (level === 'DEBUG') return 'default';
    return 'info';
};


export const getLevelBg = (level: string) => ({
    ERROR: {bgcolor: '#fef2f2', borderColor: '#fecaca', color: '#dc2626'},
    WARN: {bgcolor: '#fffbeb', borderColor: '#fde68a', color: '#d97706'},
    DEBUG: {bgcolor: '#f9fafb', borderColor: '#e5e7eb', color: '#6b7280'},
    INFO: {bgcolor: '#eff6ff', borderColor: '#bfdbfe', color: '#2563eb'},
}[level] ?? {bgcolor: '#eff6ff', borderColor: '#bfdbfe', color: '#2563eb'});
