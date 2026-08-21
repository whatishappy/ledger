package com.ledger.modules.tag.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.tag.dto.TagCreateDTO;
import com.ledger.modules.tag.dto.TagUpdateDTO;
import com.ledger.modules.tag.service.TagService;
import com.ledger.modules.tag.vo.TagStatisticsVO;
import com.ledger.modules.tag.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签模块", description = "交易标签CRUD、分配、统计（TG-01~TG-07）")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "TG-01 查询标签列表")
    public Result<List<TagVO>> listTags(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String sortBy) {
        Long userId = UserContext.requireUserId();
        List<TagVO> tags = tagService.listTags(userId, name, type, sortBy);
        return Result.success(tags);
    }

    @PostMapping
    @Operation(summary = "TG-02 创建标签")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<TagVO> createTag(@Valid @RequestBody TagCreateDTO dto) {
        Long userId = UserContext.requireUserId();
        TagVO tagVO = tagService.createTag(userId, dto);
        return Result.success(tagVO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "TG-03 更新标签")
    public Result<TagVO> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagUpdateDTO dto) {
        Long userId = UserContext.requireUserId();
        TagVO tagVO = tagService.updateTag(userId, id, dto);
        return Result.success(tagVO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "TG-04 删除标签")
    public Result<Void> deleteTag(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        tagService.deleteTag(userId, id);
        return Result.success();
    }

    @PutMapping("/accounts/{accountId}")
    @Operation(summary = "TG-05 分配标签到账目")
    public Result<Void> assignTagsToAccount(
            @PathVariable Long accountId,
            @RequestBody(required = false) List<Long> tagIds) {
        Long userId = UserContext.requireUserId();
        tagService.assignTagsToAccount(userId, accountId, tagIds);
        return Result.success();
    }

    @GetMapping("/accounts/{accountId}")
    @Operation(summary = "TG-06 查询账目关联的标签")
    public Result<List<TagVO>> getTagsByAccount(@PathVariable Long accountId) {
        Long userId = UserContext.requireUserId();
        List<TagVO> tags = tagService.getTagsByAccount(userId, accountId);
        return Result.success(tags);
    }

    @GetMapping("/statistics")
    @Operation(summary = "TG-07 标签月度统计")
    public Result<TagStatisticsVO> getTagStatistics(
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = UserContext.requireUserId();
        TagStatisticsVO statistics = tagService.getTagStatistics(userId, year, month);
        return Result.success(statistics);
    }
}
