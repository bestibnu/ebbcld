package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.PipelineCheckRequest;
import com.cloudcity.platform.api.dto.PipelineCheckResponse;
import com.cloudcity.platform.service.PipelineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/pipeline")
@Tag(name = "Pipeline")
public class PipelineController {
    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping("/check")
    public PipelineCheckResponse check(@PathVariable UUID projectId,
                                       @RequestBody(required = false) PipelineCheckRequest request) {
        return pipelineService.check(projectId, request);
    }
}
