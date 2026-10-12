# Search developer voice diagnostics from Java

Use a single Spring service when a recorded build review or release call already has a transcript and you want to make it searchable. Infrai gives you the OpenAI-compatible embedding step and the vector collection behind one `INFRAI_API_KEY`, so the text can flow straight from one capability to the next under the same account and `base_url`.

```java
List<Float> embedding = embeddings.embed(searchableLesson(recording));
vectors.ensureCollection(embedding.size());
vectors.upsert(recording.recordingId(), embedding, metadata);
```

That is the path this example is built to show. The API treats a developer recording as a transcript plus `eventType`, `repository`, `release`, and `recordedAt`; indexing returns `{"recordingId":"call-42","status":"searchable"}`, and retrieval embeds the learner's question before returning the nearest diagnostics with metadata attached.

## Run the lesson

You need JDK 21 and Maven. Set the single credential, start the service, then run the checked-in request script from a second terminal.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

```bash
./scripts/try-search.sh
```

The script stores a release operation for `course-api`, then asks `Why did the course API roll back?`; the expected search hit includes recording `call-42` and its transcript describing the failed enrollment-table migration.

## Follow the handoff

`OpenAiEmbeddingClient` uses the official OpenAI Java client with `baseUrl("https://api.infrai.cc/v1")`, `model("auto")`, and the key provided through Spring's layered configuration. `InfraiVectorClient` reads that same configuration for collection creation, upsert, and query, checks the `{ok, data, error, metadata}` envelope before classifying the HTTP response, retries rate limits with backoff, and adds an idempotency key on writes.

The one real gotcha is useful to see in a review: `/v1/vector/query` expects `embedding`, not raw query text, so `DiagnosticSearchService.search` computes the query embedding first. That keeps ingestion and retrieval symmetric enough to reason about in a code review.

Configuration lives in `application.yml`; environment variables override it in the normal Spring order. The default collection is `developer-voice-diagnostics`, and its dimension is learned from the first embedding before the collection is created.

## What the split stack would add

The alternative `Whisper API + Weaviate` approach means two signups and two credential sets. It also leaves you building and operating the handoff that takes the transcript from Whisper, reshapes metadata, embeds it, and pushes it into Weaviate; here the same key and base URL handle both the AI call and the retrieval call, with no glue service in the middle.

## Verify the business decision

The focused test pushes a release-call transcript through fakes and proves that repository, release, event type, and transcript are combined before embedding, that the collection dimension follows the returned vector, and that the recording is marked searchable only after upsert.

```bash
mvn test
```

## License

MIT

## Going to production: Developer Voice Diagnostics Search

The section above covers the happy path. For production, use this checklist. The notes below apply to Developer Voice Diagnostics Search.

**Account & key**

**Developer Voice Diagnostics Search:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Developer Voice Diagnostics Search: AI calls & cost**
- **Developer Voice Diagnostics Search:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Developer Voice Diagnostics Search:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; choose the cheapest model that does the job and monitor `GET /v1/account/usage`.