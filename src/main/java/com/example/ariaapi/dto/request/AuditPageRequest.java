package com.example.ariaapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

@Data
public class AuditPageRequest {

    @Min(value = 1, message = "페이지는 1이상이어야 합니다.")
    private int page = 1;

    @Max(value = 30, message = "페이지 크기는 30을 넘을 수 없습니다.")
    @Min(value = 1, message = "페이지 크기는 1이상이여야 합니다.")
    private int size = 10;

    // 감사로그의 기본 정렬 기준은 생성 날짜 내림차순입니다.
    private String sort = "createdAt,desc";

    public void setSort(String sort) {
        if (sort != null && !sort.isEmpty()) {
            this.sort = sort;
        }
    }

    public Pageable makePageRequest() {
        String[] sortParams = this.sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = Sort.Direction.DESC;

        if (sortParams.length > 1) {
            try {
                direction = Sort.Direction.fromString(sortParams[1]);
            } catch (IllegalArgumentException e) {
                // 잘못된 정렬 방향이 들어왔을 경우, 기본값 사용
            }
        }

        return PageRequest.of(page - 1, size, Sort.by(direction, sortField));
    }
}