package com.serverless;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractionRequest implements DiscordRequest {
	
	private String token;
	private String id;
	private String channelId;
	private InteractionData data;
	
	public InteractionRequest() {
	}
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	public InteractionData getData() {
		return data;
	}
	public void setData(InteractionData data) {
		this.data = data;
	}
	
	
}
