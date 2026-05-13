package pl.qprogramming.calendarsync.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pl.qprogramming.calendarsync.dto.HealthStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HealthController")
class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    @DisplayName("healthCheck returns 200 OK with status UP")
    void healthCheck_Returns200WithStatusUp() {
        var response = controller.healthCheck();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UP");
    }
}
