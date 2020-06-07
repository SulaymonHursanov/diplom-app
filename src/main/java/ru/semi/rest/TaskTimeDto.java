package ru.semi.rest;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskTimeDto {
	private int hours;
	private String currentActivityId;
	private String currentActivityName;
	private String processInstanceId;
	private String parentTaskId;
	private LocalDateTime fromTime;
	private Integer workerCount;
	private String taskComplexityName;
	private String parentProcessInstanceId;
}
