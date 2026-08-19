package com.ledger.service.template;

import com.ledger.dto.template.TemplateApplyDTO;
import com.ledger.dto.template.TemplateCreateDTO;
import com.ledger.dto.template.TemplateUpdateDTO;
import com.ledger.modules.account.dto.AccountVO;
import com.ledger.vo.template.TemplateVO;

import java.util.List;

public interface TemplateService {

    List<TemplateVO> listTemplates(Long userId, String keyword, Integer type, String sortBy, Integer page, Integer size);

    TemplateVO createTemplate(Long userId, TemplateCreateDTO dto);

    TemplateVO updateTemplate(Long userId, Long id, TemplateUpdateDTO dto);

    void deleteTemplate(Long userId, Long id);

    AccountVO applyTemplate(Long userId, Long id, TemplateApplyDTO dto);
}
