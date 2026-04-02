package pl.qprogramming.calendarsync.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncRunStatus;
import pl.qprogramming.calendarsync.service.LogService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LogsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogService logService;

    @Test
    @WithMockUser
    void getLogs_returnsPagedResponse() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @WithMockUser
    void getLogs_withPageAndSize_respects() throws Exception {
        mockMvc.perform(get("/api/logs?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @WithMockUser
    void getLogs_filterByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/logs?status=SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    void getLogDetails_withKnownId_returnsDetails() throws Exception {
        SyncRunEntity run = new SyncRunEntity();
        run.setId("test-run-id");
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setStatus(SyncRunStatus.SUCCESS);
        run.setCreated(0);
        run.setUpdated(0);
        run.setDeleted(0);
        logService.saveRun(run);

        mockMvc.perform(get("/api/logs/test-run-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.id").value("test-run-id"))
                .andExpect(jsonPath("$.entries").isArray());
    }

    @Test
    @WithMockUser
    void getLogDetails_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/logs/nonexistent-id"))
                .andExpect(status().isNotFound());
    }
}
