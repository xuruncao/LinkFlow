# RocketMQ statistics A/B benchmark

Test time: 2026-10-03 (Asia/Shanghai)

## Scope and parameters

- Workload: `GET http://127.0.0.1:8003/2FFQFX`, expect HTTP 302, keep-alive enabled.
- JMeter: 5.6.3, non-GUI, 1 GB heap, Java 21.
- Load: 80 threads, 5-second ramp-up, 30-second scheduled duration, 5-second response timeout.
- Repetitions: one warm-up followed by three measured runs per mode; medians are used below.
- Host: Windows 11, Intel Core Ultra 5 125H, 18 logical processors.
- Services: application, JMeter, MySQL 8, Redis and RocketMQ 5.2.0 all ran on the same machine.
- RocketMQ topic: 8 read queues and 8 write queues; broker uses `ASYNC_FLUSH`.
- Sync mode: the redirect thread directly runs the complete statistics persistence logic.
- RocketMQ mode: the redirect thread calls `syncSend` and waits for the broker acknowledgement. The
  performance-test consumer is disabled during this comparison, so this measures request-path
  decoupling rather than end-to-end database-drain throughput.
- The external IP-location API is replaced with a fixed local response in both modes.

## Three-run results

| Mode | Run | Samples | QPS | Average | P50 | P95 | P99 | Errors |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Sync persistence | 1 | 1,533 | 49.25 | 1,469.14 ms | 1,392 ms | 2,736 ms | 3,482 ms | 1 (0.07%) |
| Sync persistence | 2 | 1,593 | 51.24 | 1,414.34 ms | 1,379 ms | 2,467 ms | 3,078 ms | 0 (0%) |
| Sync persistence | 3 | 1,533 | 49.07 | 1,477.63 ms | 1,419 ms | 2,668 ms | 3,158 ms | 1 (0.07%) |
| RocketMQ request path | 1 | 69,307 | 2,315.25 | 31.62 ms | 28 ms | 53 ms | 63 ms | 0 (0%) |
| RocketMQ request path | 2 | 63,449 | 2,119.56 | 34.43 ms | 29 ms | 61 ms | 96 ms | 0 (0%) |
| RocketMQ request path | 3 | 56,301 | 1,881.53 | 38.93 ms | 33 ms | 70 ms | 115 ms | 0 (0%) |

Median comparison:

| Metric | Sync | RocketMQ | Change |
|---|---:|---:|---:|
| Throughput | 49.25 QPS | 2,119.56 QPS | 43.04x, +4,203.68% |
| P99 latency | 3,158 ms | 96 ms | -96.96% |
| Error rate | 0.07% | 0% | -0.07 percentage points |

The sync error-rate median is 0.07% because two of the three runs each contained one request that
exceeded the configured five-second timeout.

## Duplicate-message idempotency

The helper sent the same valid JSON payload 100 times with the same business key
`jmeter-idempotency-valid-20261003-2301`.

- Producer result: 100 requested, 100 `SEND_OK`.
- Database baseline: `total_pv=9905`, access-log rows `=9905`.
- Database after consumption: `total_pv=9906`, access-log rows `=9906`.
- Effective writes: 1; duplicate messages blocked: 99/99; duplicate-blocking rate: 100%.
- Redis completion value: `1`; observed remaining TTL: 1,761 seconds (configured TTL: 30 minutes).

## Consumer persistence and backlog drain

The consumer was enabled with its configured 8 concurrent consumer threads. A dedicated topic with 8
queues and a dedicated consumer group were used so the old performance-test backlog and malformed
manual test messages on other topics could not affect the result. The messages targeted the same test
short link, each with a different business idempotency key.

Two batches of 1,000 distinct messages were sent. Both had 1,000/1,000 successful broker sends. In the
timed second run:

- Publishing took 5.363 seconds.
- From starting the sender process to observing all 1,000 additional access-log rows in MySQL took
  17.865 seconds (about 56.0 messages/second end to end, including producer startup and publishing).
- PV and access-log counts each increased by exactly 1,000; UV and UIP stayed unchanged as configured.
- All 1,000 Redis idempotency keys were in completed state (`1`).
- Final offsets were 250/250 on each of the 8 queues: total lag 0, in-flight 0.
- The retry queue had 0 messages. Application logs contained no message-consumption failures.

Across both runs, all 2,000 sent records were persisted exactly once. This confirms small-batch
eventual persistence and retry/idempotency behavior on the local setup; it is not a long-duration
maximum consumer capacity test. RocketMQ client startup also logged that it could not write its
default log under the Windows user profile, but this logging permission warning did not affect
message consumption.

## Interpretation

These are development-machine results, not production capacity figures. The request-path comparison disabled the consumer, while the 1,000-message drain check measured a separate small batch. Repeat the plan on an isolated server for a longer steady-state window before drawing production conclusions. Raw outputs remain local; this repository contains the summarized observations and reproduction scripts.
