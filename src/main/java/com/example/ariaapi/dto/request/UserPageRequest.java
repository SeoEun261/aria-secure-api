package com.example.ariaapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Data
public class UserPageRequest {

    @Min(value = 1, message = "페이지는 1이상이어야 합니다.")
    private int page = 1;

    @Max(value = 30, message = "페이지 크기는 30을 넘을 수 없습니다.")
    @Min(value = 1, message = "페이지 크기는 1이상이여야 합니다.")
    private int size = 10;

    // 기본 정렬 기준은 'id' 필드의 내림차순입니다.
    private String sort = "id,desc";

    // 사용자의 정렬 요청을 처리하는 로직
    public void setSort(String sort) {
        if (sort != null && !sort.isEmpty()) {
            this.sort = sort;
        }
    }

    /**
     * DTO의 정보를 기반으로 Spring Data의 PageRequest 객체를 생성합니다.
     * sort 파라미터에서 필드명과 정렬 방향을 파싱합니다.
     */
    public PageRequest makePageRequest() {
        // sort 파라미터를 쉼표(,)로 분리합니다.
        String[] sortParams = this.sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = Sort.Direction.DESC; // 기본 정렬 방향은 내림차순(DESC)

        // 정렬 방향이 지정되었는지 확인합니다.
        if (sortParams.length > 1) {
            try {
                // "asc" 또는 "desc" 문자열을 Sort.Direction 객체로 변환합니다.
                direction = Sort.Direction.fromString(sortParams[1]);
            } catch (IllegalArgumentException e) {
                // 잘못된 정렬 방향이 들어왔을 경우, 기본값인 내림차순(DESC)을 사용합니다.
            }
        }

        // PageRequest는 0-based이므로 page-1로 설정합니다.
        return PageRequest.of(page - 1, size, Sort.by(direction, sortField));
    }
}