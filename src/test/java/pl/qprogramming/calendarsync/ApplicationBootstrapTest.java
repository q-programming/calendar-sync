package pl.qprogramming.calendarsync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Application bootstrap classes")
class ApplicationBootstrapTest {

    @Test
    @DisplayName("CalendarSyncApplication can be instantiated")
    void calendarSyncApplication_canBeInstantiated() {
        assertThat(new CalendarSyncApplication()).isNotNull();
    }

    @Test
    @DisplayName("ServletInitializer.configure() returns the builder with the application source")
    void servletInitializer_configure_returnsBuilder() {
        ServletInitializer initializer = new ServletInitializer();
        SpringApplicationBuilder builder = mock(SpringApplicationBuilder.class);
        when(builder.sources(CalendarSyncApplication.class)).thenReturn(builder);

        SpringApplicationBuilder result = initializer.configure(builder);
        assertThat(result).isSameAs(builder);
    }
}
