package ru.semi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentResource {
    private String id;
    private String name;
    private String deploymentId;
}
