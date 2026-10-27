package com.museum.reservation.module.statistics.vo;

import lombok.Data;

import java.util.List;

@Data
public class TicketDistributionVO {
    private String name;
    private long value;
    private List<NameValue> children;
}
