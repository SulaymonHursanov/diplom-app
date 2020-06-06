package ru.semi.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "task_complexity")
@Setter
@Getter
public class TaskComplexity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderComplexityName;
    private String taskId;
    private Long minHours;
    private Long maxHours;
}
