package ru.semi.services;

import ru.semi.dto.TaskTimeDto;

public interface TaskTimeCalculatorService {
    String calculateTaskTime (TaskTimeDto taskTimeDto);
}
