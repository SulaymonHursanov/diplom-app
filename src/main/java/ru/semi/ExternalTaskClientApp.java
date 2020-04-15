package ru.semi;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.variable.ClientValues;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.semi.dto.ExternalTaskDto;
import ru.semi.dto.TopicDto;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ExternalTaskClientApp {


/*
	public void processByScheduler () {
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:8080/rest/external-task/fetchAndLock";

		ExternalTaskDto externalTaskDto = new ExternalTaskDto();
		externalTaskDto.setAsyncResponseTimeout(1000);
		externalTaskDto.setMaxTasks(10L);
		externalTaskDto.setUsePriority(true);
		externalTaskDto.setWorkerId("cloud");
		TopicDto topic = new TopicDto();
		topic.setTopicName("invoiceCreator");
		topic.setLockDuration(20000L);
		externalTaskDto.setTopics(Collections.singletonList(topic));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
//		HttpEntity<ExternalTaskDto> httpEntity = new HttpEntity<>(,headers);
//		restTemplate.exchange(url, HttpMethod.POST, httpEntity, );
	}
*/


	@EventListener(ApplicationReadyEvent.class)
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
