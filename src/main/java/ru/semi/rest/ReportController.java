package ru.semi.rest;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.semi.entities.ProcessGenerator;
import ru.semi.services.ReportService;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@RestController
public class ReportController {

    private final ReportService reportService;

    @RequestMapping(value = "/tasks/queue/count")
    public ResponseEntity<Map<String, Integer>> getTasksQueueCount (@RequestParam(name = "processInstanceId") String generatorProcessInstanceId) {
        Map<String, Integer> tasksQueueCount = reportService.getTasksQueueCount(generatorProcessInstanceId);
        return ResponseEntity.ok(tasksQueueCount);
    }

    @RequestMapping(value = "/generator/instance", method = RequestMethod.GET)
    public ResponseEntity<ProcessGenerator> getGeneratorInstance (@RequestParam String generatorProcessInstanceId) {
        ProcessGenerator generatorInstanceList = reportService.getGeneratorInstance(generatorProcessInstanceId);
        return ResponseEntity.ok(generatorInstanceList);
    }

    @RequestMapping(value = "/generator/instance/list", method = RequestMethod.GET)
    public ResponseEntity<List<ProcessGenerator>> getGeneratorInstanceList () {
        List<ProcessGenerator> generatorInstanceList = reportService.getGeneratorInstanceList();
        return ResponseEntity.ok(generatorInstanceList);
    }

    @RequestMapping(value = "/download/report/{generatorInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<Void> downloadReport (@PathVariable String generatorInstanceId, HttpServletResponse httpServletResponse) {
        reportService.downloadProcessExecutionReport(generatorInstanceId, httpServletResponse);
        return ResponseEntity.ok().build();
    }

    @SneakyThrows
    @GetMapping(value = "/bpmn", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> bpmn (@RequestParam String deploymentId,
                                        @RequestParam String resourceId) {
        log.info("deploymentId: {}, resourceId: {}", deploymentId, resourceId);
        //https://docs.camunda.org/manual/7.7/reference/rest/deployment/get-resource/
//		http://localhost:8080/rest/deployment/{id}
//		http://localhost:8080/rest/deployment/{id-of-deployment}/resources/{id-of-resource}
//		example
//		http://localhost:8080/rest/deployment/1aa3788a-a57c-11ea-9be5-7cb27d1e12c5/resources/1aa39f9b-a57c-11ea-9be5-7cb27d1e12c5/data

        RestTemplate restTemplate = new RestTemplate();
        File exchange = restTemplate.execute(
                "http://localhost:8080/rest/deployment/{deploymentId}/resources/{resourceId}/data",
                HttpMethod.GET,
                null,
                clientHttpResponse -> {
                    File ret = File.createTempFile("download", "tmp");
                    StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
                    return ret;
                },
                deploymentId,
                resourceId
        );
        BufferedReader br = new BufferedReader(new FileReader(exchange));
        String body = br.lines().collect(Collectors.joining("\n"));
        log.debug(body);
        return ResponseEntity.ok()
                .body(body);
    }

}
