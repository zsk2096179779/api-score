package com.example.score.service.impl;

import com.example.score.common.PageResp;
import com.example.score.domain.ScoreDto;
import com.example.score.service.IScoreService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Profile("mock")
public class ScoreServiceMock implements IScoreService {
    @Override
    public PageResp<ScoreDto> list(String userId, int page, int pageSize) {
        ScoreDto s = new ScoreDto();
        s.setId("sc_1001");
        s.setUserId(userId != null ? userId : "u_001");
        s.setTargetId("order_888");
        s.setScore(4.5);
        s.setComment("服务不错");
        s.setCreatedAt("2025-09-10 08:12:01");

        List<ScoreDto> items = Collections.singletonList(s);
        return PageResp.of(items, page, pageSize, 1);
    }
}
