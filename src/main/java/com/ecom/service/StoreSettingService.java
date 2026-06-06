package com.ecom.service;

import com.ecom.model.StoreSetting;

public interface StoreSettingService {

	StoreSetting getSettings();

	StoreSetting saveSettings(Double deliveryFee, Double taxRatePercent);

}
