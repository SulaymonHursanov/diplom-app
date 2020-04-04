package ru.semi.activities;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TimeCalculatorActivity implements JavaDelegate {
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	@Autowired
	private TaskTimeRepository taskTimeRepository;


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


		Optional<TaskTime> previous = taskTimeRepository.findFirstByOrderByToTimeDesc();
		LocalDateTime fromTime ;
		if (previous.isPresent()){
//			fromTime = LocalDateTime.parse((String)delegateExecution.getVariable("fromTime"), formatter);
			fromTime = previous.get().getToTime();
		}else {
			fromTime = LocalDateTime.now();
		}

		Thread.sleep( hours * 1000);

		LocalDateTime endOfTaskTime = fromTime.plusHours(hours);

		saveTask(currentActivityName, currentActivityId, hours, fromTime, endOfTaskTime);

		delegateExecution.setVariable("fromTime", endOfTaskTime.format(formatter));

		log.info("from: {}, till: {}, hours: {}, name: {}", fromTime.format(formatter), endOfTaskTime.format(formatter), hours, currentActivityName);
	}

	private void saveTask(String currentActivityName, String currentActivityId, int hours, LocalDateTime fromTime, LocalDateTime endOfTaskTime) {
		TaskTime taskTime = new TaskTime();
		taskTime.setFromTime(fromTime);
		taskTime.setToTime(endOfTaskTime);
		taskTime.setHours(hours);
		taskTime.setName(currentActivityName);
		taskTime.setTaskId(currentActivityId);
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
