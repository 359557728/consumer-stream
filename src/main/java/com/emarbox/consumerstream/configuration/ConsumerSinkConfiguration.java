package com.emarbox.consumerstream.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import com.emarbox.consumerstream.infrastructrue.StreamBindableChannel;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@EnableBinding(StreamBindableChannel.class)
public class ConsumerSinkConfiguration {
	
	private static final String COMMA_SEPARATOR = ",";
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final Logger throughput = LoggerFactory.getLogger("throughput");
	
	@Autowired
	private CommonConfigration commonConfigration;
	
	@Autowired
	private Map<JedisPool, List<String>> jedisPoolMap;

	@StreamListener
	public void dataIngest(@Input(StreamBindableChannel.YZ_INPUT) Flux<Message<String>> yzInbound, 
		@Input(StreamBindableChannel.GD_INPUT) Flux<Message<String>> gdInbound, 
		@Input(StreamBindableChannel.HT_INPUT) Flux<Message<String>> htInbound) {
		yzInbound.mergeWith(gdInbound).mergeWith(htInbound)
			.groupBy(message -> message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC))
			.flatMap(records -> records
				.bufferTimeout(commonConfigration.getBufferSize(), Duration.ofSeconds(commonConfigration.getBufferTime()))
				.publishOn(Schedulers.single())
				.map(recordBatch -> 
					{
						int resetSize = 0;
						try {
							resetSize += batchProcessAccordingTopic(recordBatch);
						} catch (Exception e) {
							log.error("record process failed in stream, caused by ", e);
						}
						return resetSize;
					}
				)
			).subscribe(
				processSize -> {
					throughput.info("total number {} processed", processSize);
				},
				exception -> {
					log.error("error occured when handle kafka message, caused by " + exception);
				}
			);
			
	}
	
	/**
	 * @param isRetry -- 是否是重发
	 * @throws Exception
	 */
	private void batchProcessIgnoreTopic(String supplierId, List<String> recordBatch, boolean isRetry) throws Exception {
		Optional<Map.Entry<JedisPool, List<String>>> firstMatchedPool = jedisPoolMap.entrySet().stream().filter(entry -> entry.getValue().contains(supplierId)).findFirst();
		if(firstMatchedPool.isPresent()) {
			Jedis jedis = null;
			try {
				JedisPool jedisPool = firstMatchedPool.get().getKey();
				jedis = jedisPool.getResource();
				long timeStart = System.nanoTime();
				String[] bufferedMessage = recordBatch.stream().toArray(String[]::new);
				jedis.rpush(commonConfigration.getRedisKey(), bufferedMessage);
				double usedTime = (double)(System.nanoTime() - timeStart) / 1000000.0;
				throughput.info("send type {} use time {} size {} topic {}", isRetry, usedTime, recordBatch.size(), supplierId);
			} catch (Exception e) {
				if(jedis != null) {
					jedis.close();
				}
				log.error("record process failed, caused by ", e);
			} finally {
				if(jedis != null) {
					jedis.close();
				}
			}
		}
	}
	
	private int batchProcessAccordingTopic(List<Message<String>> reocrds) throws Exception {
		String topic = reocrds.get(0).getHeaders().get(KafkaHeaders.RECEIVED_TOPIC).toString();
		if(topic.equals("dmap_media_resend")) {//DLQ retry mechanism
			reocrds
			.stream()
			.map(Message::getPayload)
			.filter(record -> record.split(COMMA_SEPARATOR, -1).length > 6)
			.collect(Collectors.groupingBy(record -> record.split(COMMA_SEPARATOR, -1)[5]))
			.entrySet()
			.stream()
			.forEach(record -> {
				try {
					batchProcessIgnoreTopic(record.getKey(), record.getValue(), true);
				} catch (Exception e) {
					log.error("record reprocess failed, caused by ", e);
				}
			});
		} else {
			List<String> recordBatch = reocrds
										.stream()
										.map(Message::getPayload)
										.collect(Collectors.toList());
			batchProcessIgnoreTopic(topic, recordBatch, false);
		}
		return reocrds.size();
	}
	
}
