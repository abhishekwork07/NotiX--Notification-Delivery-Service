package com.abhishek.notix.api_service.monitoring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitoring")
public class InfrastructureHealthController {

    private final InfrastructureHealthService infrastructureHealthService;

    public InfrastructureHealthController(InfrastructureHealthService infrastructureHealthService) {
        this.infrastructureHealthService = infrastructureHealthService;
    }

    @GetMapping("/infra/health")
    public InfrastructureHealthService.InfrastructureHealthResponse infrastructureHealth() {
        return infrastructureHealthService.checkInfrastructure();
    }
}
