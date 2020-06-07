package ru.semi.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

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

    private LocalTime workingStartTime;
    private LocalTime workingEndTime;
    private Integer orderCount;
    @ElementCollection
    @JoinTable(name = "order_complexity", joinColumns = @JoinColumn(name = "id"))
    @MapKeyColumn (name="order_complexity_name")
    @Column(name="value")
//    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Map<String, Integer> orderComplexity = new HashMap<>();
}
