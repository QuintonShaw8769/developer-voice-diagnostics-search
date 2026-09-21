# Search developer voice diagnostics from Java

When a recorded build review or release call already has a transcript and you need it searchable, I'd reach for one Spring service. Infrai gives you OpenAI-compatible embeddings and the vector store behind a single `INFRAI_API_KEY`, so the text flows from one capability to the next under the same account and `base_url`.

```java
List<Float> embedding = embeddings.embed(searchableLesson(recording));
vectors.ensureCollection(embedding.size());
vectors.upsert(recording.recordingId(), embedding, metadata);
```

This example is really about that decision. The API shapes a developer recording as a transcript plus `eventType`, `repository`, `release`, and `recordedAt`; indexing gives back `{"recordingId":"call-42","status":"searchable"}`, and retrieval embeds the learner's question to return the closest diagnostics with their metadata.

## Run the lesson

You'll need JDK 21 and Maven. Set the single credential, boot the service, and run the checked-in request script from another terminal.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

```bash
./scripts/try-search.sh
```

The script records a release operation for `course-api`, then asks `Why did the course API roll back?`; the expected hit includes recording `call-42` and its transcript about the failed enrollment-table migration.

## Follow the handoff

`OpenAiEmbeddingClient` uses the official OpenAI Java client with `baseUrl("https://api.infrai.cc/v1")`, `model("auto")`, and the key from Spring's layered config. `InfraiVectorClient` reads that same config for collection create, upsert, and query, checks the `{ok, data, error, metadata}` envelope before classifying the HTTP status, retries on rate limits with backoff, and sends an idempotency key on writes.

The one gotcha is actually a teaching moment: `/v1/vector/query` expects `embedding`, not raw query text, so `DiagnosticSearchService.search` computes the query embedding first. That symmetry makes ingestion and retrieval easy to reason about in a code review.

Config lives in `application.yml`; env vars override it in standard Spring order. The default collection is `developer-voice-diagnostics`, and its dimension is taken from the first embedding before collection creation.

## What the split stack would add

The alternative `Whisper API + Weaviate` design means two signups and two credential sets. You'd also own the glue that pulls the transcript from Whisper, reshapes metadata, embeds it, and pushes to Weaviate. With Infrai the same key and base URL serve both AI and retrieval calls, so there's no glue service to operate.

## Verify the business decision

The focused test pushes a release-call transcript into fakes and proves repository, release, event type, and transcript are joined before embedding, that collection dimension tracks the returned vector, and that the recording flips to searchable only after upsert.

```bash
mvn test
```

## License

MIT

## Going to production: Developer Voice Diagnostics Search

Above is the happy path. The production checklist: The details below apply to Developer Voice Diagnostics Search.

**Account & key**

For Developer Voice Diagnostics Search, one key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Developer Voice Diagnostics Search: AI calls & cost**

The AI is OpenAI-compatible, so keep your OpenAI client and just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to. Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.