package ru.semi.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;
import ru.semi.rest.TaskTimeDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static ru.semi.config.Constants.DATE_TIME_FORMAT;

@Slf4j
@AllArgsConstructor
@Service
public class TaskTimeCalculatorServiceImpl implements TaskTimeCalculatorService {

    private final TaskTimeRepository taskTimeRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    @Override
    public String calculateTaskTime(TaskTimeDto taskTimeDto) {
        log.info("taskTimeDto: {}", taskTimeDto);
        int hours = taskTimeDto.getHours();
        LocalDateTime fromTime ;

        String processInstanceId = taskTimeDto.getProcessInstanceId();
        String parentTaskId = taskTimeDto.getParentTaskId();
        log.info("parent task Id: {}", parentTaskId );

        String taskComplexityName = taskTimeDto.getTaskComplexityName();

        if (nonNull(parentTaskId)) {
            Optional<TaskTime> previous = taskTimeRepository.findFirstByProcessIdAndTaskId(processInstanceId, parentTaskId);
            log.info("is found previous task: {}", previous.isPresent() ? "yes" : "no");
            if (previous.isPresent()) {
                fromTime = previous.get().getToTime();
            } else throw new IllegalArgumentException("previous task not found");
        } else {
            fromTime = nonNull(taskTimeDto.getFromTime()) ? taskTimeDto.getFromTime() : LocalDateTime.now();
        }
        List<TaskTime> previousProcessTask ;
        if (nonNull(taskComplexityName)) {
            previousProcessTask = taskTimeRepository.findAllByTaskIdAndToTimeIsAfterAndTaskComplexityOrderComplexityNameOrderByToTimeDesc(
                    taskTimeDto.getCurrentActivityId(), fromTime, taskComplexityName);
        }else {
            previousProcessTask = taskTimeRepository.findAllByTaskIdAndToTimeIsAfterOrderByToTimeDesc(taskTimeDto.getCurrentActivityId(), fromTime);
        }

        int queueCount = 0;
        int workerIndex = 1;
        if (previousProcessTask.size() > 0) {
            TaskTime taskTime = previousProcessTask.get(0);
            if (taskTime.getWorkerIndex() < taskTimeDto.getWorkerCount()) {
                workerIndex = taskTime.getWorkerIndex() + 1;
            } else {
                queueCount = previousProcessTask.size();
                LocalDateTime toTime = taskTime.getToTime();
                log.info("toTime -> fromTime {} for new task: {}", fromTime, toTime);
                fromTime = toTime;
            }
        }

        LocalDateTime endOfTaskTime = fromTime.plusHours(hours);
        log.info("from: {}, till: {}, hours: {}, name: {},  processId: {}",
                fromTime.format(formatter), endOfTaskTime.format(formatter), hours, taskTimeDto.getCurrentActivityName(),  processInstanceId);

        saveTask(taskTimeDto.getCurrentActivityName(),
                taskTimeDto.getCurrentActivityId(),
                parentTaskId,
                processInstanceId,
                hours,
                fromTime,
                endOfTaskTime,
                queueCount,
                workerIndex
        );

        return endOfTaskTime.format(formatter);
    }

    private void saveTask(String currentActivityName, String currentActivityId, String parentTaskId, String processInstanceId, int hours, LocalDateTime fromTime, LocalDateTime endOfTaskTime, int queueCount, int workerIndex) {
        TaskTime taskTime = new TaskTime();
        taskTime.setFromTime(fromTime);
        taskTime.setToTime(endOfTaskTime);
        taskTime.setHours(hours);
        taskTime.setName(currentActivityName);
        taskTime.setTaskId(currentActivityId);
        taskTime.setProcessId(processInstanceId);
        taskTime.setEventTime(LocalDateTime.now());
        taskTime.setParentTaskId(parentTaskId);
        taskTime.setQueueCount(queueCount);
        taskTime.setWorkerIndex(workerIndex);
        taskTimeRepository.save(taskTime);
    }
}
