package com.emarbox.consumerstream.infrastructrue;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.MessageChannel;

public interface StreamBindableChannel {
	
	String YZ_INPUT = "yzin";
	
	String GD_INPUT = "gdin";
	
	String HT_INPUT = "htin";
	
	String HZ_INPUT = "hzin";
	
	@Input(StreamBindableChannel.YZ_INPUT)
	MessageChannel yzIn();
	
	@Input(StreamBindableChannel.GD_INPUT)
	MessageChannel gdIn();
	
	@Input(StreamBindableChannel.HT_INPUT)
	MessageChannel htIn();
	
	@Input(StreamBindableChannel.HZ_INPUT)
	MessageChannel hzIn();
	
}
