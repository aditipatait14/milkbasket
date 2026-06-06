package com.ecom.model;

public class AnalyticsChartPoint {

	private String label;
	private Double value;
	private Double percentage;
	private String helperText;

	public AnalyticsChartPoint() {
	}

	public AnalyticsChartPoint(String label, Double value, Double percentage, String helperText) {
		this.label = label;
		this.value = value;
		this.percentage = percentage;
		this.helperText = helperText;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Double getPercentage() {
		return percentage;
	}

	public void setPercentage(Double percentage) {
		this.percentage = percentage;
	}

	public String getHelperText() {
		return helperText;
	}

	public void setHelperText(String helperText) {
		this.helperText = helperText;
	}
}
