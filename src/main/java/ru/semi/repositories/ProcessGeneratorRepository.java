package ru.semi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semi.entities.ProcessGenerator;

import java.util.Optional;


public interface ProcessGeneratorRepository extends JpaRepository<ProcessGenerator, Long> {
    Optional<ProcessGenerator> findFirstByProcessInstanceId(String processInstanceId);
}
