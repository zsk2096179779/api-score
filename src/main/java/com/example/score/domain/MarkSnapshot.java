package com.example.score.domain;

import lombok.Data;
import java.util.List;

@Data
public class MarkSnapshot {
    private String markTime;               // YYYYMMDDhhmmss
    private List<ImpactNode> impactFactor; // 顶层节点（大类）
}
