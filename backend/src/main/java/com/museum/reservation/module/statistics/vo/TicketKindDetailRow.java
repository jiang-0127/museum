package com.museum.reservation.module.statistics.vo;

import lombok.Data;

@Data
public class TicketKindDetailRow {
    private String kindName;
    private String museumName;
    private String ticketName;
    private long value;
}
