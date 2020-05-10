package ru.semi.activities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.semi.config.Constants.DATE_FORMAT;
import static ru.semi.config.Constants.DATE_TIME_FORMAT;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessGeneratorActivity implements JavaDelegate {
	private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
	private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
	private final RuntimeService runtimeService;

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		ServiceTask serviceTask = (ServiceTask) execution.getBpmnModelElementInstance();
		ExtensionElements extensionElements = serviceTask.getExtensionElements();
		Collection<CamundaProperty> camundaProperties = extensionElements.getElementsQuery()
				.filterByType(CamundaProperties.class)
				.singleResult()
				.getCamundaProperties();

		Map<String, String> properties = camundaProperties.stream()
				.collect(Collectors.toMap(CamundaProperty::getCamundaName, CamundaProperty::getCamundaValue));

		String workingStartTimeStr = properties.get("workingStartTime");
		LocalTime workingStartTime = LocalTime.parse(workingStartTimeStr);

		String workingEndTimeStr = properties.get("workingEndTime");
		LocalTime workingEndTime = LocalTime.parse(workingEndTimeStr);


		String fromDateStr = properties.get("fromDate");
		LocalDate fromDate = LocalDate.parse(fromDateStr, dateFormatter);

		String tillDateStr = properties.get("tillDate");
		LocalDate tillDate = LocalDate.parse(tillDateStr, dateFormatter);

		int orderCount = Integer.parseInt(properties.get("orderCount"));

		List<LocalDate> randomDates = new ArrayList<>();
		for (int count = 0; count < orderCount; count++) {
			LocalDate randomDate = generateDate(fromDate, tillDate);
			randomDates.add(randomDate);
		}
		randomDates.sort(Comparator.naturalOrder());

		randomDates.forEach(fromRandDate -> {

			Map<String, Object> variables = new HashMap<>();
			variables.put("fromTime", LocalDateTime.of(fromRandDate, workingStartTime).format(dateTimeFormatter));
			ProcessInstance furnitureFactoryProcess = runtimeService.startProcessInstanceByKey("FurnitureFactoryProcess", variables);
			log.info("started process");
		});
	}

	private LocalDate generateDate (LocalDate fromDate, LocalDate tillDate) {
		long between = DAYS.between(fromDate, tillDate);
		return fromDate.plusDays(ThreadLocalRandom.current().nextInt((int) (between+1)));
	}
}
