package com.emarbox.consumerstream.configuration;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@RefreshScope
public class RedisConfigration {
	
	@Value("${emar.bs.consumer.redis.info}")
	private String redisInfo;
	
	@Value("${redis.max.active}")
	private int maxActive;
	
	@Value("${redis.max.idle}")
	private int maxIdle;
	
	@Value("${redis.max.wait}")
	private int maxWait;
	
	@Value("${redis.connect.timeout}")
	private int timeout;
	
	/**
	 * @return Map key is jedis pool, value is adx push data to this redis address.
	 */
	@Bean
	@RefreshScope
	public Map<JedisPool, List<String>> jedisPoolMap() {
		JedisPoolConfig jedisConfig = new JedisPoolConfig();
		jedisConfig.setMaxIdle(maxIdle);
		jedisConfig.setMaxWaitMillis(maxWait);
		jedisConfig.setTestOnBorrow(true);
		jedisConfig.setMaxTotal(maxActive);
		return Pattern
			.compile(",")
			.splitAsStream(redisInfo)
			.map(supplierIpPort -> Pattern.compile("-").splitAsStream(supplierIpPort).collect(Collectors.toList()))
			.filter(adxIpPortGroup -> adxIpPortGroup.size() == 3)
			.collect(Collectors.toMap(adxIpPortGroup -> new JedisPool(jedisConfig, adxIpPortGroup.get(1), Integer.parseInt(adxIpPortGroup.get(2)), timeout), adxIpPortGroup -> Collections.singletonList(adxIpPortGroup.get(0)), (existAdx, newAdx) -> {
				existAdx.addAll(newAdx);
				return existAdx;
			}));
	}
	
}
