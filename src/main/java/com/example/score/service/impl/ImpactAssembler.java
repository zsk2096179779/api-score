package com.example.score.service.impl;

import com.example.score.domain.ImpactRow;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组装三层 JSON：
 * 顶层（总指数B4TEN_I） -> 子指数[5个] -> 评价参数[按各子指数定义]
 * 注意：二层必须按“子指数自身”分组（SUBINDEX_NAME 或 SUBINDEX_ID），
 * 绝不能用展示用的 code（INDEX_ID=B4TEN_I），否则会被合并成一个。
 */
public class ImpactAssembler {

    /** 空快照（当该秒无数据时按接口示例返回占位） */
    public Map<String, Object> emptySnapshot(String markTime) {
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("markTime", markTime);
        top.put("name", "高炉稳定性指数");
        top.put("code", "B4TEN_I");
        top.put("weight", null);
        top.put("value", null);
        top.put("unit", null);
        top.put("impactFactor", new ArrayList<>());
        return top;
    }

    /** 顶层 + 子指数 + 评价参数（三层） */
    public List<Map<String, Object>> assembleTopSubAndParam(List<ImpactRow> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        // 1) 先按时间切片（SQL 已排序，这里仍保证稳定顺序）
        Map<String, List<ImpactRow>> byTime = rows.stream()
                .collect(Collectors.groupingBy(ImpactRow::getMarkTime, LinkedHashMap::new, Collectors.toList()));

        // 固定的子指数顺序（按名称），用于最终输出排序
        List<String> subOrder = Arrays.asList("下料稳定性","压量关系稳定性","炉缸工况稳定性","操作炉型稳定性","煤气流分布稳定性");

        List<Map<String, Object>> out = new ArrayList<>();

        for (Map.Entry<String, List<ImpactRow>> e : byTime.entrySet()) {
            String markTime = e.getKey();
            List<ImpactRow> slice = e.getValue();

            // 2) 顶层 value（同一时刻一致，取第一条非空）
            Object topValue = slice.stream()
                    .map(ImpactRow::getTopIndexValue)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            // 3) 二层：按“子指数自身身份”分组 —— 这里选 SUBINDEX_NAME（也可以换 SUBINDEX_ID）
            Map<String, List<ImpactRow>> bySub = slice.stream()
                    .filter(r -> r.getSubIndexName() != null) // 只聚合有子指数名的行
                    .collect(Collectors.groupingBy(
                            ImpactAssembler::keyBySub, // 分组键
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            // 4) 组装二层与三层
            //    用 LinkedHashMap 暂存，最后按 subOrder 排序输出
            Map<String, Map<String, Object>> subBucket = new LinkedHashMap<>();

            for (Map.Entry<String, List<ImpactRow>> se : bySub.entrySet()) {
                List<ImpactRow> subRows = se.getValue();
                ImpactRow head = subRows.get(0); // 同一子指数的任一行都携带相同的 name/code/weight/value

                Map<String, Object> sub = subBucket.computeIfAbsent(head.getSubIndexName(), k -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("name",   head.getSubIndexName());
                    // 按你的展示要求：子指数的 code 仍返回 INDEX_ID（=B4TEN_I）
                    node.put("code",   head.getSubIndexCode());
                    node.put("weight", head.getSubIndexWeight());
                    node.put("value",  head.getSubIndexValue()); // 允许为 null（该秒无值）
                    node.put("unit",   null);
                    node.put("impactFactor", new LinkedHashMap<String, Map<String, Object>>()); // 暂存 param map
                    return node;
                });

                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> paramMap =
                        (Map<String, Map<String, Object>>) sub.get("impactFactor");

                // 三层：按参数码唯一，占位保留（value 可能为 null）
                // SQL 已保证每个 (subIndex,param) 只出一条（或为空），这里再以 code 去重确保稳妥
                for (ImpactRow r : subRows) {
                    if (r.getParamCode() == null && r.getParamName() == null) continue;

                    paramMap.computeIfAbsent(safeKey(r.getParamCode()), k -> {
                        Map<String, Object> p = new LinkedHashMap<>();
                        p.put("name",   r.getParamName());
                        p.put("code",   r.getParamCode());
                        p.put("weight", r.getParamWeight());
                        p.put("value",  r.getParamValue()); // 允许为 null
                        p.put("unit",   null);
                        p.put("impactFactor", new ArrayList<>()); // 预留第四层
                        return p;
                    });
                }
            }

            // 5) 把 param map 还原成 list，并按 subOrder 排序输出子指数
            List<Map<String, Object>> subList = subBucket.values().stream()
                    .sorted(Comparator.comparing(
                            m -> orderIndex((String) m.get("name"), subOrder)
                    ))
                    .map(sub -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, Object>> paramMap =
                                (Map<String, Map<String, Object>>) sub.get("impactFactor");
                        sub.put("impactFactor", new ArrayList<>(paramMap.values()));
                        return sub;
                    })
                    .collect(Collectors.toList());

            // 6) 顶层节点
            Map<String, Object> top = new LinkedHashMap<>();
            top.put("markTime", markTime);
            top.put("name", "高炉稳定性指数");
            top.put("code", "B4TEN_I");
            top.put("weight", null);
            top.put("value", topValue);
            top.put("unit", null);
            top.put("impactFactor", subList);

            out.add(top);
        }

        // 按时间升序
        out.sort(Comparator.comparing(m -> Objects.toString(m.get("markTime"), "")));
        return out;
    }

    /** 分组键：建议用 SUBINDEX_NAME；如果你以后在 ImpactRow 里加了 subIndexId，也可以换成 ID 更稳妥 */
    private static String keyBySub(ImpactRow r) {
        // 若将来加了 getSubIndexId()：优先返回 ID，否则退回 name
        return safeKey(r.getSubIndexName());
    }

    private static String safeKey(String s) {
        return (s == null || s.isEmpty()) ? "<NULL>" : s;
    }

    private static int orderIndex(String name, List<String> order) {
        int i = order.indexOf(name);
        return i >= 0 ? i : 99;
    }
}
