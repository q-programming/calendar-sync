import { describe, it, expect } from 'vitest';
import { formatLocal } from './dateUtils';

describe('formatLocal', () => {
    it('should format a UTC ISO string using the default format', () => {
        const result = formatLocal('2024-01-15T10:30:45Z');
        // Pattern: "Jan 15, 2024 HH:mm:ss" – exact hours depend on the browser timezone
        expect(result).toMatch(/Jan 15, 2024 \d{2}:\d{2}:\d{2}/);
    });

    it('should apply a custom format string', () => {
        const result = formatLocal('2024-06-20T14:05:09Z', 'HH:mm:ss');
        expect(result).toMatch(/^\d{2}:\d{2}:\d{2}$/);
    });

    it('should apply a verbose format', () => {
        const result = formatLocal('2024-03-01T08:00:00Z', "MMMM d, yyyy 'at' HH:mm:ss");
        expect(result).toMatch(/March 1, 2024 at \d{2}:\d{2}:\d{2}/);
    });

    it('should handle midnight UTC without throwing', () => {
        expect(() => formatLocal('2024-01-01T00:00:00Z')).not.toThrow();
    });

    it('should handle millisecond precision format', () => {
        const result = formatLocal('2024-01-15T10:00:01.123Z', 'HH:mm:ss.SSS');
        expect(result).toMatch(/\d{2}:\d{2}:\d{2}\.\d{3}/);
    });
});
