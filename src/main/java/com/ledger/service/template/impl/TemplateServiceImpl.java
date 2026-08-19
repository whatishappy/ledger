package com.ledger.service.template.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.cache.CacheService;
import com.ledger.common.enums.AccountTypeEnum;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.dto.template.TemplateApplyDTO;
import com.ledger.dto.template.TemplateCreateDTO;
import com.ledger.dto.template.TemplateUpdateDTO;
import com.ledger.entity.template.TransactionTemplate;
import com.ledger.event.template.TemplateChangedEvent;
import com.ledger.mapper.template.TransactionTemplateMapper;
import com.ledger.modules.account.dto.AccountAddRequest;
import com.ledger.modules.account.dto.AccountVO;
import com.ledger.modules.account.entity.Account;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.account.service.IAccountService;
import com.ledger.service.template.TemplateService;
import com.ledger.vo.template.TemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TransactionTemplateMapper templateMapper;
    private final AccountMapper accountMapper;
    private final IAccountService accountService;
    private final CacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public List<TemplateVO> listTemplates(Long userId, String keyword, Integer type, String sortBy, Integer page, Integer size) {
        List<TransactionTemplate> allTemplates = cacheService.getTemplates(userId, List.class);
        if (allTemplates == null) {
            LambdaQueryWrapper<TransactionTemplate> wrapper = new LambdaQueryWrapper<TransactionTemplate>()
                    .eq(TransactionTemplate::getUserId, userId);
            allTemplates = templateMapper.selectList(wrapper);
            cacheService.setTemplates(userId, allTemplates);
            log.debug("模板列表缓存写入: userId={}, count={}", userId, allTemplates.size());
        }

        List<TransactionTemplate> filtered = new ArrayList<>(allTemplates);

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.toLowerCase();
            filtered = filtered.stream()
                    .filter(t -> t.getName() != null && t.getName().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        if (type != null) {
            filtered = filtered.stream()
                    .filter(t -> type.equals(t.getType()))
                    .collect(Collectors.toList());
        }

        Comparator<TransactionTemplate> comparator = resolveComparator(sortBy);
        filtered.sort(comparator);

        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, filtered.size());
        List<TransactionTemplate> paged = from < filtered.size() ? filtered.subList(from, to) : new ArrayList<>();

        return paged.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateVO createTemplate(Long userId, TemplateCreateDTO dto) {
        LambdaQueryWrapper<TransactionTemplate> nameCheck = new LambdaQueryWrapper<TransactionTemplate>()
                .eq(TransactionTemplate::getUserId, userId)
                .eq(TransactionTemplate::getName, dto.getName());
        Long count = templateMapper.selectCount(nameCheck);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.TEMPLATE_NAME_EXISTS);
        }

        TransactionTemplate template = new TransactionTemplate();
        template.setUserId(userId);
        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setCategory(dto.getCategory());
        template.setAmount(dto.getAmount());
        template.setRemark(dto.getRemark());
        template.setTags(dto.getTags());
        LocalDateTime now = LocalDateTime.now();
        template.setCreatedAt(now);
        template.setUpdatedAt(now);

        templateMapper.insert(template);

        eventPublisher.publishEvent(new TemplateChangedEvent(userId));

        log.info("创建交易模板成功: userId={}, templateId={}, name={}", userId, template.getId(), template.getName());
        return toVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateVO updateTemplate(Long userId, Long id, TemplateUpdateDTO dto) {
        TransactionTemplate template = getTemplateOrThrow(userId, id);

        if (StringUtils.hasText(dto.getName()) && !dto.getName().equals(template.getName())) {
            LambdaQueryWrapper<TransactionTemplate> nameCheck = new LambdaQueryWrapper<TransactionTemplate>()
                    .eq(TransactionTemplate::getUserId, userId)
                    .eq(TransactionTemplate::getName, dto.getName());
            Long count = templateMapper.selectCount(nameCheck);
            if (count != null && count > 0) {
                throw new BusinessException(ResultCode.TEMPLATE_NAME_EXISTS);
            }
            template.setName(dto.getName());
        }

        if (dto.getType() != null) {
            template.setType(dto.getType());
        }
        if (dto.getCategory() != null) {
            template.setCategory(dto.getCategory());
        }
        if (dto.getAmount() != null) {
            template.setAmount(dto.getAmount());
        }
        if (dto.getRemark() != null) {
            template.setRemark(dto.getRemark());
        }
        if (dto.getTags() != null) {
            template.setTags(dto.getTags());
        }
        template.setUpdatedAt(LocalDateTime.now());

        templateMapper.updateById(template);

        eventPublisher.publishEvent(new TemplateChangedEvent(userId));

        log.info("更新交易模板成功: userId={}, templateId={}", userId, id);
        return toVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long userId, Long id) {
        TransactionTemplate template = getTemplateOrThrow(userId, id);
        templateMapper.deleteById(template.getId());

        eventPublisher.publishEvent(new TemplateChangedEvent(userId));

        log.info("删除交易模板成功: userId={}, templateId={}", userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO applyTemplate(Long userId, Long id, TemplateApplyDTO dto) {
        TransactionTemplate template = getTemplateOrThrow(userId, id);

        AccountAddRequest request = new AccountAddRequest();

        int accountType = convertTemplateTypeToAccountType(template.getType());
        request.setType(accountType);
        request.setCategory(template.getCategory());

        BigDecimal amount = dto.getAmount() != null ? dto.getAmount() : template.getAmount();
        request.setAmount(amount);

        String remark = dto.getRemark() != null ? dto.getRemark() : template.getRemark();
        request.setRemark(remark);

        LocalDate dateAt = dto.getDateAt() != null ? dto.getDateAt() : LocalDate.now();
        request.setAccountDate(dateAt);

        request.setExtraJson(buildExtraJsonFromTemplate(template));

        Long accountId = accountService.addAccount(userId, request);

        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        return toAccountVO(account);
    }

    private TransactionTemplate getTemplateOrThrow(Long userId, Long id) {
        LambdaQueryWrapper<TransactionTemplate> wrapper = new LambdaQueryWrapper<TransactionTemplate>()
                .eq(TransactionTemplate::getId, id)
                .eq(TransactionTemplate::getUserId, userId);
        TransactionTemplate template = templateMapper.selectOne(wrapper);
        if (template == null) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    private Comparator<TransactionTemplate> resolveComparator(String sortBy) {
        if (sortBy == null) {
            return Comparator.comparing(TransactionTemplate::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        switch (sortBy) {
            case "name":
                return Comparator.comparing(TransactionTemplate::getName, Comparator.nullsLast(String::compareTo));
            case "amount":
                return Comparator.comparing(TransactionTemplate::getAmount, Comparator.nullsLast(Comparator.reverseOrder()));
            case "updatedAt":
                return Comparator.comparing(TransactionTemplate::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case "createdAt":
            default:
                return Comparator.comparing(TransactionTemplate::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
    }

    private TemplateVO toVO(TransactionTemplate template) {
        return TemplateVO.builder()
                .id(template.getId())
                .userId(template.getUserId())
                .name(template.getName())
                .type(template.getType())
                .category(template.getCategory())
                .amount(template.getAmount())
                .remark(template.getRemark())
                .tags(template.getTags())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .applyExample(buildApplyExample(template))
                .build();
    }

    private AccountVO buildApplyExample(TransactionTemplate template) {
        if (template.getType() == null && template.getCategory() == null && template.getAmount() == null) {
            return null;
        }
        return new AccountVO(
                null,
                convertTemplateTypeToAccountType(template.getType()),
                template.getCategory(),
                template.getAmount(),
                LocalDate.now(),
                template.getRemark(),
                1,
                null,
                null
        );
    }

    private int convertTemplateTypeToAccountType(Integer templateType) {
        if (templateType == null) {
            return AccountTypeEnum.EXPENSE.getValue();
        }
        switch (templateType) {
            case 1:
                return AccountTypeEnum.EXPENSE.getValue();
            case 2:
                return AccountTypeEnum.INCOME.getValue();
            default:
                return AccountTypeEnum.EXPENSE.getValue();
        }
    }

    private String buildExtraJsonFromTemplate(TransactionTemplate template) {
        try {
            Map<String, Object> extra = new HashMap<>();
            extra.put("templateId", template.getId());
            extra.put("templateName", template.getName());
            if (template.getTags() != null && !template.getTags().isEmpty()) {
                extra.put("tags", template.getTags());
            }
            return objectMapper.writeValueAsString(extra);
        } catch (JsonProcessingException e) {
            log.warn("序列化模板extraJson失败: templateId={}", template.getId(), e);
            return null;
        }
    }

    private AccountVO toAccountVO(Account account) {
        return new AccountVO(
                account.getId(),
                account.getType(),
                account.getCategory(),
                account.getAmount(),
                account.getAccountDate(),
                account.getRemark(),
                account.getVersion(),
                account.getCreateTime(),
                account.getUpdateTime()
        );
    }
}
