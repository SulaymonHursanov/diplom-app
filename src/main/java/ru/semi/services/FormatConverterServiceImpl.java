package ru.semi.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import ru.semi.dto.ProcessExecutionInfo;
import ru.semi.entities.ProcessGenerator;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
public class FormatConverterServiceImpl implements FormatConverterService {

    private final DateTimeFormatter localTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @SneakyThrows
    @Override
    public void toExcel(Map<String, Double> taskAverageTime,
                        Map<String, ProcessExecutionInfo> processExecutionInfoMap,
                        LocalDate fromTime, LocalDate toTime,
                        ProcessGenerator processGenerator,
                        OutputStream outputStream) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);
        XSSFSheet sheet = workbook.createSheet("sheet");
        XSSFRow titleRow = sheet.createRow(0);
        XSSFCell cell = titleRow.createCell(0);
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cell.setCellStyle(cellStyle);
        cell.setCellValue("Входные данные");

        XSSFRow row = sheet.createRow(1);
        row.createCell(0).setCellValue("Начало работы");
        row.createCell(1).setCellValue("workingStartTime");
        row.createCell(2).setCellValue(processGenerator.getWorkingStartTime().format(localTimeFormatter));

        XSSFRow stopRow = sheet.createRow(2);
        stopRow.createCell(0).setCellValue("Конец работы");
        stopRow.createCell(1).setCellValue("workingEndTime");
        stopRow.createCell(2).setCellValue(processGenerator.getWorkingEndTime().format(localTimeFormatter));

        XSSFRow startDateRow = sheet.createRow(3);
        startDateRow.createCell(0).setCellValue("С даты");
        startDateRow.createCell(1).setCellValue("fromDate");
        startDateRow.createCell(2).setCellValue(processGenerator.getFromDate().format(dateFormatter));

        XSSFRow tillDateRow = sheet.createRow(4);
        tillDateRow.createCell(0).setCellValue("До даты");
        tillDateRow.createCell(1).setCellValue("tillDate");
        tillDateRow.createCell(2).setCellValue(processGenerator.getTillDate().format(dateFormatter));

        XSSFRow orderCountRow = sheet.createRow(5);
        orderCountRow.createCell(0).setCellValue("Кол-во заказов");
        orderCountRow.createCell(1).setCellValue("orderCount");
        orderCountRow.createCell(2).setCellValue(processGenerator.getOrderCount());

        XSSFRow processNameRow = sheet.createRow(6);
        processNameRow.createCell(0).setCellValue("Название процесса");
        processNameRow.createCell(1).setCellValue("processTemplateId");
        processNameRow.createCell(2).setCellValue(processGenerator.getProcessTemplateId());

        XSSFRow orderComplexityPart = sheet.createRow(7);
        orderComplexityPart.createCell(0).setCellValue("Сложность заказа");
        orderComplexityPart.createCell(1).setCellValue("в процентах");

        Map<String, Integer> orderComplexity = processGenerator.getOrderComplexity();
        int rowIndex = 8;
        int idIndex = 1;
        for (Map.Entry<String, Integer> entry: orderComplexity.entrySet()) {
            int cellIndex = 0;
            XSSFRow sheetRow = sheet.createRow(rowIndex++);
            sheetRow.createCell(cellIndex++).setCellValue(idIndex++);
            sheetRow.createCell(cellIndex++).setCellValue(entry.getKey());
            sheetRow.createCell(cellIndex).setCellValue(entry.getValue());
        }

        XSSFRow resultRow = sheet.createRow(rowIndex++);
        resultRow.createCell(0).setCellValue("Результат:");

        XSSFRow link = sheet.createRow(rowIndex++);
        link.createCell(0).setCellValue("Ссылка на bpmn файл");
        String linkVal = String.format("http://localhost:8080/rest/deployment/%s/resources/%s/data", processGenerator.getDeploymentId(), processGenerator.getDeploymentId());
        XSSFCell linkCell = link.createCell(1);
        linkCell.setCellValue("скачать");
        hyperlinkScreenshot(linkCell, linkVal);

        XSSFRow executed = sheet.createRow(rowIndex++);
        executed.createCell(0).setCellValue("Выполнено:");
        executed.createCell(1).setCellValue(fromTime.format(dateFormatter));
        executed.createCell(2).setCellValue(toTime.format(dateFormatter));

        XSSFRow deadline = sheet.createRow(rowIndex++);
        deadline.createCell(0).setCellValue("Просрочено");
        long days = ChronoUnit.DAYS.between(toTime, processGenerator.getTillDate());
        deadline.createCell(1).setCellValue(days + getTitle (days));

        XSSFRow order = sheet.createRow(rowIndex++);
        order.createCell(0).setCellValue("Заказ");
        order.createCell(1).setCellValue("с");
        order.createCell(2).setCellValue("по");

        idIndex = 1;
        for (Map.Entry<String, ProcessExecutionInfo> entry: processExecutionInfoMap.entrySet()) {
            int cellIndex = 0;
            XSSFRow sheetRow = sheet.createRow(rowIndex++);
            sheetRow.createCell(cellIndex++).setCellValue(idIndex++);
            sheetRow.createCell(cellIndex++).setCellValue(entry.getValue().getStartTime().format(dateTimeFormatter));
            sheetRow.createCell(cellIndex).setCellValue(entry.getValue().getStopTime().format(dateTimeFormatter));
        }

        XSSFRow taskAverage = sheet.createRow(rowIndex++);
        taskAverage.createCell(0).setCellValue("Среднее время выполнение задачи");

        idIndex = 1;
        for (Map.Entry<String, Double> entry: taskAverageTime.entrySet()) {
            int cellIndex = 0;
            XSSFRow sheetRow = sheet.createRow(rowIndex++);
            sheetRow.createCell(cellIndex++).setCellValue(idIndex++);
            sheetRow.createCell(cellIndex++).setCellValue(entry.getKey());
            sheetRow.createCell(cellIndex).setCellValue(entry.getValue());
        }

        for (int index = 0; index < 4; index++) {
            sheet.autoSizeColumn(index);
        }
        workbook.write(outputStream);
    }

    private String getTitle(long days) {
        switch (Long.valueOf(days).intValue()) {
            case 1: return " день";
            case 2:
            case 3:
                return " дня";
            default: return " дней";
        }
    }

    private void setVal (XSSFRow row, String value, int cellIndex) {
        row.createCell(cellIndex).setCellValue(value);
    }

    //hyperlink screenshot
    public static void hyperlinkScreenshot(XSSFCell cell, String FileAddress){
        XSSFWorkbook wb=cell.getRow().getSheet().getWorkbook();
        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle hlink_style = wb.createCellStyle();
        Font hlink_font = wb.createFont();
        hlink_font.setUnderline(Font.U_SINGLE);
        hlink_font.setColor(IndexedColors.BLUE.getIndex());
        hlink_style.setFont(hlink_font);
        Hyperlink hp = createHelper.createHyperlink(HyperlinkType.URL);
        FileAddress=FileAddress.replace("\\", "/");
        hp.setAddress(FileAddress);
        cell.setHyperlink(hp);
        cell.setCellStyle(hlink_style);
    }


}
