package com.ledger.modules.tag.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TagChangedEvent {

    private Long userId;
}
