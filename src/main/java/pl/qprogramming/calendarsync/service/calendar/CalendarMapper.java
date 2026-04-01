package pl.qprogramming.calendarsync.service.calendar;

import com.google.api.client.util.DateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * MapStruct mapper for converting Google Calendar API objects to DTO objects
 */
@Mapper(componentModel = "spring")
public interface CalendarMapper {

    /**
     * Maps Google Calendar DateTime to Java OffsetDateTime
     *
     * @param dateTime Google Calendar DateTime
     * @return OffsetDateTime representation of the date
     */
    @Named("dateTimeToOffsetDateTime")
    default OffsetDateTime dateTimeToOffsetDateTime(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return OffsetDateTime.parse(dateTime.toStringRfc3339());
    }

    /**
     * Maps Google Calendar DateTime to Java LocalDate for all-day events.
     *
     * @param dateTime Google Calendar DateTime (expected to be a date-only value)
     * @return LocalDate representation of the date
     */
    @Named("dateTimeToLocalDate")
    default LocalDate dateTimeToLocalDate(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        // For date-only values, Google's DateTime toStringRfc3339() returns YYYY-MM-DD.
        return LocalDate.parse(dateTime.toStringRfc3339().substring(0, 10));
    }
}
