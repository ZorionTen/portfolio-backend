package dev.zorionten.portfolio.time;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/time")
class TimeController {

	private static final long PROCESS_START_TIME_MS = System.currentTimeMillis();
	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

	@Value("${BUILD_TIMESTAMP:}")
	String buildTimestamp;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	TimeResponse time() {
		long now = System.currentTimeMillis();
		long uptimeMs = now - PROCESS_START_TIME_MS;
		long uptimeSeconds = uptimeMs / 1000;

		return new TimeResponse(
				now,
				Instant.ofEpochMilli(now).atOffset(ZoneOffset.UTC).format(ISO_FORMATTER),
				uptimeSeconds,
				formatUptime(uptimeSeconds),
				PROCESS_START_TIME_MS,
				Instant.ofEpochMilli(PROCESS_START_TIME_MS).atOffset(ZoneOffset.UTC).format(ISO_FORMATTER),
				buildTimestamp.isBlank() ? null : buildTimestamp,
				buildTimestamp.isBlank() ? null : calculateDeploymentDiff(now)
		);
	}

	private String formatUptime(long seconds) {
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long secs = seconds % 60;
		return hours + "h " + minutes + "m " + secs + "s";
	}

	private Long calculateDeploymentDiff(long nowMs) {
		try {
			long buildMs = Long.parseLong(buildTimestamp);
			return (nowMs - buildMs) / 1000;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	record TimeResponse(
			long timestamp,
			String timestamp_iso,
			long uptime_seconds,
			String uptime_human,
			long process_start_time,
			String process_start_iso,
			String build_timestamp,
			Long deployment_time_diff_seconds
	) {}
}