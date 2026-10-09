# Short-link redirect performance test

The A/B test uses the same redirect URL and load parameters for both modes:

- `sync`: persist all statistics directly on the redirect request thread.
- `rocketmq`: synchronously send the statistics message to the broker and persist it later. Keep the
  performance-test consumer disabled so consumer database writes do not compete with the request path.

Run JMeter 5.6.3 in non-GUI mode:

```powershell
.\.local\tools\apache-jmeter-5.6.3\bin\jmeter.bat -n `
  -t .\scripts\performance\shortlink-redirect.jmx `
  -Jshort_uri=2FFQFX -Jthreads=80 -Jramp=5 -Jduration=30 `
  -Jconnect_timeout=1000 -Jresponse_timeout=5000 `
  -l .\.local\perf\result.jtl
```

The plan calls the aggregation service directly on `127.0.0.1:8003`, keeps HTTP connections alive, does not follow redirects, and treats HTTP 302 as success.

Run each mode three times after a warm-up and compare the medians. Raw results are written under
`.local/perf`. See `RESULTS.md` for the measured result and limitations.

To repeat the 100-message idempotency check, compile and run the helper against the RocketMQ client
libraries:

```powershell
$rocketLib=(Resolve-Path '.\.local\tools\rocketmq-all-5.2.0-bin-release\lib').Path + '\*'
& 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javac.exe' `
  -encoding UTF-8 -cp $rocketLib -d .\.local\perf `
  .\scripts\performance\RocketMqDuplicateMessageSender.java
& 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe' `
  -cp ".\.local\perf;$rocketLib" RocketMqDuplicateMessageSender `
  127.0.0.1:9876 short-link-stats <unique-business-key> 100 127.0.0.1:8003/2FFQFX
```

For a consumer drain check, first start the app with the consumer enabled and point its topic/group at
an empty test topic. Compile `RocketMqStatsBatchSender.java`, then send a batch of distinct records:

```powershell
& 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javac.exe' `
  -encoding UTF-8 -cp $rocketLib -d .\.local\perf `
  .\scripts\performance\RocketMqStatsBatchSender.java
& 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe' `
  -cp ".\.local\perf;$rocketLib" RocketMqStatsBatchSender `
  127.0.0.1:9876 <test-topic> <batch-prefix> 1000 127.0.0.1:8003/2FFQFX
```

Record the DB baseline before sending. Wait until both the link PV and access-log row count increase by
1,000; then check `mqadmin consumerProgress` for zero lag, zero in-flight messages and an empty retry
queue. The measured local run is in `RESULTS.md`.
