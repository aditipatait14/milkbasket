package com.ecom.model;

import lombok.Getter;
import lombok.Setter;


public class PaymentOrderResponse {

	private String key;

	private String orderId;

	private Long amount;

	private String currency;

	private String name;

	private String description;

	private String image;

	private String prefillName;

	private String prefillEmail;

	private String prefillContact;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public Long getAmount() {
		return amount;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getPrefillName() {
		return prefillName;
	}

	public void setPrefillName(String prefillName) {
		this.prefillName = prefillName;
	}

	public String getPrefillEmail() {
		return prefillEmail;
	}

	public void setPrefillEmail(String prefillEmail) {
		this.prefillEmail = prefillEmail;
	}

	public String getPrefillContact() {
		return prefillContact;
	}

	public void setPrefillContact(String prefillContact) {
		this.prefillContact = prefillContact;
	}
	
	

}
