package com.ledger.modules.ai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiOcrResultDTO {

    private String merchant;

    private String date;

    private BigDecimal total;

    private String paymentMethod;

    private String category;

    private List<ReceiptItem> items;

    private String imageUrl;

    @Data
    public static class ReceiptItem {
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }
}
