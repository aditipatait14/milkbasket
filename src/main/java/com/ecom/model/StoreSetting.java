package com.ecom.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_settings")
public class StoreSetting {

	@Id
	private Integer id;

	private Double deliveryFee;

	private Double taxRatePercent;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Double getDeliveryFee() {
		return deliveryFee;
	}

	public void setDeliveryFee(Double deliveryFee) {
		this.deliveryFee = deliveryFee;
	}

	public Double getTaxRatePercent() {
		return taxRatePercent;
	}

	public void setTaxRatePercent(Double taxRatePercent) {
		this.taxRatePercent = taxRatePercent;
	}

}
