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
import org.springframework.stereotype.Service;
import ru.semi.dto.OrderInfo;

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

		orderInfos.forEach(orderInfo -> {
			Map<String, Object> variables = new HashMap<>();
			variables.put("fromTime", LocalDateTime.of(orderInfo.getFromDate(), workingStartTime).format(dateTimeFormatter));
			variables.put("orderComplexity", orderInfo.getOrderComplexity());
			log.info("started process");
			runtimeService.startProcessInstanceByKey("FurnitureFactoryProcess", variables);
		});
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
}
