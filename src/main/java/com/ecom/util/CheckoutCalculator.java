package com.ecom.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ecom.model.Cart;
import com.ecom.model.CheckoutSummary;
import com.ecom.model.StoreSetting;
import com.ecom.service.StoreSettingService;

@Component
public class CheckoutCalculator {

	private final StoreSettingService storeSettingService;

	public CheckoutCalculator(StoreSettingService storeSettingService) {
		this.storeSettingService = storeSettingService;
	}

	public CheckoutSummary buildSummary(List<Cart> carts) {
		CheckoutSummary summary = new CheckoutSummary();
		if (carts == null || carts.isEmpty()) {
			summary.setSubtotal(0.0);
			summary.setDeliveryFee(0.0);
			summary.setTaxAmount(0.0);
			summary.setTotalAmount(0.0);
			summary.setItemCount(0);
			return summary;
		}

		StoreSetting settings = storeSettingService.getSettings();
		double deliveryFee = defaultValue(settings.getDeliveryFee());
		double taxRatePercent = clampTaxRate(settings.getTaxRatePercent());

		double subtotal = 0.0;
		int itemCount = 0;

		for (Cart cart : carts) {
			double lineAmount = cart.getProduct().getDiscountPrice() * cart.getQuantity();
			subtotal += lineAmount;
			itemCount += cart.getQuantity();
		}

		double taxAmount = roundCurrency(subtotal * taxRatePercent / 100.0);
		double totalAmount = subtotal + deliveryFee + taxAmount;
		summary.setSubtotal(roundCurrency(subtotal));
		summary.setDeliveryFee(roundCurrency(deliveryFee));
		summary.setTaxAmount(taxAmount);
		summary.setTaxRatePercent(taxRatePercent);
		summary.setTotalAmount(roundCurrency(totalAmount));
		summary.setItemCount(itemCount);
		return summary;
	}

	private double defaultValue(Double value) {
		return value == null ? 0.0 : value;
	}

	private double clampTaxRate(Double taxRatePercent) {
		if (taxRatePercent == null || taxRatePercent < 0) {
			return 0.0;
		}
		return Math.min(taxRatePercent, 5.0);
	}

	private double roundCurrency(double amount) {
		return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

}
