package ru.semi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalTaskDto {
	private String activityId;
	private String processInstanceId;
	private String id;
}
