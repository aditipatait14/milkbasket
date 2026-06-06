package com.ecom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ecom.model.StoreSetting;
import com.ecom.repository.StoreSettingRepository;
import com.ecom.service.StoreSettingService;

@Service
public class StoreSettingServiceImpl implements StoreSettingService {

	private static final int SETTINGS_ID = 1;

	@Value("${checkout.default-delivery-fee:50}")
	private Double defaultDeliveryFee;

	@Value("${checkout.default-tax-rate:5}")
	private Double defaultTaxRate;

	@Autowired
	private StoreSettingRepository storeSettingRepository;

	@Override
	public StoreSetting getSettings() {
		return storeSettingRepository.findById(SETTINGS_ID).orElseGet(this::buildDefaultSettings);
	}

	@Override
	public StoreSetting saveSettings(Double deliveryFee, Double taxRatePercent) {
		StoreSetting settings = storeSettingRepository.findById(SETTINGS_ID).orElseGet(this::buildDefaultSettings);
		settings.setDeliveryFee(sanitizeDeliveryFee(deliveryFee));
		settings.setTaxRatePercent(sanitizeTaxRate(taxRatePercent));
		return storeSettingRepository.save(settings);
	}

	private StoreSetting buildDefaultSettings() {
		StoreSetting settings = new StoreSetting();
		settings.setId(SETTINGS_ID);
		settings.setDeliveryFee(sanitizeDeliveryFee(defaultDeliveryFee));
		settings.setTaxRatePercent(sanitizeTaxRate(defaultTaxRate));
		return settings;
	}

	private Double sanitizeDeliveryFee(Double deliveryFee) {
		if (deliveryFee == null || deliveryFee < 0) {
			return 0.0;
		}
		return deliveryFee;
	}

	private Double sanitizeTaxRate(Double taxRatePercent) {
		if (taxRatePercent == null || taxRatePercent < 0) {
			return 0.0;
		}
		return Math.min(taxRatePercent, 5.0);
	}

}
