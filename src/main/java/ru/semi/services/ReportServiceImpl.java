package ru.semi.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.semi.dto.ProcessExecutionInfo;
import ru.semi.entities.ProcessGenerator;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.ProcessGeneratorRepository;
import ru.semi.repositories.TaskTimeRepository;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Slf4j
@AllArgsConstructor
@Service
public class ReportServiceImpl implements ReportService {

    private final TaskTimeRepository taskTimeRepository;
    private final ProcessGeneratorRepository processGeneratorRepository;

    @Override
    public Map<String, Integer> getTasksQueueCount(String generatorProcessInstanceId) {
        log.info("getTasksQueueCount: generatorProcessInstanceId: {}", generatorProcessInstanceId);
        List<TaskTime> taskTimes = taskTimeRepository.findAllByTaskIdAndMaxQueueCount(generatorProcessInstanceId);
        Map<String, Integer> taskQueueCount = taskTimes.stream()
                .collect(Collectors.toMap(TaskTime::getTaskId, TaskTime::getQueueCount))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        return taskQueueCount;
    }

    @Override
    public List<ProcessGenerator> getGeneratorInstanceList() {
        List<ProcessGenerator> processGeneratorList = processGeneratorRepository.findAll(Sort.by("startTime").descending());
        return processGeneratorList;
    }

    @Override
    public void downloadProcessExecutionReport(String generatorInstanceId, HttpServletResponse httpServletResponse) {
        ProcessGenerator processGenerator = processGeneratorRepository.findFirstByProcessInstanceId(generatorInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Not found generator process instance by id " + generatorInstanceId));
        List<TaskTime> taskTimeList = taskTimeRepository.findAllByParentProcessInstanceId(generatorInstanceId);
        // legacy code
/*        Map<String, Double> taskAverageTimeLegacy = new HashMap<>();
        Map<String, Integer> taskCount = new HashMap<>();
        for (TaskTime taskTime : taskTimeList) {
            taskCount.put(taskTime.getName(), taskCount.getOrDefault(taskTime.getName(), 0) + 1);
            Double taskExecutionTime = taskAverageTimeLegacy.getOrDefault(taskTime.getName(), 0.0);
            taskAverageTimeLegacy.put(taskTime.getName(), taskTime.getHours() + taskExecutionTime);
        }
        taskCount.forEach((k, v) -> taskAverageTimeLegacy.put(k ,taskAverageTimeLegacy.get(k)/(double)v));*/
        Map<String, Double> taskAverageTime = taskTimeList.stream()
                .collect(
                        Collectors.groupingBy(TaskTime::getName, Collectors.averagingDouble(TaskTime::getHours))
                );
        taskAverageTime.forEach((k, v) ->{
            log.info("task name: {}, average time: {}", k, v);
        });
        Map<String, ProcessExecutionInfo> processExecutionInfoMap = getProcessExecutionInfoMap(taskTimeList);
        LocalDateTime fromTime = taskTimeList.stream()
                .min(Comparator.comparing(TaskTime::getFromTime))
                .get()
                .getFromTime();
        LocalDateTime toTime = taskTimeList.stream()
                .max(Comparator.comparing(TaskTime::getToTime))
                .get()
                .getToTime();
        processGenerator.getFromDate();
        processGenerator.getTillDate();
    }

    @Override
    public ProcessGenerator getGeneratorInstance(String generatorProcessInstanceId) {
        Optional<ProcessGenerator> processGeneratorOptional = processGeneratorRepository.findFirstByProcessInstanceId(generatorProcessInstanceId);
        ProcessGenerator processGenerator = processGeneratorOptional.orElseThrow(
                () -> new IllegalArgumentException("Not found generator process instance by id " + generatorProcessInstanceId)
        );
        return processGenerator;
    }

    private Map<String, ProcessExecutionInfo> getProcessExecutionInfoMap(List<TaskTime> taskTimeList) {
        Map<String, ProcessExecutionInfo> processExecutionInfoMap = new HashMap<>();
        for (TaskTime taskTime : taskTimeList) {
            ProcessExecutionInfo processExecutionInfo = processExecutionInfoMap.getOrDefault(taskTime.getProcessId(), new ProcessExecutionInfo());
            LocalDateTime fromTime = taskTime.getFromTime();
            LocalDateTime toTime = taskTime.getToTime();
            if (nonNull(processExecutionInfo.getInstanceId())) {
                LocalDateTime startTime = processExecutionInfo.getStartTime();
                LocalDateTime stopTime = processExecutionInfo.getStopTime();
                if (startTime.isAfter(fromTime)) {
                    processExecutionInfo.setStartTime(fromTime);
                }
                if (stopTime.isBefore(toTime)) {
                    processExecutionInfo.setStopTime(toTime);
                }
            }else {
                processExecutionInfo.setInstanceId(taskTime.getParentProcessInstanceId());
                processExecutionInfo.setStartTime(fromTime);
                processExecutionInfo.setStopTime(toTime);
            }
        }
        return processExecutionInfoMap;
    }
}
