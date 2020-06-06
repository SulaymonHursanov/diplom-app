package ru.semi.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderInfo {
    private LocalDate fromDate;
    private String orderComplexity;
}
