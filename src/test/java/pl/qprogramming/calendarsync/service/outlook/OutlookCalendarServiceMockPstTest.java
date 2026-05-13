package pl.qprogramming.calendarsync.service.outlook;

import com.pff.PSTAppointment;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTTimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pl.qprogramming.calendarsync.model.CalendarRef;
import pl.qprogramming.calendarsync.model.DateRange;
import pl.qprogramming.calendarsync.service.LogService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests OutlookCalendarService using {@code mockConstruction} to intercept
 * {@code new PSTFile(path)} so the reading logic can be exercised without a real
 * PST/OST file on disk. A temporary {@code .pst} file satisfies the
 * {@code validateProfilePath} existence check.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OutlookCalendarService — mock PST construction")
class OutlookCalendarServiceMockPstTest {

    @Mock private LogService logService;

    @InjectMocks
    private OutlookCalendarService service;

    // ── listCalendars ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listCalendars")
    class ListCalendarsTests {

        @Test
        @DisplayName("returns empty list when root folder has no calendar subfolders")
        void noCalendarFolder_returnsEmpty() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDisplayName()).thenReturn("Root");
                when(rootFolder.getContainerClass()).thenReturn("IPF.Note");
                when(rootFolder.hasSubfolders()).thenReturn(false);

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    List<CalendarRef> result = service.listCalendars(tempFile.toAbsolutePath().toString());
                    assertThat(result).isEmpty();
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("discovers calendar folder by container class IPF.Appointment")
        void calendarFolderByContainerClass_discovered() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder calFolder = mock(PSTFolder.class);
                when(calFolder.getDisplayName()).thenReturn("My Calendar");
                when(calFolder.getContainerClass()).thenReturn("IPF.Appointment");
                when(calFolder.getDescriptorNodeId()).thenReturn(456L);
                when(calFolder.hasSubfolders()).thenReturn(false);

                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDisplayName()).thenReturn("Root");
                when(rootFolder.getContainerClass()).thenReturn("IPF.Root");
                when(rootFolder.hasSubfolders()).thenReturn(true);
                when(rootFolder.getSubFolders()).thenReturn(new Vector<>(List.of(calFolder)));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    List<CalendarRef> result = service.listCalendars(tempFile.toAbsolutePath().toString());
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).name()).isEqualTo("My Calendar");
                    assertThat(result.get(0).id()).isEqualTo("456");
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("discovers calendar folder by display name 'Calendar'")
        void calendarFolderByDisplayName_discovered() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder calFolder = mock(PSTFolder.class);
                when(calFolder.getDisplayName()).thenReturn("Calendar");
                when(calFolder.getContainerClass()).thenReturn("");
                when(calFolder.getDescriptorNodeId()).thenReturn(789L);
                when(calFolder.hasSubfolders()).thenReturn(false);

                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDisplayName()).thenReturn("Root");
                when(rootFolder.getContainerClass()).thenReturn("");
                when(rootFolder.hasSubfolders()).thenReturn(true);
                when(rootFolder.getSubFolders()).thenReturn(new Vector<>(List.of(calFolder)));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    List<CalendarRef> result = service.listCalendars(tempFile.toAbsolutePath().toString());
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).name()).isEqualTo("Calendar");
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("wraps IOException from getRootFolder as RuntimeException")
        void getRootFolderThrowsIOException_wrapsAsRuntimeException() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                    (pstFile, ctx) -> when(pstFile.getRootFolder()).thenThrow(new java.io.IOException("disk error")))) {
                org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.listCalendars(tempFile.toAbsolutePath().toString()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to read Outlook file");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("skips folder when getDisplayName throws (collectCalendarFolders catch)")
        void corruptFolderDisplayName_skipped() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDisplayName()).thenThrow(new RuntimeException("corrupt node"));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {
                    List<CalendarRef> result = service.listCalendars(tempFile.toAbsolutePath().toString());
                    assertThat(result).isEmpty();
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    // ── readEvents ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("readEvents")
    class ReadEventsTests {

        private final DateRange testRange = new DateRange(
                ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2025, 6, 30, 23, 59, 59, 0, ZoneId.of("UTC")));

        @Test
        @DisplayName("returns empty list when calendar folder not found")
        void calendarNotFound_returnsEmpty() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDescriptorNodeId()).thenReturn(1L);
                when(rootFolder.hasSubfolders()).thenReturn(false);

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    // "999" does not match root (1) or any subfolder
                    List<OutlookEvent> result = service.readEvents(
                            tempFile.toAbsolutePath().toString(), "999", testRange);
                    assertThat(result).isEmpty();
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("returns empty list when calendar folder has no items")
        void emptyCalendarFolder_returnsEmpty() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder calFolder = mock(PSTFolder.class);
                when(calFolder.getDescriptorNodeId()).thenReturn(456L);
                when(calFolder.getContentCount()).thenReturn(0);
                when(calFolder.getDisplayName()).thenReturn("Calendar");
                when(calFolder.hasSubfolders()).thenReturn(false);

                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDescriptorNodeId()).thenReturn(1L);
                when(rootFolder.hasSubfolders()).thenReturn(true);
                when(rootFolder.getSubFolders()).thenReturn(new Vector<>(List.of(calFolder)));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    List<OutlookEvent> result = service.readEvents(
                            tempFile.toAbsolutePath().toString(), "456", testRange);
                    assertThat(result).isEmpty();
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("reads non-recurring appointment within range")
        void nonRecurringAppointment_inRange_returned() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                Date start = Date.from(ZonedDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant());
                Date end   = Date.from(ZonedDateTime.of(2025, 6, 15, 11, 0, 0, 0, ZoneId.of("UTC")).toInstant());

                PSTAppointment appt = mock(PSTAppointment.class);
                when(appt.isRecurring()).thenReturn(false);
                when(appt.getStartTime()).thenReturn(start);
                when(appt.getEndTime()).thenReturn(end);
                when(appt.getDescriptorNodeId()).thenReturn(101L);
                when(appt.getSubject()).thenReturn("Team Meeting");
                when(appt.getBody()).thenReturn("Agenda");
                when(appt.getLocation()).thenReturn("Room A");
                when(appt.getSubType()).thenReturn(false);
                when(appt.getStartTimeZone()).thenReturn(null); // UTC fallback

                PSTFolder calFolder = mock(PSTFolder.class);
                when(calFolder.getDescriptorNodeId()).thenReturn(456L);
                when(calFolder.getContentCount()).thenReturn(1);
                when(calFolder.getDisplayName()).thenReturn("Calendar");
                when(calFolder.hasSubfolders()).thenReturn(false);
                // First call returns the appointment; second returns null (end of folder)
                when(calFolder.getNextChild()).thenReturn(appt, (com.pff.PSTObject) null);

                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDescriptorNodeId()).thenReturn(1L);
                when(rootFolder.hasSubfolders()).thenReturn(true);
                when(rootFolder.getSubFolders()).thenReturn(new Vector<>(List.of(calFolder)));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    List<OutlookEvent> result = service.readEvents(
                            tempFile.toAbsolutePath().toString(), "456", testRange);
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).subject()).isEqualTo("Team Meeting");
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("skips non-recurring appointment outside range")
        void nonRecurringAppointment_outOfRange_skipped() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                Date start = Date.from(ZonedDateTime.of(2025, 1, 5, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant());
                Date end   = Date.from(ZonedDateTime.of(2025, 1, 5, 11, 0, 0, 0, ZoneId.of("UTC")).toInstant());

                PSTAppointment appt = mock(PSTAppointment.class);
                when(appt.isRecurring()).thenReturn(false);
                when(appt.getStartTime()).thenReturn(start);
                when(appt.getEndTime()).thenReturn(end);

                PSTFolder calFolder = mock(PSTFolder.class);
                when(calFolder.getDescriptorNodeId()).thenReturn(456L);
                when(calFolder.getContentCount()).thenReturn(1);
                when(calFolder.getDisplayName()).thenReturn("Calendar");
                when(calFolder.hasSubfolders()).thenReturn(false);
                when(calFolder.getNextChild()).thenReturn(appt, (com.pff.PSTObject) null);

                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDescriptorNodeId()).thenReturn(1L);
                when(rootFolder.hasSubfolders()).thenReturn(true);
                when(rootFolder.getSubFolders()).thenReturn(new Vector<>(List.of(calFolder)));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    List<OutlookEvent> result = service.readEvents(
                            tempFile.toAbsolutePath().toString(), "456", testRange);
                    assertThat(result).isEmpty();
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("recurring appointment with null blob falls back and returns event in range")
        void recurringAppointment_nullBlob_fallbackReturnsEvent() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                Date start = Date.from(ZonedDateTime.of(2025, 6, 15, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant());
                Date end   = Date.from(ZonedDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant());

                PSTAppointment appt = mock(PSTAppointment.class);
                when(appt.isRecurring()).thenReturn(true);
                when(appt.getRecurrenceStructure()).thenReturn(null); // triggers fallback
                when(appt.getStartTime()).thenReturn(start);
                when(appt.getEndTime()).thenReturn(end);
                when(appt.getDescriptorNodeId()).thenReturn(202L);
                when(appt.getSubject()).thenReturn("Weekly Standup");
                when(appt.getBody()).thenReturn("Notes");
                when(appt.getLocation()).thenReturn("Zoom");
                when(appt.getSubType()).thenReturn(false);
                when(appt.getStartTimeZone()).thenReturn(null);
                when(appt.getColor()).thenReturn((int) (short) 0);

                PSTFolder calFolder = mock(PSTFolder.class);
                when(calFolder.getDescriptorNodeId()).thenReturn(456L);
                when(calFolder.getContentCount()).thenReturn(1);
                when(calFolder.getDisplayName()).thenReturn("Calendar");
                when(calFolder.hasSubfolders()).thenReturn(false);
                when(calFolder.getNextChild()).thenReturn(appt, (com.pff.PSTObject) null);

                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDescriptorNodeId()).thenReturn(1L);
                when(rootFolder.hasSubfolders()).thenReturn(true);
                when(rootFolder.getSubFolders()).thenReturn(new Vector<>(List.of(calFolder)));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {

                    List<OutlookEvent> result = service.readEvents(
                            tempFile.toAbsolutePath().toString(), "456", testRange);
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).subject()).isEqualTo("Weekly Standup");
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("wraps IOException from getRootFolder in readEvents as RuntimeException")
        void readEvents_getRootFolderThrows_wrapsAsRuntimeException() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                    (pstFile, ctx) -> when(pstFile.getRootFolder()).thenThrow(new java.io.IOException("disk error")))) {
                org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.readEvents(tempFile.toAbsolutePath().toString(), "456", testRange))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to read Outlook events");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("wraps exception from findFolderById traversal (corrupt node) gracefully")
        void readEvents_findFolderById_corruptRoot_returnsEmpty() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDescriptorNodeId()).thenThrow(new RuntimeException("corrupt descriptor"));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {
                    List<OutlookEvent> result = service.readEvents(
                            tempFile.toAbsolutePath().toString(), "456", testRange);
                    assertThat(result).isEmpty();
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("outer catch in readAppointmentsFromFolder logs warning when getNextChild throws")
        void readAppointmentsFromFolder_getNextChildThrows_logsWarning() throws Exception {
            Path tempFile = Files.createTempFile("calendar-test-", ".pst");
            try {
                PSTFolder calFolder = mock(PSTFolder.class);
                when(calFolder.getDescriptorNodeId()).thenReturn(456L);
                when(calFolder.getContentCount()).thenReturn(1);
                when(calFolder.getDisplayName()).thenReturn("Calendar");
                when(calFolder.hasSubfolders()).thenReturn(false);
                when(calFolder.getNextChild()).thenThrow(new RuntimeException("corrupt item"));

                PSTFolder rootFolder = mock(PSTFolder.class);
                when(rootFolder.getDescriptorNodeId()).thenReturn(1L);
                when(rootFolder.hasSubfolders()).thenReturn(true);
                when(rootFolder.getSubFolders()).thenReturn(new Vector<>(List.of(calFolder)));

                try (MockedConstruction<PSTFile> mc = mockConstruction(PSTFile.class,
                        (pstFile, ctx) -> when(pstFile.getRootFolder()).thenReturn(rootFolder))) {
                    List<OutlookEvent> result = service.readEvents(
                            tempFile.toAbsolutePath().toString(), "456", testRange);
                    assertThat(result).isEmpty();
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
