package pl.qprogramming.calendarsync.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebController")
class WebControllerTest {

    private final WebController controller = new WebController();

    @Test
    @DisplayName("forwardToIndex returns forward to index.html")
    void forwardToIndex_returnsForwardDirective() {
        String result = controller.forwardToIndex();
        assertThat(result).isEqualTo("forward:/index.html");
    }
}
