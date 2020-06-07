package ru.semi.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ProcessGenerator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String processTemplateId;
    private String deploymentId;
    private String resourceId;
    private String processInstanceId;
    private LocalDateTime startTime;
    private LocalDate fromDate;
    private LocalDate tillDate;
}
