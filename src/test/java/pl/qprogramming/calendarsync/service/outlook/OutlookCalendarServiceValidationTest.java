package pl.qprogramming.calendarsync.service.outlook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.qprogramming.calendarsync.model.DateRange;
import pl.qprogramming.calendarsync.service.LogService;

import java.io.File;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutlookCalendarService — path validation")
class OutlookCalendarServiceValidationTest {

    @Mock LogService logService;

    @InjectMocks OutlookCalendarService service;

    private static final DateRange RANGE = new DateRange(
            ZonedDateTime.now(ZoneOffset.UTC).minusDays(7),
            ZonedDateTime.now(ZoneOffset.UTC).plusDays(30));

    // ── listCalendars validation ───────────────────────────────────────────────

    @Nested
    @DisplayName("listCalendars — validateProfilePath")
    class ListCalendarsValidation {

        @ParameterizedTest(name = "throws IAE for blank path [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("throws IAE when path is null or blank")
        void throwsIAE_WhenNullOrBlankPath(String path) {
            assertThatThrownBy(() -> service.listCalendars(path))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not configured");
        }

        @Test
        @DisplayName("throws IAE when file does not exist")
        void throwsIAE_WhenFileNotExist() {
            assertThatThrownBy(() -> service.listCalendars("/no/such/path.pst"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not readable");
        }

        @Test
        @DisplayName("proceeds past validation for an existing readable file")
        void proceedsPastValidation_WhenFileReadable(@TempDir Path tempDir) throws Exception {
            File file = tempDir.resolve("test.pst").toFile();
            file.createNewFile();

            // The file exists, so validation passes; then it tries to parse as PST → exception
            // We verify the IAE (validation) is NOT thrown, instead a RuntimeException (parse failure)
            assertThatThrownBy(() -> service.listCalendars(file.getAbsolutePath()))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── readEvents validation ─────────────────────────────────────────────────

    @Nested
    @DisplayName("readEvents — validateProfilePath")
    class ReadEventsValidation {

        @ParameterizedTest(name = "throws IAE for blank path [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("throws IAE when path is null or blank")
        void throwsIAE_WhenNullOrBlankPath(String path) {
            assertThatThrownBy(() -> service.readEvents(path, "cal-id", RANGE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not configured");
        }

        @Test
        @DisplayName("throws IAE when file does not exist")
        void throwsIAE_WhenFileNotExist() {
            assertThatThrownBy(() -> service.readEvents("/no/such/file.ost", "cal-id", RANGE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not readable");
        }
    }
}
