package com.ledger.modules.tag.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagStatisticsVO {

    private String month;

    private List<TagStatItem> items;
}
