import { describe, it, expect } from 'vitest';
import { getStatusChipSx, getLevelColor, getLevelBg } from './colorUtils';
import { LogLevel, SyncRunStatus } from '@api';

describe('getStatusChipSx', () => {
    it('should return white label style for Success', () => {
        expect(getStatusChipSx(SyncRunStatus.Success)).toEqual({
            color: '#fff',
            '& .MuiChip-label': { color: '#fff' },
        });
    });

    it('should return white label style for Running', () => {
        expect(getStatusChipSx(SyncRunStatus.Running)).toEqual({
            color: '#fff',
            '& .MuiChip-label': { color: '#fff' },
        });
    });

    it('should return undefined for Failed', () => {
        expect(getStatusChipSx(SyncRunStatus.Failed)).toBeUndefined();
    });
});

describe('getLevelColor', () => {
    it('should return error for Error level', () => {
        expect(getLevelColor(LogLevel.Error)).toBe('error');
    });

    it('should return warning for Warn level', () => {
        expect(getLevelColor(LogLevel.Warn)).toBe('warning');
    });

    it('should return default for Debug level', () => {
        expect(getLevelColor(LogLevel.Debug)).toBe('default');
    });

    it('should return info for Info level', () => {
        expect(getLevelColor(LogLevel.Info)).toBe('info');
    });
});

describe('getLevelBg', () => {
    it('should return red tones for Error', () => {
        const bg = getLevelBg(LogLevel.Error);
        expect(bg.bgcolor).toBe('#fef2f2');
        expect(bg.color).toBe('#dc2626');
        expect(bg.borderColor).toBe('#fecaca');
    });

    it('should return amber tones for Warn', () => {
        const bg = getLevelBg(LogLevel.Warn);
        expect(bg.bgcolor).toBe('#fffbeb');
        expect(bg.color).toBe('#d97706');
        expect(bg.borderColor).toBe('#fde68a');
    });

    it('should return grey tones for Debug', () => {
        const bg = getLevelBg(LogLevel.Debug);
        expect(bg.bgcolor).toBe('#f9fafb');
        expect(bg.color).toBe('#6b7280');
        expect(bg.borderColor).toBe('#e5e7eb');
    });

    it('should return blue tones for Info', () => {
        const bg = getLevelBg(LogLevel.Info);
        expect(bg.bgcolor).toBe('#eff6ff');
        expect(bg.color).toBe('#2563eb');
        expect(bg.borderColor).toBe('#bfdbfe');
    });
});
