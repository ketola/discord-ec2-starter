package com.serverless;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.ec2.AmazonEC2AsyncClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Handler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
	
	private static final String EC2_INSTANCE_ID = "i-0fa7b5cec54d06911";
	private static final String PUBLIC_KEY_PREFIX = "302a300506032b6570032100";
	private static final String PUBLIC_KEY = PUBLIC_KEY_PREFIX + "aa113209aa0c7bf75241d281f1f20a39a6483d40b1f1c0975ded26e1eddc8db9";
	private static final Logger LOG = LogManager.getLogger(Handler.class);

	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		LOG.info("received: {}", input);
		
		ObjectMapper mapper = new ObjectMapper();
		DiscordRequest discordRequest = null;
		try {
			discordRequest = mapper.readValue(input.get("body").toString(), DiscordRequest.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if(discordRequest instanceof PingRequest) {
			return handlePingRequest();
		} else if(discordRequest instanceof InteractionRequest) {
			return handleInteractionRequest(input);
		} 
		throw new IllegalArgumentException();	
	}
	
	private ApiGatewayResponse handlePingRequest() {
		return ApiGatewayResponse.builder()
				.setStatusCode(200)
				.setObjectBody(new PingResponse())
				.build();
	}
	
	private ApiGatewayResponse handleInteractionRequest(Map<String, Object> input) {
		Security.addProvider(new BouncyCastleProvider()); 
		Map<String, String> headers = (Map<String, String>) input.get("headers");
		
		String signature = headers.get("x-signature-ed25519");
		String timestamp = headers.get("x-signature-timestamp");
		String body = input.get("body").toString();
		
		try {
			LOG.info("Verify signature");
			// verify the signature
			java.security.Signature sgr = java.security.Signature.getInstance("Ed25519", "BC");
			sgr.initVerify(fromHexString(PUBLIC_KEY));
			sgr.update((timestamp + body).getBytes());
			if(!sgr.verify(Hex.decode(signature))) {
				return ApiGatewayResponse.builder()
						.setStatusCode(401)
						.build();
			}
			LOG.info("Signature ok, start instance");
			// start the instance, just start without waiting for the result because Discord will wait only for 3 seconds
			// before timing out
			AmazonEC2 client = AmazonEC2ClientBuilder.defaultClient();
			StartInstancesResult result = client.startInstances(new StartInstancesRequest(Arrays.asList(EC2_INSTANCE_ID)));
			LOG.info("Result " + result.getSdkResponseMetadata());
			
		} catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
			e.printStackTrace();
			return ApiGatewayResponse.builder()
					.setStatusCode(401)
					.build();
		}
		
		return ApiGatewayResponse.builder()
				.setStatusCode(200)
				.setObjectBody(new InteractionResponse())
				.build();
	}
	
	private static java.security.PublicKey fromHexString(String hexString) {
        try {
            return getKeyFactory().generatePublic(new X509EncodedKeySpec(Hex.decode(hexString)));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("PublicKey generation failed", e);
        }
    }
	
	private static KeyFactory getKeyFactory() {
        try {
            return KeyFactory.getInstance("Ed25519");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not get KeyFactory", e);
        }
    }
}
