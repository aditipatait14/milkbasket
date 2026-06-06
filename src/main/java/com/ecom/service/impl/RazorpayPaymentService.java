package com.ecom.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ecom.model.Cart;
import com.ecom.model.CheckoutSummary;
import com.ecom.model.OrderRequest;
import com.ecom.model.PaymentOrderResponse;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.service.PaymentService;
import com.ecom.util.CheckoutCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RazorpayPaymentService implements PaymentService {

	@Value("${payment.razorpay.enabled:false}")
	private boolean razorpayEnabled;

	@Value("${payment.razorpay.key-id:}")
	private String keyId;

	@Value("${payment.razorpay.key-secret:}")
	private String keySecret;

	@Value("${payment.razorpay.currency:INR}")
	private String currency;

	@Value("${payment.razorpay.merchant-name:Ecom Store}")
	private String merchantName;

	@Value("${payment.razorpay.description:Secure checkout for Ecom Store}")
	private String merchantDescription;

	@Value("${payment.razorpay.logo-url:/img/MB.png}")
	private String merchantLogoUrl;

	@Value("${payment.razorpay.api-base-url:https://api.razorpay.com/v1}")
	private String apiBaseUrl;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CheckoutCalculator checkoutCalculator;

	@Autowired
	private ObjectMapper objectMapper;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Override
	public PaymentOrderResponse createOrder(UserDtls user, OrderRequest orderRequest) throws Exception {
		validatePaymentConfig();

		List<Cart> carts = cartRepository.findByUserId(user.getId());
		CheckoutSummary summary = checkoutCalculator.buildSummary(carts);
		if (summary.getTotalAmount() <= 0) {
			throw new IllegalStateException("Your cart is empty. Add products before starting payment.");
		}

		Map<String, Object> payload = new HashMap<>();
		payload.put("amount", Math.round(summary.getTotalAmount() * 100));
		payload.put("currency", currency);
		payload.put("receipt", "rcpt_" + System.currentTimeMillis());

		Map<String, Object> notes = new HashMap<>();
		notes.put("userId", String.valueOf(user.getId()));
		notes.put("email", pickValue(orderRequest.getEmail(), user.getEmail()));
		payload.put("notes", notes);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiBaseUrl + "/orders"))
				.header("Authorization", buildAuthHeader())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 400) {
			throw new IllegalStateException(resolveGatewayMessage(response.body()));
		}

		JsonNode jsonNode = objectMapper.readTree(response.body());
		PaymentOrderResponse paymentOrderResponse = new PaymentOrderResponse();
		paymentOrderResponse.setKey(keyId);
		paymentOrderResponse.setOrderId(jsonNode.path("id").asText());
		paymentOrderResponse.setAmount(jsonNode.path("amount").asLong());
		paymentOrderResponse.setCurrency(jsonNode.path("currency").asText(currency));
		paymentOrderResponse.setName(merchantName);
		paymentOrderResponse.setDescription(merchantDescription);
		paymentOrderResponse.setImage(resolveCheckoutImageUrl());
		paymentOrderResponse.setPrefillName(buildCustomerName(orderRequest, user));
		paymentOrderResponse.setPrefillEmail(pickValue(orderRequest.getEmail(), user.getEmail()));
		paymentOrderResponse.setPrefillContact(pickValue(orderRequest.getMobileNo(), user.getMobileNumber()));
		return paymentOrderResponse;
	}

	@Override
	public boolean verifySignature(OrderRequest orderRequest) {
		if (!isOnlinePaymentEnabled()) {
			return false;
		}

		if (!StringUtils.hasText(orderRequest.getRazorpayOrderId())
				|| !StringUtils.hasText(orderRequest.getRazorpayPaymentId())
				|| !StringUtils.hasText(orderRequest.getRazorpaySignature())) {
			return false;
		}

		try {
			String payload = orderRequest.getRazorpayOrderId() + "|" + orderRequest.getRazorpayPaymentId();
			String expectedSignature = generateSignature(payload, keySecret);
			return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
					((String) orderRequest.getRazorpaySignature()).getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isOnlinePaymentEnabled() {
		return razorpayEnabled && StringUtils.hasText(keyId) && StringUtils.hasText(keySecret);
	}

	private void validatePaymentConfig() {
		if (!isOnlinePaymentEnabled()) {
			throw new IllegalStateException("Online payment is not configured yet. Add Razorpay keys to continue.");
		}
	}

	private String buildAuthHeader() {
		String value = keyId + ":" + keySecret;
		return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private String generateSignature(String payload, String secret) throws Exception {
		Mac sha256Hmac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		sha256Hmac.init(secretKey);
		byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

		StringBuilder builder = new StringBuilder();
		for (byte singleByte : hash) {
			builder.append(String.format("%02x", singleByte & 0xff));
		}
		return builder.toString();
	}

	private String buildCustomerName(OrderRequest orderRequest, UserDtls user) {
		String firstName = orderRequest.getFirstName();
		String lastName = orderRequest.getLastName();
		if (StringUtils.hasText(firstName) || StringUtils.hasText(lastName)) {
			return (pickValue(firstName, "") + " " + pickValue(lastName, "")).trim();
		}
		return pickValue(user.getName(), "Ecom Customer");
	}

	private String pickValue(String primaryValue, String fallbackValue) {
		return StringUtils.hasText(primaryValue) ? primaryValue : fallbackValue;
	}

	private String resolveCheckoutImageUrl() {
		if (!StringUtils.hasText(merchantLogoUrl)) {
			return null;
		}

		try {
			URI uri = URI.create(merchantLogoUrl.trim());
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if ("https".equalsIgnoreCase(scheme) && StringUtils.hasText(host)
					&& !"localhost".equalsIgnoreCase(host)
					&& !"127.0.0.1".equals(host)
					&& !"0.0.0.0".equals(host)
					&& !"::1".equals(host)) {
				return merchantLogoUrl.trim();
			}
		} catch (IllegalArgumentException ignored) {
		}

		return null;
	}

	private String resolveGatewayMessage(String responseBody) {
		try {
			JsonNode rootNode = objectMapper.readTree(responseBody);
			String errorMessage = rootNode.path("error").path("description").asText();
			if (StringUtils.hasText(errorMessage)) {
				return errorMessage;
			}
		} catch (Exception ignored) {
		}
		return "Unable to initiate Razorpay payment right now. Please try again.";
	}

}
