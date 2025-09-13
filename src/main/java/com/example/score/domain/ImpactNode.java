package com.example.score.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImpactNode {
    private String name;
    private String code;
    private BigDecimal weight; // 可为 null
    private BigDecimal value;  // 可为 null
    private String unit;       // 可为 null
    private List<ImpactNode> impactFactor = new ArrayList<>();
}
