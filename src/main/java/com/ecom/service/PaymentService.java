package com.ecom.service;

import com.ecom.model.OrderRequest;
import com.ecom.model.PaymentOrderResponse;
import com.ecom.model.UserDtls;

public interface PaymentService {

	PaymentOrderResponse createOrder(UserDtls user, OrderRequest orderRequest) throws Exception;

	boolean verifySignature(OrderRequest orderRequest);

	boolean isOnlinePaymentEnabled();

}
