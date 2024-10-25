package com.example.redisson.lock.demo;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.Version;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Log4j2
@SpringBootTest
class RedissonLockTest {

	@Test
	@SneakyThrows
	void redissonLockTest() {

		Config config = new Config();
		ClusterServersConfig clusterServersConfig = config.useClusterServers();
		clusterServersConfig.addNodeAddress("redis://127.0.0.1:7001","redis://127.0.0.1:7002","redis://127.0.0.1:7003","redis://127.0.0.1:7004","redis://127.0.0.1:7005","redis://127.0.0.1:7006");

		RedissonClient client = Redisson.create(config);

		AtomicInteger times = new AtomicInteger(0);

		AtomicInteger unLockFailedTimes = new AtomicInteger(0);
		ForkJoinPool forkJoinPool = new ForkJoinPool(20);
		forkJoinPool.submit(() -> IntStream.range(0, 10000)
				.parallel()
				.forEach(i -> {
					times.incrementAndGet();
					RLock lock = client.getLock(UUID.randomUUID().toString());
					lock.lock(100, TimeUnit.MILLISECONDS);
					// do the task ...
					if (lock.isLocked() && lock.isHeldByCurrentThread()) {
						try {
							lock.unlock();
						} catch (IllegalMonitorStateException ex) {
							unLockFailedTimes.incrementAndGet();
						}
					}
				})).get();
		Version.logVersion();
		log.info("total times {} unlock failed times : {}", times.get(), unLockFailedTimes.get());
	}

}
