package com.example.score.api;

import com.example.score.common.ApiResp;
import com.example.score.common.PageResp;
import com.example.score.domain.ScoreDto;
import com.example.score.service.IScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final IScoreService scoreService;

    @GetMapping
    public ApiResp<PageResp<ScoreDto>> list(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResp.ok(scoreService.list(userId, page, pageSize));
    }
}
