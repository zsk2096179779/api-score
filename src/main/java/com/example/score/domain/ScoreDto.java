package com.example.score.domain;

import lombok.Data;

@Data
public class ScoreDto {
    private String id;
    private String userId;
    private String targetId;
    private Double score;
    private String comment;
    private String createdAt; // 也可用 LocalDateTime，Jackson 已设定格式
}
