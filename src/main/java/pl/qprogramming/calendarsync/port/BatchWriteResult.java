package pl.qprogramming.calendarsync.port;

public record BatchWriteResult(int created, int updated, int deleted, int failed) {
    public static BatchWriteResult empty() {
        return new BatchWriteResult(0, 0, 0, 0);
    }
}
