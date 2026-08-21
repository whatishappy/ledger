package com.ledger.modules.imports.service;

import com.ledger.modules.imports.dto.BillImportConfirmDTO;
import com.ledger.modules.imports.vo.BillImportPreviewVO;
import com.ledger.modules.imports.vo.BillImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IBillImportService {

    BillImportPreviewVO preview(Long userId, String source, MultipartFile file);

    BillImportResultVO confirm(Long userId, BillImportConfirmDTO dto);

    Map<String, Integer> getImportStatus();
}
