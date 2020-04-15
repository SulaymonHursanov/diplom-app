package ru.semi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semi.entities.TaskTime;

import java.util.Optional;

public interface TaskTimeRepository extends JpaRepository<TaskTime, Long> {
	Optional<TaskTime> findFirstByOrderByToTimeDesc();
	Optional<TaskTime> findFirstByProcessIdOrderByEventTimeDesc(String processInstanceId);
	Optional<TaskTime> findFirstByProcessIdOrderByToTimeDesc(String processInstanceId);
	Optional<TaskTime> findFirstByProcessIdAndTaskId(String processInstanceId, String taskId);
}
