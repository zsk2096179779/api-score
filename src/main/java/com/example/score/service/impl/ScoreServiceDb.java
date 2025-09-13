package com.example.score.service.impl;

import com.example.score.common.PageResp;
import com.example.score.domain.ScoreDto;
import com.example.score.mapper.ScoreMapper;
import com.example.score.service.IScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class ScoreServiceDb implements IScoreService {

    private final ScoreMapper mapper;

    @Override
    public PageResp<ScoreDto> list(String userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<ScoreDto> items = mapper.selectScores(userId, pageSize, offset);
        int total = mapper.countScores(userId);
        return PageResp.of(items, page, pageSize, total);
    }
}
