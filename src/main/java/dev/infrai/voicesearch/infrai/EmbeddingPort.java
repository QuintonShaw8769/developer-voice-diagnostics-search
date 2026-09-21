package dev.infrai.voicesearch.infrai;

import java.util.List;

public interface EmbeddingPort {
    List<Float> embed(String text);
}
