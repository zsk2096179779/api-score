package com.example.score.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 扁平行：同一时间点的一行聚合字段（此阶段只用到顶层总指数+子指数） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpactRow {
    // 顶层
    private String markTime;               // yyyyMMddHHmmss（SQL 中已格式化）
    private BigDecimal topIndexValue;      // ★ 新增：总指数 value = MODEL_DATA_INDEX.INDEX_VALUE

    // 子指数（第二层）
    private String     subIndexName;
    private String     subIndexCode;       // SUBINDEX_INFO.INDEX_ID（按你的要求）
    private BigDecimal subIndexWeight;
    private BigDecimal subIndexValue;

    // 以下为将来扩展预留（本阶段不使用）
    private String     paramName;
    private String     paramCode;
    private BigDecimal paramWeight;
    private BigDecimal paramValue;
    private String     basicName;
    private String     basicCode;
    private String     basicUnit;
    private BigDecimal basicValue;
    private String     blastName;
    private String     procParamId;
}
