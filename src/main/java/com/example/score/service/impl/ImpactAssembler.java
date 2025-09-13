//package com.example.score.service.impl;
//
//import com.example.score.domain.ImpactRow;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * 组装三层 JSON：
// * 顶层（总指数B4TEN_I） -> 子指数[5个] -> 评价参数[按各子指数定义]
// * 注意：二层必须按“子指数自身”分组（SUBINDEX_NAME 或 SUBINDEX_ID），
// * 绝不能用展示用的 code（INDEX_ID=B4TEN_I），否则会被合并成一个。
// */
//public class ImpactAssembler {
//
//    /** 空快照（当该秒无数据时按接口示例返回占位） */
//    public Map<String, Object> emptySnapshot(String markTime) {
//        Map<String, Object> top = new LinkedHashMap<>();
//        top.put("markTime", markTime);
//        top.put("name", "高炉稳定性指数");
//        top.put("code", "B4TEN_I");
//        top.put("weight", null);
//        top.put("value", null);
//        top.put("unit", null);
//        top.put("impactFactor", new ArrayList<>());
//        return top;
//    }
//
//    /** 顶层 + 子指数 + 评价参数（三层） */
//    public List<Map<String, Object>> assembleTopSubAndParam(List<ImpactRow> rows) {
//        if (rows == null || rows.isEmpty()) return Collections.emptyList();
//
//        // 1) 先按时间切片（SQL 已排序，这里仍保证稳定顺序）
//        Map<String, List<ImpactRow>> byTime = rows.stream()
//                .collect(Collectors.groupingBy(ImpactRow::getMarkTime, LinkedHashMap::new, Collectors.toList()));
//
//        // 固定的子指数顺序（按名称），用于最终输出排序
//        List<String> subOrder = Arrays.asList("下料稳定性","压量关系稳定性","炉缸工况稳定性","操作炉型稳定性","煤气流分布稳定性");
//
//        List<Map<String, Object>> out = new ArrayList<>();
//
//        for (Map.Entry<String, List<ImpactRow>> e : byTime.entrySet()) {
//            String markTime = e.getKey();
//            List<ImpactRow> slice = e.getValue();
//
//            // 2) 顶层 value（同一时刻一致，取第一条非空）
//            Object topValue = slice.stream()
//                    .map(ImpactRow::getTopIndexValue)
//                    .filter(Objects::nonNull)
//                    .findFirst()
//                    .orElse(null);
//
//            // 3) 二层：按“子指数自身身份”分组 —— 这里选 SUBINDEX_NAME（也可以换 SUBINDEX_ID）
//            Map<String, List<ImpactRow>> bySub = slice.stream()
//                    .filter(r -> r.getSubIndexName() != null) // 只聚合有子指数名的行
//                    .collect(Collectors.groupingBy(
//                            ImpactAssembler::keyBySub, // 分组键
//                            LinkedHashMap::new,
//                            Collectors.toList()
//                    ));
//
//            // 4) 组装二层与三层
//            //    用 LinkedHashMap 暂存，最后按 subOrder 排序输出
//            Map<String, Map<String, Object>> subBucket = new LinkedHashMap<>();
//
//            for (Map.Entry<String, List<ImpactRow>> se : bySub.entrySet()) {
//                List<ImpactRow> subRows = se.getValue();
//                ImpactRow head = subRows.get(0); // 同一子指数的任一行都携带相同的 name/code/weight/value
//
//                Map<String, Object> sub = subBucket.computeIfAbsent(head.getSubIndexName(), k -> {
//                    Map<String, Object> node = new LinkedHashMap<>();
//                    node.put("name",   head.getSubIndexName());
//                    // 按你的展示要求：子指数的 code 仍返回 INDEX_ID（=B4TEN_I）
//                    node.put("code",   head.getSubIndexCode());
//                    node.put("weight", head.getSubIndexWeight());
//                    node.put("value",  head.getSubIndexValue()); // 允许为 null（该秒无值）
//                    node.put("unit",   null);
//                    node.put("impactFactor", new LinkedHashMap<String, Map<String, Object>>()); // 暂存 param map
//                    return node;
//                });
//
//                @SuppressWarnings("unchecked")
//                Map<String, Map<String, Object>> paramMap =
//                        (Map<String, Map<String, Object>>) sub.get("impactFactor");
//
//                // 三层：按参数码唯一，占位保留（value 可能为 null）
//                // SQL 已保证每个 (subIndex,param) 只出一条（或为空），这里再以 code 去重确保稳妥
//                for (ImpactRow r : subRows) {
//                    if (r.getParamCode() == null && r.getParamName() == null) continue;
//
//                    paramMap.computeIfAbsent(safeKey(r.getParamCode()), k -> {
//                        Map<String, Object> p = new LinkedHashMap<>();
//                        p.put("name",   r.getParamName());
//                        p.put("code",   r.getParamCode());
//                        p.put("weight", r.getParamWeight());
//                        p.put("value",  r.getParamValue()); // 允许为 null
//                        p.put("unit",   null);
//                        p.put("impactFactor", new ArrayList<>()); // 预留第四层
//                        return p;
//                    });
//                }
//            }
//
//            // 5) 把 param map 还原成 list，并按 subOrder 排序输出子指数
//            List<Map<String, Object>> subList = subBucket.values().stream()
//                    .sorted(Comparator.comparing(
//                            m -> orderIndex((String) m.get("name"), subOrder)
//                    ))
//                    .map(sub -> {
//                        @SuppressWarnings("unchecked")
//                        Map<String, Map<String, Object>> paramMap =
//                                (Map<String, Map<String, Object>>) sub.get("impactFactor");
//                        sub.put("impactFactor", new ArrayList<>(paramMap.values()));
//                        return sub;
//                    })
//                    .collect(Collectors.toList());
//
//            // 6) 顶层节点
//            Map<String, Object> top = new LinkedHashMap<>();
//            top.put("markTime", markTime);
//            top.put("name", "高炉稳定性指数");
//            top.put("code", "B4TEN_I");
//            top.put("weight", null);
//            top.put("value", topValue);
//            top.put("unit", null);
//            top.put("impactFactor", subList);
//
//            out.add(top);
//        }
//
//        // 按时间升序
//        out.sort(Comparator.comparing(m -> Objects.toString(m.get("markTime"), "")));
//        return out;
//    }
//
//    /** 分组键：建议用 SUBINDEX_NAME；如果你以后在 ImpactRow 里加了 subIndexId，也可以换成 ID 更稳妥 */
//    private static String keyBySub(ImpactRow r) {
//        // 若将来加了 getSubIndexId()：优先返回 ID，否则退回 name
//        return safeKey(r.getSubIndexName());
//    }
//
//    private static String safeKey(String s) {
//        return (s == null || s.isEmpty()) ? "<NULL>" : s;
//    }
//
//    private static int orderIndex(String name, List<String> order) {
//        int i = order.indexOf(name);
//        return i >= 0 ? i : 99;
//    }
//}
package com.example.score.service.impl;

import com.example.score.domain.ImpactRow;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组装三/四层 JSON：
 * 顶层（总指数B4TEN_I） -> 子指数[5个] -> 评价参数[按各子指数定义] -> 基础小参数（骨架 + 可回填值）
 *
 * 说明：
 * - 二层必须按“子指数自身”分组（SUBINDEX_NAME 或 SUBINDEX_ID），不要用展示用的 code（INDEX_ID=B4TEN_I）。
 * - 第三层无单位（unit=null）；第四层才有单位（来自 BASIC_PARAM_INFO）。
 * - 允许各层 value 为 null（窗口或源数据缺失时）。
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

    /**
     * 顶层 + 子指数 + 评价参数 + 基础小参数（四层）
     * SQL 已经把四层的字段（basicName/basicCode/basicUnit/basicValue/procParamId/blastName）拍平成行带回。
     * 这里负责把拍平结果组装成树状 JSON。
     */
    public List<Map<String, Object>> assembleTopSubAndParam(List<ImpactRow> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        // 1) 先按时间切片（SQL 通常已排序；此处仍保证稳定顺序）
        Map<String, List<ImpactRow>> byTime = rows.stream()
                .collect(Collectors.groupingBy(ImpactRow::getMarkTime, LinkedHashMap::new, Collectors.toList()));

        // 固定的子指数顺序（按名称），用于最终输出排序
        List<String> subOrder = Arrays.asList(
                "下料稳定性", "压量关系稳定性", "炉缸工况稳定性", "操作炉型稳定性", "煤气流分布稳定性"
        );

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

            // 4) 组装二层、三层、四层
            //    用 LinkedHashMap 暂存，最后按 subOrder 排序输出
            Map<String, Map<String, Object>> subBucket = new LinkedHashMap<>();

            for (Map.Entry<String, List<ImpactRow>> se : bySub.entrySet()) {
                List<ImpactRow> subRows = se.getValue();
                ImpactRow head = subRows.get(0); // 同一子指数的任一行都携带相同的 name/code/weight/value

                Map<String, Object> sub = subBucket.computeIfAbsent(head.getSubIndexName(), k -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("name",   head.getSubIndexName());
                    node.put("code",   head.getSubIndexCode());   // 展示用 code（如 B4TEN_I_XL 等）
                    node.put("weight", head.getSubIndexWeight());
                    node.put("value",  head.getSubIndexValue());  // 允许为 null（该秒无值）
                    node.put("unit",   null);
                    // 先用 map 暂存三层（参数），便于去重与填充；稍后转成 list
                    node.put("impactFactor", new LinkedHashMap<String, Map<String, Object>>());
                    return node;
                });

                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> paramMap =
                        (Map<String, Map<String, Object>>) sub.get("impactFactor");

                // 三层：按参数码唯一，占位保留（value 可能为 null）
                for (ImpactRow r : subRows) {
                    // 没有参数信息时跳过三层建模（但仍可能有顶/二层结构）
                    if (r.getParamCode() == null && r.getParamName() == null) continue;

                    Map<String, Object> p = paramMap.computeIfAbsent(safeKey(r.getParamCode()), k -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name",   r.getParamName());
                        m.put("code",   r.getParamCode());
                        m.put("weight", r.getParamWeight());
                        m.put("value",  r.getParamValue()); // 允许为 null
                        m.put("unit",   null);              // 第三层没有单位
                        // ⭐ 第四层容器：先用 List，但为去重稳妥，这里再加一个 shadow map
                        m.put("impactFactor", new ArrayList<Map<String, Object>>());
                        m.put("__basic_seen", new LinkedHashSet<String>()); // 仅内部使用，组装完会移除
                        return m;
                    });

                    // ⭐⭐ 第四层骨架：当 basicCode 非空，就挂到该参数下面
                    if (r.getBasicCode() != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> basics = (List<Map<String, Object>>) p.get("impactFactor");
                        @SuppressWarnings("unchecked")
                        Set<String> seen = (Set<String>) p.get("__basic_seen");

                        String basicKey = safeKey(r.getBasicCode());
                        if (!seen.contains(basicKey)) {
                            Map<String, Object> b = new LinkedHashMap<>();
                            b.put("name",   r.getBasicName());
                            b.put("code",   r.getBasicCode());
                            b.put("weight", null);                   // 第四层权重固定 null
                            b.put("value",  r.getBasicValue());      // 现阶段多为 null，后续批量补过程值
                            b.put("unit",   r.getBasicUnit());
                            // 如需把过程参数/高炉透出（可选字段）：
                            // b.put("procParamId", r.getProcParamId());
                            // b.put("blastName",   r.getBlastName());

                            basics.add(b);
                            seen.add(basicKey);
                        }
                    }
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

                        // 将 paramMap -> List，并清理内部辅助字段
                        List<Map<String, Object>> params = new ArrayList<>();
                        for (Map<String, Object> p : paramMap.values()) {
                            // 清理内部的 __basic_seen
                            p.remove("__basic_seen");
                            params.add(p);
                        }
                        sub.put("impactFactor", params);
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

    /** 分组键：建议用 SUBINDEX_NAME；如果以后在 ImpactRow 里加了 subIndexId，也可以换成 ID 更稳妥 */
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
