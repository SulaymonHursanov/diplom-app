package ru.semi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semi.entities.TaskTime;

import java.util.Optional;

public interface TaskTimeRepository extends JpaRepository<TaskTime, Long> {
	Optional<TaskTime> findFirstByOrderByToTimeDesc();
}
