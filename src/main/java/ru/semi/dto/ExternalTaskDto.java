package ru.semi.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExternalTaskDto {
	private String workerId;
	private Long maxTasks;
	private boolean usePriority;
	private long asyncResponseTimeout;
	private List<TopicDto> topics;
}
