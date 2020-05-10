package ru.semi.activities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;
import ru.semi.rest.TaskTimeDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.semi.config.Constants.DATE_TIME_FORMAT;

@RequiredArgsConstructor
@Slf4j
@Service
public class TimeCalculatorActivity implements JavaDelegate {
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
	private final TaskTimeRepository taskTimeRepository;


	@Override
	public void execute(DelegateExecution delegateExecution) throws Exception {
		ServiceTask serviceTask = (ServiceTask) delegateExecution.getBpmnModelElementInstance();
		ExtensionElements extensionElements = serviceTask.getExtensionElements();
		Collection<CamundaProperty> camundaProperties = extensionElements.getElementsQuery()
				.filterByType(CamundaProperties.class)
				.singleResult()
				.getCamundaProperties();

		Map<String, String> properties = camundaProperties.stream()
				.collect(Collectors.toMap(CamundaProperty::getCamundaName, CamundaProperty::getCamundaValue));
		String currentActivityName = delegateExecution.getCurrentActivityName();
		String currentActivityId = delegateExecution.getCurrentActivityId();

		int min = Integer.parseInt(properties.get("min"));
		int max = Integer.parseInt(properties.get("max"));
		int hours = getRandomNumberInRange(min, max);


		String processInstanceId = delegateExecution.getProcessInstanceId();
		String parentTaskId = properties.get("parentTaskId");
//		log.info("parent task Id: {}", parentTaskId );


		LocalDateTime fromTime = null;

		Object fromTimeObj = delegateExecution.getVariable("fromTime");
		if (Objects.nonNull(fromTimeObj)) {
			fromTime = LocalDateTime.parse((String) fromTimeObj, formatter);
		}


		TaskTimeDto taskTimeDto = new TaskTimeDto();
		taskTimeDto.setCurrentActivityId(currentActivityId);
		taskTimeDto.setCurrentActivityName(currentActivityName);
		taskTimeDto.setFromTime(fromTime);
		taskTimeDto.setHours(hours);
		taskTimeDto.setParentTaskId(parentTaskId);
		taskTimeDto.setProcessInstanceId(processInstanceId);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<TaskTimeDto> httpEntity = new HttpEntity<>(taskTimeDto, httpHeaders);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> exchange = restTemplate.exchange(
				"http://localhost:8080/calculateTime",
				HttpMethod.POST,
				httpEntity,
				String.class
		);
		String endOfTaskTime = exchange.getBody();

		delegateExecution.setVariable("fromTime", endOfTaskTime);
//		Thread.sleep( hours * 1000);

	}

	private void saveTask(String currentActivityName, String currentActivityId, String processInstanceId, int hours, LocalDateTime fromTime, LocalDateTime endOfTaskTime) {
		TaskTime taskTime = new TaskTime();
		taskTime.setFromTime(fromTime);
		taskTime.setToTime(endOfTaskTime);
		taskTime.setHours(hours);
		taskTime.setName(currentActivityName);
		taskTime.setTaskId(currentActivityId);
		taskTime.setProcessId(processInstanceId);
		taskTime.setEventTime(LocalDateTime.now());
		taskTimeRepository.save(taskTime);
	}

	private int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}
}
