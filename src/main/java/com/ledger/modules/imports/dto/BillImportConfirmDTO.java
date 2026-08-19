package com.ledger.modules.imports.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BillImportConfirmDTO {

    private String token;

    private Map<String, String> categoryOverrides;

    private Boolean skipConflicts = true;
}
