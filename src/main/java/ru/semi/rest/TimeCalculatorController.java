package ru.semi.rest;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
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

	@SneakyThrows
	@GetMapping(value = "/bpmn", produces = "text/plain;charset=UTF-8")
	public ResponseEntity<String> bpmn () {
		RestTemplate restTemplate = new RestTemplate();

		//https://docs.camunda.org/manual/7.7/reference/rest/deployment/get-resource/
//		http://localhost:8080/rest/deployment/{id}
//		http://localhost:8080/rest/deployment/{id-of-deployment}/resources/{id-of-resource}
		File exchange = restTemplate.execute(
				"http://localhost:8080/rest/deployment/1aa3788a-a57c-11ea-9be5-7cb27d1e12c5/resources/1aa39f9b-a57c-11ea-9be5-7cb27d1e12c5/data",
				HttpMethod.GET,
				null,
				clientHttpResponse -> {
					File ret = File.createTempFile("download", "tmp");
					StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
					return ret;
		});
		BufferedReader br = new BufferedReader(new FileReader(exchange));
		String body = br.lines().collect(Collectors.joining("\n"));
		log.debug(body);
		return ResponseEntity.ok()
				.body(body);
	}

	@PostMapping("/calculateTime")
	public ResponseEntity<String> calculateTime (@RequestBody TaskTimeDto taskTimeDto) {
		String taskTime = taskTimeCalculatorService.calculateTaskTime(taskTimeDto);
		return ResponseEntity.ok(taskTime);
	}




}
