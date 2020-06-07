package ru.semi.services;

import ru.semi.dto.ProcessExecutionInfo;
import ru.semi.entities.ProcessGenerator;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FormatConverterService {

    void toExcel(Map<String, Double> taskAverageTime,
                 Map<String, ProcessExecutionInfo> processExecutionInfoMap,
                 LocalDate fromTime, LocalDate toTime,
                 ProcessGenerator processGenerator,
                 OutputStream outputStream);
}
