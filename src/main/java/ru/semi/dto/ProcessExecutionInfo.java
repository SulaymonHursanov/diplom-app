package ru.semi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessExecutionInfo {
    private String instanceId;
    private LocalDateTime startTime;
    private LocalDateTime stopTime;
}
