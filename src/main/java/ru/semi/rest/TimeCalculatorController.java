package ru.semi.rest;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static ru.semi.config.Constants.DATE_TIME_FORMAT;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TimeCalculatorController {

	private final TaskTimeRepository taskTimeRepository;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

	@SneakyThrows
	@GetMapping(value = "/bpmn", produces = "text/plain;charset=UTF-8")
	public ResponseEntity<String> bpmn () {
		RestTemplate restTemplate = new RestTemplate();

		File exchange = restTemplate.execute(
				"http://localhost:8080/rest/deployment/35b4da19-92d7-11ea-ad5f-54f52165c615/resources/35b4da1a-92d7-11ea-ad5f-54f52165c615/data",
				HttpMethod.GET,
				null,
				clientHttpResponse -> {
					File ret = File.createTempFile("download", "tmp");
					StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
					return ret;
		});
		BufferedReader br = new BufferedReader(new FileReader(exchange));
		String body = br.lines().collect(Collectors.joining("\n"));
		log.debug(body);
		return ResponseEntity.ok()
				.body(body);
	}

	@PostMapping("/calculateTime")
	public ResponseEntity<String> calculateTime (@RequestBody TaskTimeDto taskTimeDto) {
		log.info("taskTimeDto: {}", taskTimeDto);
		int hours = taskTimeDto.getHours();
		LocalDateTime fromTime ;

		String processInstanceId = taskTimeDto.getProcessInstanceId();
		String parentTaskId = taskTimeDto.getParentTaskId();
		log.info("parent task Id: {}", parentTaskId );


		if (nonNull(parentTaskId)) {
			Optional<TaskTime> previous = taskTimeRepository.findFirstByProcessIdAndTaskId(processInstanceId, parentTaskId);
			log.info("is found previous task: {}", previous.isPresent() ? "yes" : "no");
			if (previous.isPresent()) {
				fromTime = previous.get().getToTime();
			} else throw new IllegalArgumentException("previous task not found");
		} else {
			fromTime = nonNull(taskTimeDto.getFromTime()) ? taskTimeDto.getFromTime() : LocalDateTime.now();
		}

		List<TaskTime> previousProcessTask = taskTimeRepository.findAllByTaskIdAndToTimeIsAfterOrderByToTimeDesc(taskTimeDto.getCurrentActivityId(), fromTime);
		int queueCount = previousProcessTask.size();
		if (previousProcessTask.size() > 0) {
			LocalDateTime toTime = previousProcessTask.get(0).getToTime();
			log.info("toTime -> fromTime {} for new task: {}", fromTime, toTime);
			fromTime = toTime;
		}

		LocalDateTime endOfTaskTime = fromTime.plusHours(hours);
		log.info("from: {}, till: {}, hours: {}, name: {},  processId: {}",
				fromTime.format(formatter), endOfTaskTime.format(formatter), hours, taskTimeDto.getCurrentActivityName(),  processInstanceId);

		saveTask(taskTimeDto.getCurrentActivityName(), taskTimeDto.getCurrentActivityId(), parentTaskId, processInstanceId,hours, fromTime, endOfTaskTime, queueCount);

		return ResponseEntity.ok(endOfTaskTime.format(formatter));
	}

	private void saveTask(String currentActivityName, String currentActivityId, String parentTaskId, String processInstanceId, int hours, LocalDateTime fromTime, LocalDateTime endOfTaskTime, int queueCount) {
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
		taskTimeRepository.save(taskTime);
	}


}
