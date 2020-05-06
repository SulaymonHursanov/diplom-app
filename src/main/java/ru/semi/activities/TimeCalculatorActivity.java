package ru.semi.activities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.stereotype.Service;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class TimeCalculatorActivity implements JavaDelegate {
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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

		LocalDateTime fromTime ;

		String processInstanceId = delegateExecution.getProcessInstanceId();
		String parentTaskId = properties.get("parentTaskId");
		log.info("parent task Id: {}", parentTaskId );


		if (Objects.nonNull(parentTaskId)) {
			Optional<TaskTime> previous = taskTimeRepository.findFirstByProcessIdAndTaskId(processInstanceId, parentTaskId);
			log.info("is found previous task: {}", previous.isPresent() ? "yes" : "no");
			if (previous.isPresent()) {
				fromTime = previous.get().getToTime();
			} else throw new IllegalArgumentException("previous task not found");
		} else {
			/*Optional<TaskTime> firstByOrderByToTimeDesc = taskTimeRepository.findFirstByProcessIdOrderByEventTimeDesc(processInstanceId);
			if (firstByOrderByToTimeDesc.isPresent()) {
				TaskTime taskTime = firstByOrderByToTimeDesc.get();
				fromTime = taskTime.getToTime();
			} else {
				fromTime = LocalDateTime.now();
			}*/
			Object fromTimeObj = delegateExecution.getVariable("fromTime");
			if (Objects.nonNull(fromTimeObj)) {
				fromTime = LocalDateTime.parse((String)fromTimeObj, formatter);
			} else {
				fromTime = LocalDateTime.now();
			}
		}


		LocalDateTime endOfTaskTime = fromTime.plusHours(hours);
		log.info("from: {}, till: {}, hours: {}, name: {}, id: {}. processId: {}",
				fromTime.format(formatter), endOfTaskTime.format(formatter), hours, currentActivityName, serviceTask.getId(), processInstanceId);

		saveTask(currentActivityName, currentActivityId, processInstanceId,hours, fromTime, endOfTaskTime);

		delegateExecution.setVariable("fromTime", endOfTaskTime.format(formatter));
		Thread.sleep( hours * 1000);

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
