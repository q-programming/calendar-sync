package pl.qprogramming.calendarsync.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.api.HealthzApiDelegate;
import pl.qprogramming.calendarsync.dto.HealthStatus;

@Component
public class HealthController implements HealthzApiDelegate {

    @Override
    public ResponseEntity<HealthStatus> healthCheck() {
        return ResponseEntity.ok(new HealthStatus().status("UP"));
    }
}
