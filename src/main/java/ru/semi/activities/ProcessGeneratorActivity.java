package ru.semi.activities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.semi.dto.DeploymentResource;
import ru.semi.dto.OrderInfo;
import ru.semi.dto.ProcessDeployment;
import ru.semi.entities.ProcessGenerator;
import ru.semi.repositories.ProcessGeneratorRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.semi.config.Constants.DATE_FORMAT;
import static ru.semi.config.Constants.DATE_TIME_FORMAT;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessGeneratorActivity implements JavaDelegate {
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
	private final RuntimeService runtimeService;
	private final ProcessGeneratorRepository processGeneratorRepository;

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		ServiceTask serviceTask = (ServiceTask) execution.getBpmnModelElementInstance();
		ExtensionElements extensionElements = serviceTask.getExtensionElements();
		Collection<CamundaProperty> camundaProperties = extensionElements.getElementsQuery()
				.filterByType(CamundaProperties.class)
				.singleResult()
				.getCamundaProperties();

		Map<String, String> properties = camundaProperties.stream()
				.filter(camundaProperty -> nonNull(camundaProperty) && nonNull(camundaProperty.getCamundaName()) && nonNull(camundaProperty.getCamundaValue()))
				.collect(Collectors.toMap(CamundaProperty::getCamundaName, CamundaProperty::getCamundaValue));

		String workingStartTimeStr = properties.get("workingStartTime");
		LocalTime workingStartTime = LocalTime.parse(workingStartTimeStr);

		String workingEndTimeStr = properties.get("workingEndTime");
		LocalTime workingEndTime = LocalTime.parse(workingEndTimeStr);


		String fromDateStr = properties.get("fromDate");
		LocalDate fromDate = LocalDate.parse(fromDateStr, dateFormatter);

		String tillDateStr = properties.get("tillDate");
		LocalDate tillDate = LocalDate.parse(tillDateStr, dateFormatter);

		int orderCount = Integer.parseInt(properties.get("orderCount"));

		Map<String, Integer> orderComplexityMap = getOrderComplexityMap(properties, orderCount);

		List<String> keys = new ArrayList<>(orderComplexityMap.keySet());
		Random random = new Random();
		List<OrderInfo> orderInfos = getOrderInfos(fromDate, tillDate, orderCount, orderComplexityMap, keys, random);
		String processTemplateId = properties.get("processTemplateId");
		String deploymentId = getDeploymentId(processTemplateId);
		String resourceId = getResourceId(deploymentId);
		log.info("deploymentId: {}, resourceId: {}", deploymentId, resourceId);

		String generatorProcessInstanceId = execution.getProcessInstanceId();
		saveProcessGeneratorInfo(
				generatorProcessInstanceId,
				processTemplateId,
				deploymentId,
				resourceId
		);

		orderInfos.forEach(orderInfo -> {
			Map<String, Object> variables = new HashMap<>();
			variables.put("fromTime", LocalDateTime.of(orderInfo.getFromDate(), workingStartTime).format(dateTimeFormatter));
			variables.put("orderComplexity", orderInfo.getOrderComplexity());
			variables.put("parentProcessInstance", generatorProcessInstanceId);
			log.info("started process");
			runtimeService.startProcessInstanceByKey(processTemplateId, variables);
		});
	}

	private void saveProcessGeneratorInfo(String processInstanceId, String processTemplateId, String deploymentId, String resourceId) {
		ProcessGenerator processGenerator = new ProcessGenerator();
		processGenerator.setProcessInstanceId(processInstanceId);
		processGenerator.setProcessTemplateId(processTemplateId);
		processGenerator.setDeploymentId(deploymentId);
		processGenerator.setResourceId(resourceId);
		processGenerator.setStartTime(LocalDateTime.now());
		processGeneratorRepository.save(processGenerator);
	}

	private List<OrderInfo> getOrderInfos(LocalDate fromDate, LocalDate tillDate, int orderCount, Map<String, Integer> orderComplexityMap, List<String> keys, Random random) {
		//		List<LocalDate> randomDates = new ArrayList<>();
		Map<Integer, OrderInfo> randomDateMap = new HashMap<>();
		for (int count = 0; count < orderCount; count++) {
			String key = getRandomOrderComplexity(orderComplexityMap, keys, random);
			LocalDate randomDate = generateDate(fromDate, tillDate);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setFromDate(randomDate);
			orderInfo.setOrderComplexity(key);
			randomDateMap.put(count, orderInfo);
		}

		log.info("randomMap size: {}, map: {}", randomDateMap.size(),randomDateMap);
		return randomDateMap.values().stream()
				.sorted(Comparator.comparing(OrderInfo::getFromDate))
				.collect(Collectors.toList());
	}

	private Map<String, Integer> getOrderComplexityMap(Map<String, String> properties, double orderCount) {
		Map<String, Integer> orderComplexityMap = new HashMap<>();
		for (int count = 0; count < 10; count++) {
			String key = "orderComplexity_" + count;
			String orderComplexity = properties.get(key);
			if (nonNull(orderComplexity) && !orderComplexity.isEmpty()) {
				int orderComplexityPercentage = Integer.parseInt(orderComplexity);
				if (orderComplexityPercentage == 0){
					continue;
				}
				int orderComplexityCount =  (int)((orderCount / 100.0) * (double) orderComplexityPercentage);
				log.info("key: {}, orderComplexityCount: {}", key, orderComplexityCount);
				orderComplexityMap.put(key, orderComplexityCount);
			}
		}
		return orderComplexityMap;
	}

	private String getRandomOrderComplexity(Map<String, Integer> orderComplexityMap, List<String> keys, Random random) {
		int randomIndex = random.nextInt(keys.size());
		String key = keys.get(randomIndex);
		if (!orderComplexityMap.containsKey(key)) {
			return getRandomOrderComplexity(orderComplexityMap, keys, random);
		}
		Integer orderComplexityCount = orderComplexityMap.get(key);
		if (orderComplexityCount == 1) {
			orderComplexityMap.remove(key);
		} else if (orderComplexityCount > 1) {
			orderComplexityMap.put(key, orderComplexityCount - 1);
		}
		return key;
	}

	private LocalDate generateDate (LocalDate fromDate, LocalDate tillDate) {
		long between = DAYS.between(fromDate, tillDate);
		return fromDate.plusDays(ThreadLocalRandom.current().nextInt((int) (between+1)));
	}

	private String getDeploymentId (String name) {
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:8080/rest/deployment?name={name}";
		ResponseEntity<List<ProcessDeployment>> exchange = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<ProcessDeployment>>() {
				},
				name
		);
		List<ProcessDeployment> processDeployments = exchange.getBody();
		ProcessDeployment processDeployment = processDeployments.stream()
				.sorted(Comparator.comparing(ProcessDeployment::getDeploymentTime, Comparator.reverseOrder()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Process deployment not found by name: " + name));
		return processDeployment.getId();
	}

	private String getResourceId (String deploymentId) {
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:8080/rest/deployment/{deploymentId}/resources";
		ResponseEntity<List<DeploymentResource>> exchange = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<DeploymentResource>>() {
				},
				deploymentId
		);
		List<DeploymentResource> deploymentResources = exchange.getBody();
		DeploymentResource deploymentResource = deploymentResources.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Deployment resource not found by id: " + deploymentId));
		return deploymentResource.getId();
	}
}
