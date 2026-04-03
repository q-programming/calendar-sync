package pl.qprogramming.calendarsync.service.google;

/**
 * Aggregated result of a {@link GoogleCalendarService#batchWrite} call.
 *
 * @param created number of new Google events successfully inserted
 * @param updated number of existing Google events successfully updated
 * @param deleted number of Google events successfully deleted
 * @param failed  number of operations that the Google API rejected
 */
public record BatchWriteResult(int created, int updated, int deleted, int failed) {
    /** Convenience factory for a no-op result when there is nothing to write. */
    public static BatchWriteResult empty() {
        return new BatchWriteResult(0, 0, 0, 0);
    }
}

