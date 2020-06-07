package ru.semi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semi.entities.TaskTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskTimeRepository extends JpaRepository<TaskTime, Long> {
	Optional<TaskTime> findFirstByProcessIdOrderByToTimeDesc(String processInstanceId);
	Optional<TaskTime> findFirstByProcessIdAndTaskId(String processInstanceId, String taskId);
	List<TaskTime> findAllByTaskIdAndToTimeIsAfterOrderByToTimeDesc(String taskId, LocalDateTime toTime);
	List<TaskTime> findAllByTaskIdAndToTimeIsAfterAndTaskComplexityOrderComplexityNameOrderByToTimeDesc(String taskId, LocalDateTime toTime, String taskComplexityName);

	@Query(nativeQuery = true,value = "select * from ( " +
			"    select distinct on (task_id) * " +
			"    from task_time " +
			"	 where parent_process_instance_id =:parentProcessInstanceId " +
			"    order by task_id, queue_count desc " +
			"    ) as tmp " +
			"order by tmp.queue_count desc")
	List<TaskTime> findAllByTaskIdAndMaxQueueCount (@Param(value="parentProcessInstanceId") String generatorProcessInstanceId);
	List<TaskTime> findAllByParentProcessInstanceId(String parentProcessInstanceId);
}
