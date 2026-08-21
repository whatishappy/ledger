package com.ledger.event.template;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TemplateChangedEvent {

    private Long userId;
}
