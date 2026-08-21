package com.ledger.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAvatarRequest {

    @NotBlank(message = "objectKey不能为空")
    private String objectKey;
}
