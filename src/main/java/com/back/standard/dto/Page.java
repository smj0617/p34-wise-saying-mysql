package com.back.standard.dto;

import java.util.List;

// 한 페이지 분량의 결과와, 페이지 메뉴를 그리는 데 필요한 정보를 함께 담는다.
public record Page<T>(List<T> content, int totalCount, int page, int pageSize) {

    // 결과가 0개여도 1페이지는 있다고 본다.
    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}
