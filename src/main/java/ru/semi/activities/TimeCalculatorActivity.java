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
import ru.semi.entities.TaskComplexity;
import ru.semi.repositories.TaskComplexityRepository;
import ru.semi.rest.TaskTimeDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.semi.config.Constants.DATE_TIME_FORMAT;

@RequiredArgsConstructor
@Slf4j
@Service
public class TimeCalculatorActivity implements JavaDelegate {
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
	private final TaskComplexityRepository taskComplexityRepository;

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



		String processInstanceId = delegateExecution.getProcessInstanceId();
		String parentTaskId = properties.get("parentTaskId");
//		log.info("parent task Id: {}", parentTaskId );


		int workerCountByOrderComplexity = 1;
		String taskComplexityName = null;
		Object orderComplexityObj = delegateExecution.getVariable("orderComplexity");
		if (nonNull(orderComplexityObj)) {
			String key = String.valueOf(orderComplexityObj);
			String taskComplexity = properties.get(key);
			if (nonNull(taskComplexity) && !taskComplexity.isEmpty()) {
				taskComplexityName = key;
				workerCountByOrderComplexity = Integer.parseInt(taskComplexity);
			}
		}
		Integer min = null;
		Integer max = null;
		if (nonNull(taskComplexityName)) {
			Optional<TaskComplexity> taskComplexityOpt = taskComplexityRepository.findFirstByOrderComplexityNameAndTaskId(taskComplexityName, currentActivityId);
			if (taskComplexityOpt.isPresent()){
				TaskComplexity taskComplexity = taskComplexityOpt.get();
				min = taskComplexity.getMinHours().intValue();
				max = taskComplexity.getMaxHours().intValue();
			}
		}

		if (isNull(max) || isNull(min)){
			min = Integer.parseInt(properties.get("min"));
			max = Integer.parseInt(properties.get("max"));
		}
		int hours = getRandomNumberInRange(min, max);

		LocalDateTime fromTime = null;

		Object fromTimeObj = delegateExecution.getVariable("fromTime");
		if (nonNull(fromTimeObj)) {
			fromTime = LocalDateTime.parse((String) fromTimeObj, formatter);
		}


		String endOfTaskTime = requestCalculation(currentActivityName, currentActivityId, processInstanceId, parentTaskId, workerCountByOrderComplexity, taskComplexityName, hours, fromTime);

		delegateExecution.setVariable("fromTime", endOfTaskTime);

	}

	private String requestCalculation(String currentActivityName, String currentActivityId, String processInstanceId, String parentTaskId, int workerCountByOrderComplexity, String taskComplexityName, int hours, LocalDateTime fromTime) {
		TaskTimeDto taskTimeDto = new TaskTimeDto();
		taskTimeDto.setCurrentActivityId(currentActivityId);
		taskTimeDto.setCurrentActivityName(currentActivityName);
		taskTimeDto.setFromTime(fromTime);
		taskTimeDto.setHours(hours);
		taskTimeDto.setParentTaskId(parentTaskId);
		taskTimeDto.setProcessInstanceId(processInstanceId);
		taskTimeDto.setWorkerCount(workerCountByOrderComplexity);
		taskTimeDto.setTaskComplexityName(taskComplexityName);

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
		return exchange.getBody();
	}


	private int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}
}
