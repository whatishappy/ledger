package com.ledger.modules.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.common.cache.CacheService;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.tag.dto.TagCreateDTO;
import com.ledger.modules.tag.dto.TagUpdateDTO;
import com.ledger.modules.tag.entity.AccountTag;
import com.ledger.modules.tag.entity.Tag;
import com.ledger.modules.tag.event.TagChangedEvent;
import com.ledger.modules.tag.mapper.AccountTagMapper;
import com.ledger.modules.tag.mapper.TagMapper;
import com.ledger.modules.tag.service.TagService;
import com.ledger.modules.tag.vo.TagStatItem;
import com.ledger.modules.tag.vo.TagStatisticsVO;
import com.ledger.modules.tag.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final AccountTagMapper accountTagMapper;
    private final AccountMapper accountMapper;
    private final CacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<TagVO> listTags(Long userId, String name, Integer type, String sortBy) {
        List<TagVO> allCached = cacheService.getTags(userId, List.class);
        List<TagVO> allTags;
        if (allCached != null && !allCached.isEmpty()) {
            log.debug("标签列表缓存命中: userId={}", userId);
            allTags = allCached;
        } else {
            LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getUserId, userId);
            List<Tag> tags = tagMapper.selectList(wrapper);
            allTags = tags.stream().map(this::toVO).collect(Collectors.toList());
            cacheService.setTags(userId, allTags);
        }
        return filterAndSort(allTags, name, type, sortBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO createTag(Long userId, TagCreateDTO dto) {
        Long sameNameCount = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getName, dto.getName()));
        if (sameNameCount != null && sameNameCount > 0) {
            throw new BusinessException(ResultCode.TAG_NAME_ALREADY_EXISTS);
        }
        Long totalCount = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId));
        if (totalCount != null && totalCount >= 50) {
            throw new BusinessException(ResultCode.TAG_LIMIT_EXCEEDED);
        }
        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(dto.getName());
        tag.setColor(dto.getColor());
        tag.setType(dto.getType() != null ? dto.getType() : 0);
        tag.setSort(dto.getSort() != null ? dto.getSort() : 0);
        tag.setCreatedAt(LocalDateTime.now());
        tagMapper.insert(tag);
        eventPublisher.publishEvent(new TagChangedEvent(userId));
        log.info("创建标签成功: userId={}, tagId={}, name={}", userId, tag.getId(), tag.getName());
        return toVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO updateTag(Long userId, Long id, TagUpdateDTO dto) {
        Tag tag = getTagAndValidate(userId, id);
        if (dto.getName() != null && !dto.getName().equals(tag.getName())) {
            Long sameNameCount = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getUserId, userId)
                    .eq(Tag::getName, dto.getName())
                    .ne(Tag::getId, id));
            if (sameNameCount != null && sameNameCount > 0) {
                throw new BusinessException(ResultCode.TAG_NAME_ALREADY_EXISTS);
            }
            tag.setName(dto.getName());
        }
        if (dto.getColor() != null) {
            tag.setColor(dto.getColor());
        }
        if (dto.getType() != null) {
            tag.setType(dto.getType());
        }
        if (dto.getSort() != null) {
            tag.setSort(dto.getSort());
        }
        tagMapper.updateById(tag);
        eventPublisher.publishEvent(new TagChangedEvent(userId));
        log.info("更新标签成功: userId={}, tagId={}", userId, id);
        return toVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long userId, Long id) {
        getTagAndValidate(userId, id);
        accountTagMapper.delete(new LambdaQueryWrapper<AccountTag>()
                .eq(AccountTag::getTagId, id));
        tagMapper.deleteById(id);
        eventPublisher.publishEvent(new TagChangedEvent(userId));
        log.info("删除标签成功: userId={}, tagId={}", userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTagsToAccount(Long userId, Long accountId, List<Long> tagIds) {
        if (!validateAccountOwnership(userId, accountId)) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        accountTagMapper.delete(new LambdaQueryWrapper<AccountTag>()
                .eq(AccountTag::getAccountId, accountId));
        if (tagIds != null && !tagIds.isEmpty()) {
            List<Long> validTagIds = tagIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            for (Long tagId : validTagIds) {
                getTagAndValidate(userId, tagId);
                AccountTag at = new AccountTag();
                at.setAccountId(accountId);
                at.setTagId(tagId);
                accountTagMapper.insert(at);
            }
        }
        eventPublisher.publishEvent(new TagChangedEvent(userId));
        log.info("分配标签到账目成功: userId={}, accountId={}, tagIds={}", userId, accountId, tagIds);
    }

    @Override
    public List<TagVO> getTagsByAccount(Long userId, Long accountId) {
        if (!validateAccountOwnership(userId, accountId)) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        List<AccountTag> accountTags = accountTagMapper.selectList(
                new LambdaQueryWrapper<AccountTag>().eq(AccountTag::getAccountId, accountId));
        if (accountTags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = accountTags.stream()
                .map(AccountTag::getTagId)
                .collect(Collectors.toList());
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        return tags.stream()
                .filter(t -> t.getUserId().equals(userId))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public TagStatisticsVO getTagStatistics(Long userId, int year, int month) {
        validateYearMonth(year, month);
        List<TagStatItem> items = tagMapper.getTagStatistics(userId, year, month);
        if (items == null) {
            items = Collections.emptyList();
        }
        String monthStr = YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return TagStatisticsVO.builder()
                .month(monthStr)
                .items(items)
                .build();
    }

    private Tag getTagAndValidate(Long userId, Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || !tag.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.TAG_NOT_FOUND);
        }
        return tag;
    }

    private boolean validateAccountOwnership(Long userId, Long accountId) {
        var account = accountMapper.selectById(accountId);
        return account != null && account.getUserId().equals(userId);
    }

    private void validateYearMonth(int year, int month) {
        try {
            YearMonth.of(year, month);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "年份或月份无效");
        }
    }

    private List<TagVO> filterAndSort(List<TagVO> list, String name, Integer type, String sortBy) {
        List<TagVO> result = new ArrayList<>(list);
        if (name != null && !name.isBlank()) {
            result = result.stream()
                    .filter(t -> t.getName() != null && t.getName().contains(name))
                    .collect(Collectors.toList());
        }
        if (type != null) {
            result = result.stream()
                    .filter(t -> type.equals(t.getType()) || (t.getType() != null && t.getType() == 0))
                    .collect(Collectors.toList());
        }
        if ("name".equalsIgnoreCase(sortBy)) {
            result.sort((a, b) -> {
                String na = a.getName() == null ? "" : a.getName();
                String nb = b.getName() == null ? "" : b.getName();
                return na.compareTo(nb);
            });
        } else if ("createdAt".equalsIgnoreCase(sortBy)) {
            result.sort((a, b) -> {
                LocalDateTime ta = a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt();
                LocalDateTime tb = b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt();
                return tb.compareTo(ta);
            });
        } else {
            result.sort((a, b) -> {
                Integer sa = a.getSort() == null ? 0 : a.getSort();
                Integer sb = b.getSort() == null ? 0 : b.getSort();
                return sa.compareTo(sb);
            });
        }
        return result;
    }

    private TagVO toVO(Tag tag) {
        return TagVO.builder()
                .id(tag.getId())
                .userId(tag.getUserId())
                .name(tag.getName())
                .color(tag.getColor())
                .type(tag.getType())
                .sort(tag.getSort())
                .createdAt(tag.getCreatedAt())
                .totalAmount(BigDecimal.ZERO)
                .build();
    }
}
