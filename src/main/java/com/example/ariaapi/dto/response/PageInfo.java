package com.example.ariaapi.dto.response;

import lombok.Data;

@Data
public class PageInfo {
    private int page;
    private int size;
    private int totalPage;
    private long totalElements;
    private int numberOfElements;
}