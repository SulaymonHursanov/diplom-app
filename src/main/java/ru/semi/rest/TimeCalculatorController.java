package ru.semi.rest;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.semi.entities.TaskTime;
import ru.semi.repositories.TaskTimeRepository;
import ru.semi.services.TaskTimeCalculatorService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static ru.semi.config.Constants.DATE_TIME_FORMAT;

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
