package com.yskts.netty.load;

import java.util.LongSummaryStatistics;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yskts.netty.load.client.NettyLoadTestClient;
import com.yskts.netty.load.client.Result;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private static final String REQUEST_PAYLOAD = "test";

	private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(2);
	private LongSummaryStatistics totalLatency = new LongSummaryStatistics();
	private LongSummaryStatistics sendLatency = new LongSummaryStatistics();
	private LongSummaryStatistics responseLatency = new LongSummaryStatistics();

	public static void main(String[] args) {

		final Main main = new Main();
		main.run();
	}

	public void run() {
		final NettyLoadTestClient client = new NettyLoadTestClient.Builder("http://www.google.com:80/load/oauth", 1, 1600)
				.withRamp(128, 12)
				.withRequestFunction(uri -> {
					final DefaultFullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
					return fullHttpRequest;
				})
				.withResultConsumer(this::handleResult)
				.build();

		LOGGER.info("Starting Sending...");
		printStats();
		client.start().join();
	}

	private void handleResult(final Result result) {
		LOGGER.info("Handling Result...");
		sendLatency.accept(result.getSendLatency());
		responseLatency.accept(result.getResponseLatency());
		totalLatency.accept(result.getTotalLatency());
	}

	private void printStats() {
		System.out.println(String.format("Send Latency - avg: %f min: %d max: %d", sendLatency.getAverage() / 1000, sendLatency.getMin() / 1000, sendLatency.getMax()  / 1000));
		System.out.println(String.format("Recv Latency - avg: %f min: %d max: %d", responseLatency.getAverage() / 1000, responseLatency.getMin() / 1000, responseLatency.getMax()  / 1000));
		System.out.println(String.format("TotalLatency - avg: %f min: %d max: %d", totalLatency.getAverage() / 1000, totalLatency.getMin() / 1000, totalLatency.getMax()  / 1000));
		scheduledExecutorService.schedule(this::printStats, 1, TimeUnit.SECONDS);
	}
}
