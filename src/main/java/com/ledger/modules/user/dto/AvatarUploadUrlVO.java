package com.ledger.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvatarUploadUrlVO {

    private String uploadUrl;

    private String objectKey;

    private String method;

    private int expiresIn;
}
