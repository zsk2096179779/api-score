package com.example.score.common;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class PageResp<T> {
    private List<T> items;
    private int page;
    private int pageSize;
    private int total;

    public static <T> PageResp<T> of(List<T> items, int page, int pageSize, int total) {
        return new PageResp<>(items, page, pageSize, total);
    }
}
