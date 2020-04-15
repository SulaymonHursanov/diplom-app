package ru.semi.activities;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class TimeSelectorActivity implements JavaDelegate {
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Autowired
	private TaskTimeRepository taskTimeRepository;

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String processInstanceId = execution.getProcessInstanceId();

		Optional<TaskTime> taskTimeOpt = taskTimeRepository.findFirstByProcessIdOrderByToTimeDesc(processInstanceId);
		TaskTime taskTime = taskTimeOpt.orElseThrow(() -> new IllegalArgumentException("task not found on process: " + processInstanceId));
		execution.setVariable("fromTime", taskTime.getToTime().format(formatter));
	}
}
