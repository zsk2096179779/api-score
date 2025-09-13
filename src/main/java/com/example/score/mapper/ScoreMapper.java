package com.example.score.mapper;

import com.example.score.domain.ImpactRow;
import com.example.score.domain.ScoreDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ScoreMapper {

    List<ScoreDto> selectScores(@Param("userId") String userId,
                                @Param("pageSize") int pageSize,
                                @Param("offset") int offset);

    int countScores(@Param("userId") String userId);

    // 两个时间点查询“总指数 + 子指数（五个占位）”
    List<ImpactRow> selectImpactRows(@Param("time1") String time1,
                                     @Param("time2") String time2);

    // 预留
    List<Map<String, Object>> selectProcessDataValuesBatch(@Param("table") String table,
                                                           @Param("time") String time,
                                                           @Param("paramIds") List<String> paramIds);
}
