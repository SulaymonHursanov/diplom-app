package ru.semi.dto;

import lombok.Data;

@Data
public class TopicDto {
	private String topicName;
	private Long lockDuration;
}
