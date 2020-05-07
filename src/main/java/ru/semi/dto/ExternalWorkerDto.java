package ru.semi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalWorkerDto {
	private String workerId;
	private Long maxTasks;
	private boolean usePriority;
	private long asyncResponseTimeout;
	private List<TopicDto> topics;
}
