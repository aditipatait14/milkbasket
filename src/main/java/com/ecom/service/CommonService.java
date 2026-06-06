package com.ecom.service;

public interface CommonService {

	public void removeSessionMessage();
	public String rupeeSign();
	public String formatPrice(Double amount);
	public void removeOrderSuccessData();
}
