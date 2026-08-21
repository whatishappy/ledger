package com.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ledger.entity.TransactionImage;
import com.ledger.mapper.TransactionImageMapper;
import com.ledger.service.TransactionImageService;
import com.ledger.service.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionImageServiceImpl implements TransactionImageService {

    private final TransactionImageMapper imageMapper;
    private final MinioStorageService minioStorageService;

    @Override
    @Transactional
    public TransactionImage uploadImage(Long userId, Long accountId, MultipartFile file, Integer imageType) {
        String objectKey = "accounts/" + accountId + "/" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + getExtension(file);

        try {
            minioStorageService.uploadObject(objectKey, file.getInputStream(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("上传图片到MinIO失败", e);
        }

        String imageUrl = minioStorageService.getPublicUrl(objectKey);

        TransactionImage image = new TransactionImage();
        image.setAccountId(accountId);
        image.setImageUrl(imageUrl);
        image.setImageType(imageType != null ? imageType : 1);
        image.setCreatedAt(LocalDateTime.now());
        imageMapper.insert(image);

        log.info("交易图片上传成功: userId={}, accountId={}, imageId={}", userId, accountId, image.getId());
        return image;
    }

    @Override
    public List<TransactionImage> listByAccountId(Long accountId) {
        LambdaQueryWrapper<TransactionImage> wrapper = new LambdaQueryWrapper<TransactionImage>()
                .eq(TransactionImage::getAccountId, accountId)
                .orderByDesc(TransactionImage::getCreatedAt);
        return imageMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TransactionImage image = imageMapper.selectById(id);
        if (image != null) {
            imageMapper.deleteById(id);
            try {
                String objectKey = extractObjectKey(image.getImageUrl());
                if (objectKey != null) {
                    minioStorageService.deleteObject(objectKey);
                }
            } catch (Exception e) {
                log.warn("删除MinIO文件失败(不影响DB删除): imageId={}", id, e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteByAccountId(Long accountId) {
        List<TransactionImage> images = listByAccountId(accountId);
        for (TransactionImage image : images) {
            deleteById(image.getId());
        }
    }

    private String getExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
    }

    private String extractObjectKey(String imageUrl) {
        if (imageUrl == null) return null;
        int lastSlash = imageUrl.lastIndexOf("/");
        if (lastSlash > 0) {
            return imageUrl.substring(lastSlash + 1);
        }
        return null;
    }
}
