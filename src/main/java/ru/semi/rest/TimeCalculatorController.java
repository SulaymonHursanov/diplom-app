package ru.semi.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.semi.dto.TaskTimeDto;
import ru.semi.services.TaskTimeCalculatorService;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TimeCalculatorController {

	private final TaskTimeCalculatorService taskTimeCalculatorService;



	@PostMapping("/calculateTime")
	public ResponseEntity<String> calculateTime (@RequestBody TaskTimeDto taskTimeDto) {
		String taskTime = taskTimeCalculatorService.calculateTaskTime(taskTimeDto);
		return ResponseEntity.ok(taskTime);
	}




}
