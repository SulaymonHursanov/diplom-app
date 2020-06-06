package ru.semi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semi.entities.TaskTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskTimeRepository extends JpaRepository<TaskTime, Long> {
	Optional<TaskTime> findFirstByProcessIdOrderByToTimeDesc(String processInstanceId);
	Optional<TaskTime> findFirstByProcessIdAndTaskId(String processInstanceId, String taskId);
	List<TaskTime> findAllByTaskIdAndToTimeIsAfterOrderByToTimeDesc(String taskId, LocalDateTime toTime);
	List<TaskTime> findAllByTaskIdAndToTimeIsAfterAndTaskComplexityOrderComplexityNameOrderByToTimeDesc(String taskId, LocalDateTime toTime, String taskComplexityName);
}
