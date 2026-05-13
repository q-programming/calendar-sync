import { format } from 'date-fns';
import { toZonedTime } from 'date-fns-tz';

/** Format a UTC ISO timestamp into the browser's local timezone. */
export const formatLocal = (iso: string, fmt = 'MMM d, yyyy HH:mm:ss') => {
    const browserTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    return format(toZonedTime(new Date(iso), browserTz), fmt);
};
