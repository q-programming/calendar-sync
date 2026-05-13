package pl.qprogramming.calendarsync.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnauthorizedException")
class UnauthorizedExceptionTest {

    @Test
    @DisplayName("message-only constructor stores message")
    void messageConstructor() {
        var ex = new UnauthorizedException("not authorized");
        assertThat(ex.getMessage()).isEqualTo("not authorized");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("message+cause constructor stores both")
    void messageCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new UnauthorizedException("auth failed", cause);
        assertThat(ex.getMessage()).isEqualTo("auth failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("is an Exception subtype")
    void isException() {
        assertThat(new UnauthorizedException("x")).isInstanceOf(Exception.class);
    }
}
