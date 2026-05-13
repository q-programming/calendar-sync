package pl.qprogramming.calendarsync.service.google;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BatchWriteResult")
class BatchWriteResultTest {

    @Test
    @DisplayName("empty() factory returns all-zero result")
    void empty_returnsAllZero() {
        var result = BatchWriteResult.empty();

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isZero();
        assertThat(result.deleted()).isZero();
        assertThat(result.failed()).isZero();
    }

    @Test
    @DisplayName("constructor stores all fields")
    void constructor_storesAllFields() {
        var result = new BatchWriteResult(3, 2, 1, 0);

        assertThat(result.created()).isEqualTo(3);
        assertThat(result.updated()).isEqualTo(2);
        assertThat(result.deleted()).isEqualTo(1);
        assertThat(result.failed()).isZero();
    }

    @Test
    @DisplayName("records with same values are equal")
    void recordEquality() {
        assertThat(new BatchWriteResult(1, 2, 3, 0))
                .isEqualTo(new BatchWriteResult(1, 2, 3, 0));
    }

    @Test
    @DisplayName("empty() is equal to zero BatchWriteResult")
    void emptyEquality() {
        assertThat(BatchWriteResult.empty()).isEqualTo(new BatchWriteResult(0, 0, 0, 0));
    }
}
