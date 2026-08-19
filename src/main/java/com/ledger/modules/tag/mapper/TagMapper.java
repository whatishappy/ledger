package com.ledger.modules.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ledger.modules.tag.entity.Tag;
import com.ledger.modules.tag.vo.TagStatItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    List<TagStatItem> getTagStatistics(@Param("userId") Long userId,
                                        @Param("year") int year,
                                        @Param("month") int month);
}
