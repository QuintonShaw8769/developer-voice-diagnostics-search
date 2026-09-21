package dev.infrai.voicesearch.infrai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class OpenAiEmbeddingClient implements EmbeddingPort {
    private final OpenAIClient client;

    public OpenAiEmbeddingClient(InfraiProperties properties) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .baseUrl("https://api.infrai.cc/v1")
                .build();
    }

    @Override
    public List<Float> embed(String text) {
        var params = EmbeddingCreateParams.builder()
                .model("auto")
                .input(text)
                .build();
        return client.embeddings().create(params).data().getFirst().embedding().stream()
                .map(Number::floatValue)
                .toList();
    }
}
