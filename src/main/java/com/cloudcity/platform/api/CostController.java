package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.CostDeltaResponse;
import com.cloudcity.platform.api.dto.CostResponse;
import com.cloudcity.platform.domain.CostSnapshot;
import com.cloudcity.platform.service.CostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/cost")
@Tag(name = "Cost")
public class CostController {
    private final CostService costService;

    public CostController(CostService costService) {
        this.costService = costService;
    }

    @GetMapping
    public CostResponse getCost(@PathVariable UUID projectId) {
        CostSnapshot snapshot = costService.getLatest(projectId);
        if (snapshot == null) {
            snapshot = costService.recompute(projectId);
        }
        return toResponse(snapshot);
    }

    @PostMapping("/recompute")
    @ResponseStatus(HttpStatus.CREATED)
    public CostResponse recompute(@PathVariable UUID projectId) {
        CostSnapshot snapshot = costService.recompute(projectId);
        return toResponse(snapshot);
    }

    @GetMapping("/delta")
    public CostDeltaResponse getDelta(@PathVariable UUID projectId) {
        CostSnapshot latest = costService.getLatest(projectId);
        BigDecimal previous = latest == null ? BigDecimal.ZERO : latest.getTotalCost();
        BigDecimal current = costService.computeCurrentTotal(projectId);
        return new CostDeltaResponse(previous, current, current.subtract(previous), costService.getCurrency());
    }

    private CostResponse toResponse(CostSnapshot snapshot) {
        return new CostResponse(
                snapshot.getTotalCost(),
                snapshot.getCurrency(),
                snapshot.getBreakdownJson(),
                snapshot.getPricingVersion(),
                snapshot.getCreatedAt()
        );
    }
}
