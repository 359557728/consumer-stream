package com.emarbox.consumerstream.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfigration {
	
	@Value("${emar.bs.consumer.buffer.size}")
	private int bufferSize;
	
	@Value("${emar.bs.consumer.buffer.time}")
	private int bufferTime;
	
	@Value("${emar.bs.consumer.redis.key}")
	private String redisKey;

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public int getBufferTime() {
		return bufferTime;
	}

	public void setBufferTime(int bufferTime) {
		this.bufferTime = bufferTime;
	}

	public String getRedisKey() {
		return redisKey;
	}

	public void setRedisKey(String redisKey) {
		this.redisKey = redisKey;
	}
	
}
