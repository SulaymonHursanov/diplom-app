package ru.semi.services;

import ru.semi.entities.ProcessGenerator;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ReportService {
    Map<String, Integer> getTasksQueueCount(String generatorProcessInstanceId);

    List<ProcessGenerator> getGeneratorInstanceList();

    void downloadProcessExecutionReport(String generatorInstanceId, HttpServletResponse httpServletResponse);

    ProcessGenerator getGeneratorInstance(String generatorProcessInstanceId);
}
