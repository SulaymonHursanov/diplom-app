package ru.semi;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.semi.dto.ExternalTaskDto;
import ru.semi.dto.ExternalWorkerDto;
import ru.semi.dto.TopicDto;

import java.util.*;

@Slf4j
@Component
public class ExternalTaskClientApp {

	private Map<String, Queue<String>> taskQueueMap = new HashMap<>();

	@SneakyThrows
	@Scheduled(fixedDelay = 10 * 1000)
	public void handleTasks () {
		log.info("starting worker handler");
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:8080/rest/external-task/fetchAndLock";

		ExternalWorkerDto externalWorkerDto = new ExternalWorkerDto();
		externalWorkerDto.setAsyncResponseTimeout(1000);
		externalWorkerDto.setMaxTasks(10L);
		externalWorkerDto.setUsePriority(true);
		externalWorkerDto.setWorkerId("cloud");
		TopicDto topic = new TopicDto();
		topic.setTopicName("invoiceCreator");
		topic.setLockDuration(20000L);
		externalWorkerDto.setTopics(Collections.singletonList(topic));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<ExternalWorkerDto> httpEntity = new HttpEntity<>(externalWorkerDto,headers);
		ResponseEntity<List<ExternalTaskDto>> exchange = restTemplate.exchange(
					url,
					HttpMethod.POST,
					httpEntity,
					new ParameterizedTypeReference<List<ExternalTaskDto>>() {}
				);
		List<ExternalTaskDto> externalTaskDtos = exchange.getBody();
		log.info("worker req status: {}", exchange.getStatusCode().getReasonPhrase());
		log.info("worker req body: {}",externalTaskDtos);
		externalTaskDtos.forEach(externalTaskDto -> {
					String task = externalTaskDto.getProcessInstanceId() + ":" + externalTaskDto.getActivityId() + "::" + externalTaskDto.getId();
					if (taskQueueMap.containsKey(externalTaskDto.getActivityId())) {
						Queue<String> taskQueue = taskQueueMap.get(externalTaskDto.getActivityId());
						if (!taskQueue.contains(task)){
							taskQueue.add(task);
						}
					}else {
						taskQueueMap.put(externalTaskDto.getActivityId(), new LinkedList<String>() {{add(task);}});
					}
		});

		if (!taskQueueMap.isEmpty()) {
			System.out.println(taskQueueMap);
			processTask();

		}
		log.info("ending worker handler");
	}

	// i need new function for storing end time of tasks to compare with current execution task to get sequental date

	@SneakyThrows
//	@Scheduled(fixedDelay = 5 * 1000)
	public void processTask () {
		log.info("started process task");
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:8080/rest/external-task/{id}/complete";

		ExternalWorkerDto externalWorkerDto = new ExternalWorkerDto();
		externalWorkerDto.setWorkerId("cloud");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<ExternalWorkerDto> httpEntity = new HttpEntity<>(externalWorkerDto,headers);

		Map<String, Queue<String>> swap = new HashMap<>(taskQueueMap);
		swap.forEach((k, v) -> {
			for (String task : v) {
				// process task
				String id = task.split("::")[1];
				System.out.println("id: " + id);
				ResponseEntity<Void> exchange = restTemplate.exchange(
						url,
						HttpMethod.POST,
						httpEntity,
						Void.class,
						id
				);
				System.out.println(exchange.getStatusCode().getReasonPhrase());
				boolean remove = taskQueueMap.get(k).remove(task);
				log.info("is removed task " + task + " ? " + (remove ? "yes": "no"));
			}
		});
		log.info("ending process task");
	}


//	@EventListener(ApplicationReadyEvent.class)
	public void process() {
		System.out.println("test");
		ExternalTaskClient client = ExternalTaskClient.create()
				.baseUrl("http://localhost:8080/rest")
				.asyncResponseTimeout(1000)
				.maxTasks(10)
				.build();

		// subscribe to the topic
		client.subscribe("invoiceCreator")
				.handler((externalTask, externalTaskService) -> {

//					// instantiate an invoice object
//					Invoice invoice = new Invoice("A123");
//
//					// create an object typed variable with the serialization format XML
//					ObjectValue invoiceValue = ClientValues
//							.objectValue(invoice)
//							.serializationDataFormat("application/xml")
//							.create();
					log.info("The External Task " + externalTask.getId() + " name: " + externalTask.getActivityId() +
							" has been started!");

					if (externalTask.getActivityId().equals("Task_2")) {
						Map<String, Object> allVariables = externalTask.getAllVariables();
						System.out.println(allVariables);
					}

					// add the invoice object and its id to a map
					Map<String, Object> variables = new HashMap<>();
//					variables.put("invoiceId", invoice.id);
//					variables.put("invoice", invoiceValue);

					externalTaskService.complete(externalTask);

					log.info("The External Task " + externalTask.getId() + " name: " + externalTask.getActivityId() +
							" has been completed!");

				}).lockDuration(1000).open();
	}
}
