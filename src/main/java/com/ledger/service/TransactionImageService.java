package com.ledger.service;

import com.ledger.entity.TransactionImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TransactionImageService {

    TransactionImage uploadImage(Long userId, Long accountId, MultipartFile file, Integer imageType);

    List<TransactionImage> listByAccountId(Long accountId);

    void deleteById(Long id);

    void deleteByAccountId(Long accountId);
}
