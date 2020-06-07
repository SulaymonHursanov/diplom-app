package ru.semi.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_time")
@Getter
@Setter
@ToString
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
	private String parentTaskId;
	private Integer queueCount;
	private Integer workerIndex;
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private TaskComplexity taskComplexity;
	private String parentProcessInstanceId;
}
