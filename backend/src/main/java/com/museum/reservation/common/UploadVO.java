package com.museum.reservation.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadVO {
    private String url;
    private String originalName;
    private long size;
}
