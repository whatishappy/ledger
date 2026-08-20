package com.ledger.modules.ai.service;

import java.util.List;

public interface EmbeddingService {

    List<Float> embed(String text);

    List<float[]> batchEmbed(List<String> texts);
}
