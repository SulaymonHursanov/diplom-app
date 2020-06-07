package ru.semi.activities;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static ru.semi.config.Constants.DATE_TIME_FORMAT;

@Service
public class TimeSelectorActivity implements JavaDelegate {
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

	@Autowired
	private TaskTimeRepository taskTimeRepository;

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String processInstanceId = execution.getProcessInstanceId();
		String parentProcessInstance = (String) execution.getVariable("parentProcessInstance");
		Optional<TaskTime> taskTimeOpt = taskTimeRepository.findFirstByProcessIdAndParentProcessInstanceIdOrderByToTimeDesc(processInstanceId, parentProcessInstance);
		TaskTime taskTime = taskTimeOpt.orElseThrow(() -> new IllegalArgumentException("task not found on process: " + processInstanceId));
		execution.setVariable("fromTime", taskTime.getToTime().format(formatter));
	}
}
