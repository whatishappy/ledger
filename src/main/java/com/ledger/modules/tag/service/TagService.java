package com.ledger.modules.tag.service;

import com.ledger.modules.tag.dto.TagCreateDTO;
import com.ledger.modules.tag.dto.TagUpdateDTO;
import com.ledger.modules.tag.vo.TagStatisticsVO;
import com.ledger.modules.tag.vo.TagVO;

import java.util.List;

public interface TagService {

    List<TagVO> listTags(Long userId, String name, Integer type, String sortBy);

    TagVO createTag(Long userId, TagCreateDTO dto);

    TagVO updateTag(Long userId, Long id, TagUpdateDTO dto);

    void deleteTag(Long userId, Long id);

    void assignTagsToAccount(Long userId, Long accountId, List<Long> tagIds);

    List<TagVO> getTagsByAccount(Long userId, Long accountId);

    TagStatisticsVO getTagStatistics(Long userId, int year, int month);
}
