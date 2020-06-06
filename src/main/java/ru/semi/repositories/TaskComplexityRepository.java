package ru.semi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semi.entities.TaskComplexity;

import java.util.Optional;

public interface TaskComplexityRepository extends JpaRepository<TaskComplexity, Long> {
    Optional<TaskComplexity> findFirstByOrderComplexityNameAndTaskId(String orderComplexityName, String taskId);
}
