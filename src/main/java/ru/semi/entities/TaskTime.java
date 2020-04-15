package ru.semi.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_time")
@Getter
@Setter
public class TaskTime {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private LocalDateTime fromTime;
	private LocalDateTime toTime;
	private int hours;
	private String taskId;
	private LocalDateTime eventTime;
	private String processId;
}
