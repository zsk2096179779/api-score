package com.example.score.domain;

import lombok.*;

import java.math.BigDecimal;

/** 扁平行：同一时间点、某子指数-评价参数的一行聚合字段（不含基础参数层） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpactRow {
    // 顶层（从 MODEL_DATA_INDEX 推导）
    private String     markTime;        // yyyyMMddHHmmss
    private BigDecimal topIndexValue;   // 顶层总指数值（INDEX_VALUE）

    // 子指数（第二层）
    private String     subIndexName;    // SUBINDEX_INFO.SUBINDEX_NAME
    private String     subIndexCode;    // SUBINDEX_INFO.INDEX_ID（按你们对外编码要求）
    private BigDecimal subIndexWeight;  // SUBINDEX_INFO.WEIGHT
    private BigDecimal subIndexValue;   // DATA_SUBINDEX.SUBINDEX_VALUE（同秒对齐）

    // 评价参数（第三层）
    private String     paramName;       // PARAM_SCORE_INFO.NAME
    private String     paramCode;       // PARAM_SCORE_ID
    private BigDecimal paramWeight;     // MODEL_PARAM_WEIGHT.WEIGHT
    private BigDecimal paramValue;      // PARAM_SCORE_DATA.DATA_VALUE（FETCH_TIME 同秒对齐）

    // 以下字段保留给后续第四层（基础参数）扩展，当前不使用
    private String     basicName;
    private String     basicCode;
    private String     basicUnit;
    private BigDecimal basicValue;

    // 动态扩展（暂不用）
    private String     blastName;
    private String     procParamId;

    @Setter
    @Getter
    private Integer exceptionFlag;  // 0=正常，1=休风

}
