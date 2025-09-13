package com.example.score.service;

import com.example.score.common.PageResp;
import com.example.score.domain.ScoreDto;

public interface IScoreService {
    PageResp<ScoreDto> list(String userId, int page, int pageSize);
}
