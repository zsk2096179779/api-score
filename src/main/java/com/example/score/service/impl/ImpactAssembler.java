package com.example.score.service.impl;

import com.example.score.domain.ImpactRow;
import com.example.score.mapper.ScoreMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组装为接口文档要求的结构：
 * 第一级：markTime + impactFactor[ {name:"高炉稳定性指数", code:"B4TEN_I", value:顶层值, weight:null, unit:null, impactFactor:[子指数…]} ]
 * 第二级：子指数（五个），字段严格为 name/code/weight/value/unit/null impactFactor:[]
 */
public class ImpactAssembler {

    /** 只拼前两级（总指数 + 五个子指数） */
    public List<Map<String, Object>> assembleFirstTwoLayers(List<ImpactRow> rows) {
        // 按时间分组
        Map<String, List<ImpactRow>> byTime = rows.stream()
                .collect(Collectors.groupingBy(ImpactRow::getMarkTime, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> out = new ArrayList<>();

        for (Map.Entry<String, List<ImpactRow>> e : byTime.entrySet()) {
            String markTime = e.getKey();
            List<ImpactRow> group = e.getValue();

            // 顶层 value：取该时刻任一行中的 topIndexValue（SQL 已保证同一时刻相同）
            // 若整组都为 null，value 也为 null（保持与真实数据一致）
            Object topValue = group.stream()
                    .map(ImpactRow::getTopIndexValue)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            // 子指数：按出现顺序去重（SQL 已做排序控制），仅保留非空 name
            List<Map<String, Object>> subList = new ArrayList<>();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (ImpactRow r : group) {
                if (r.getSubIndexName() == null) continue;
                String key = r.getSubIndexName() + "|" + r.getSubIndexCode();
                if (!seen.add(key)) continue;

                Map<String, Object> sub = new LinkedHashMap<>();
                sub.put("name",   r.getSubIndexName());
                sub.put("code",   r.getSubIndexCode());
                sub.put("weight", r.getSubIndexWeight());
                sub.put("value",  r.getSubIndexValue());
                sub.put("unit",   null);
                sub.put("impactFactor", new ArrayList<>()); // 第三、四级此阶段为空
                subList.add(sub);
            }

            // 第一级：一个时刻只放一个“总指数”节点
            Map<String, Object> topNode = new LinkedHashMap<>();
            topNode.put("name",   "高炉稳定性指数");
            topNode.put("code",   "B4TEN_I");
            topNode.put("weight", null);
            topNode.put("value",  topValue);
            topNode.put("unit",   null);
            topNode.put("impactFactor", subList);

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("markTime", markTime);
            snapshot.put("impactFactor", List.of(topNode));

            out.add(snapshot);
        }

        // 结果按时间正序
        out.sort(Comparator.comparing(m -> Objects.toString(m.get("markTime"), "")));
        return out;
    }

    // 其余旧方法保留（如果不再使用，可以删除或标 @Deprecated）
}
