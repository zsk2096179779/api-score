package com.example.score.api;

import com.example.score.domain.ImpactRow;
import com.example.score.mapper.ScoreMapper;
import com.example.score.service.impl.ImpactAssembler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/scores")
@Slf4j
@RequiredArgsConstructor
public class ScoreControllerNested {

    @Autowired(required = false)
    private ScoreMapper scoreMapper;

    private final ImpactAssembler assembler = new ImpactAssembler();

    @Value("${feature.useDb:false}")
    private boolean useDb;

    private static final Pattern P_TIME14 = Pattern.compile("^\\d{14}$");
    private static final DateTimeFormatter FMT_14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @PostMapping("/nested")
    public Object getNested(@RequestBody TimeRange req) {
        String t1 = normalize(req.getTime1());
        String t2 = normalize(req.getTime2());
        if (t1 == null || t2 == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", 400);
            err.put("message", "time1/time2 需为 14 位时间串：yyyyMMddHHmmss");
            return err;
        }
        if (t1.compareTo(t2) > 0) { String tmp = t1; t1 = t2; t2 = tmp; }

        log.info("useDb={}, mapperPresent={}", useDb, (scoreMapper != null));

        if (!useDb) {
            // 本地演示（字段形状与正式一致）
            Map<String, Object> s1 = new LinkedHashMap<>();
            s1.put("markTime", t1);
            s1.put("name", "高炉稳定性指数");
            s1.put("code", "B4TEN_I");
            s1.put("weight", null);
            s1.put("value", null);
            s1.put("unit", null);
            s1.put("impactFactor", List.of());
            Map<String, Object> s2 = new LinkedHashMap<>(s1);
            s2.put("markTime", t2);
            return List.of(s1, s2);
        }

        if (scoreMapper == null) {
            return Map.of("code", 500, "message", "数据访问组件缺失");
        }

        List<ImpactRow> rows = scoreMapper.selectImpactRows(t1, t2);
        log.info("selectImpactRows size={}", (rows == null ? 0 : rows.size()));

        if (CollectionUtils.isEmpty(rows)) {
            // 窗内一个都没命中：按接口格式给两个空占位
            Map<String, Object> s1 = new LinkedHashMap<>();
            s1.put("markTime", t1);
            s1.put("name", "高炉稳定性指数");
            s1.put("code", "B4TEN_I");
            s1.put("weight", null);
            s1.put("value", null);
            s1.put("unit", null);
            s1.put("impactFactor", List.of());
            Map<String, Object> s2 = new LinkedHashMap<>(s1);
            s2.put("markTime", t2);
            return List.of(s1, s2);
        }

        // ★ 关键：两层结构（总指数 + 子指数五个）
        return assembler.assembleFirstTwoLayers(rows);
    }

    private String normalize(String in) {
        if (in == null) return null;
        String s = in.trim();
        if (!P_TIME14.matcher(s).matches()) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(
                        s.replace('T', ' '),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return ldt.format(FMT_14);
            } catch (Exception ignore) {
                return null;
            }
        }
        return s;
    }

    @Data
    public static class TimeRange {
        private String time1;
        private String time2;
    }
}
