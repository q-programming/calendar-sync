package pl.qprogramming.calendarsync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.qprogramming.calendarsync.dto.SyncSettings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void getSettings_returnsDefaultSettings() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.frequencyMinutes").isNumber())
                .andExpect(jsonPath("$.daysPast").isNumber())
                .andExpect(jsonPath("$.daysFuture").isNumber())
                .andExpect(jsonPath("$.debugLogging").isBoolean());
    }

    @Test
    @WithMockUser
    void updateSettings_persistsAndReturns204() throws Exception {
        SyncSettings settings = new SyncSettings()
                .frequencyMinutes(30)
                .daysPast(14)
                .daysFuture(60)
                .debugLogging(true)
                .syncColorLabels(false);

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settings)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequencyMinutes").value(30))
                .andExpect(jsonPath("$.daysPast").value(14))
                .andExpect(jsonPath("$.daysFuture").value(60))
                .andExpect(jsonPath("$.debugLogging").value(true));
    }
}
