package com.ledger.controller;

import com.ledger.common.result.Result;
import com.ledger.entity.TransactionImage;
import com.ledger.service.TransactionImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "交易图片附件", description = "交易图片上传、查询、删除")
public class TransactionImageController {

    private final TransactionImageService imageService;

    @PostMapping("/upload")
    @Operation(summary = "上传交易图片")
    public Result<TransactionImage> upload(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long accountId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "1") Integer imageType) {
        TransactionImage image = imageService.uploadImage(userId, accountId, file, imageType);
        return Result.success(image);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "查询账目下的所有图片")
    public Result<List<TransactionImage>> listByAccount(@PathVariable Long accountId) {
        return Result.success(imageService.listByAccountId(accountId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图片")
    public Result<Void> delete(@PathVariable Long id) {
        imageService.deleteById(id);
        return Result.success();
    }
}
