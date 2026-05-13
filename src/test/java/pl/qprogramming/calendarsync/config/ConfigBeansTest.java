package pl.qprogramming.calendarsync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Configuration bean factories")
class ConfigBeansTest {

    // ── CacheConfig ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CacheConfig")
    class CacheConfigTests {

        @Test
        @DisplayName("cacheManager() returns a CaffeineCacheManager instance")
        void cacheManagerReturnsCaffeine() {
            CacheConfig config = new CacheConfig();
            CacheManager manager = config.cacheManager();

            assertThat(manager).isInstanceOf(CaffeineCacheManager.class);
        }

        @Test
        @DisplayName("cacheManager() can create caches on demand")
        void cacheManagerCreatesCache() {
            CacheConfig config = new CacheConfig();
            CacheManager manager = config.cacheManager();

            // Accessing a cache name should succeed (Caffeine cache manager creates on demand)
            var cache = manager.getCache("test-cache");
            assertThat(cache).isNotNull();
        }
    }

    // ── JacksonConfig ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JacksonConfig")
    class JacksonConfigTests {

        @Test
        @DisplayName("objectMapper() returns a non-null ObjectMapper")
        void objectMapperIsNotNull() {
            JacksonConfig config = new JacksonConfig();
            ObjectMapper mapper = config.objectMapper(new Jackson2ObjectMapperBuilder());
            assertThat(mapper).isNotNull();
        }

        @Test
        @DisplayName("objectMapper() disables WRITE_DATES_AS_TIMESTAMPS")
        void objectMapperDisablesDatesAsTimestamps() {
            JacksonConfig config = new JacksonConfig();
            ObjectMapper mapper = config.objectMapper(new Jackson2ObjectMapperBuilder());
            assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        }

        @Test
        @DisplayName("objectMapper() serializes LocalDate as ISO string, not array")
        void objectMapperSerializesDateAsString() throws Exception {
            JacksonConfig config = new JacksonConfig();
            ObjectMapper mapper = config.objectMapper(new Jackson2ObjectMapperBuilder());
            String json = mapper.writeValueAsString(LocalDate.of(2025, 6, 15));
            assertThat(json).isEqualTo("\"2025-06-15\"");
        }
    }

    // ── RestTemplateConfig ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("RestTemplateConfig")
    class RestTemplateConfigTests {

        @Test
        @DisplayName("standardRestTemplate() returns a non-null RestTemplate")
        void standardRestTemplateIsNotNull() {
            RestTemplateConfig config = new RestTemplateConfig();
            RestTemplate restTemplate = config.standardRestTemplate();
            assertThat(restTemplate).isNotNull();
        }

        @Test
        @DisplayName("devRestTemplate() returns a non-null trust-all RestTemplate")
        void devRestTemplateIsNotNull() {
            RestTemplateConfig config = new RestTemplateConfig();
            RestTemplate restTemplate = config.devRestTemplate();
            assertThat(restTemplate).isNotNull();
        }
    }

    // ── SchedulerConfig ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SchedulerConfig")
    class SchedulerConfigTests {

        @Test
        @DisplayName("taskScheduler() returns a ThreadPoolTaskScheduler with correct pool size")
        void taskSchedulerHasCorrectPoolSize() {
            SchedulerConfig config = new SchedulerConfig();
            TaskScheduler scheduler = config.taskScheduler();

            assertThat(scheduler).isInstanceOf(ThreadPoolTaskScheduler.class);
            ThreadPoolTaskScheduler tpts = (ThreadPoolTaskScheduler) scheduler;
            // getCorePoolSize() returns the configured pool size, not active thread count
            assertThat(tpts.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("taskScheduler() thread name prefix is 'sync-scheduler-'")
        void taskSchedulerThreadNamePrefix() {
            SchedulerConfig config = new SchedulerConfig();
            ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) config.taskScheduler();
            assertThat(scheduler.getThreadNamePrefix()).isEqualTo("sync-scheduler-");
        }
    }

    // ── SessionCookieConfigInitializer ─────────────────────────────────────────

    @Nested
    @DisplayName("SessionCookieConfigInitializer")
    class SessionCookieConfigInitializerTests {

        @Test
        @DisplayName("onStartup configures session cookie with correct settings")
        void onStartupConfiguresCookie() throws Exception {
            SessionCookieConfigInitializer initializer = new SessionCookieConfigInitializer();

            jakarta.servlet.SessionCookieConfig scc = mock(jakarta.servlet.SessionCookieConfig.class);
            jakarta.servlet.ServletContext ctx = mock(jakarta.servlet.ServletContext.class);
            when(ctx.getSessionCookieConfig()).thenReturn(scc);
            when(ctx.getContextPath()).thenReturn("");

            // setSessionTimeout not available on raw mock → throws NoSuchMethodException handled gracefully
            initializer.onStartup(ctx);

            verify(scc).setName("JSESSIONID");
            verify(scc).setHttpOnly(true);
            verify(scc).setSecure(true);
            verify(scc).setMaxAge(14 * 24 * 60 * 60);
            verify(scc).setPath("/");
        }

        @Test
        @DisplayName("onStartup uses contextPath when non-empty")
        void onStartupUsesContextPath() throws Exception {
            SessionCookieConfigInitializer initializer = new SessionCookieConfigInitializer();

            jakarta.servlet.SessionCookieConfig scc = mock(jakarta.servlet.SessionCookieConfig.class);
            jakarta.servlet.ServletContext ctx = mock(jakarta.servlet.ServletContext.class);
            when(ctx.getSessionCookieConfig()).thenReturn(scc);
            when(ctx.getContextPath()).thenReturn("/myapp");

            initializer.onStartup(ctx);

            verify(scc).setPath("/myapp");
        }
    }
}
