package com.serverless;

import java.util.ArrayList;
import java.util.List;

public class InteractionResponse {
	private final int type = 4;
	private final Data data = new Data();
	
	public int getType() {
		return type;
	}
	
	public Data getData() {
		return data;
	}
	
	public static class Data {
		private final String content = "Server is starting";
		private final boolean tts = false;
		private final List embeds = new ArrayList<>();
		
		public String getContent() {
			return content;
		}

		public boolean isTts() {
			return tts;
		}

		public List getEmbeds() {
			return embeds;
		}	
	}
}
