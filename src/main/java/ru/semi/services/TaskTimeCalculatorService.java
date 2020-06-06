package ru.semi.services;

import ru.semi.rest.TaskTimeDto;

public interface TaskTimeCalculatorService {
    String calculateTaskTime (TaskTimeDto taskTimeDto);
}
