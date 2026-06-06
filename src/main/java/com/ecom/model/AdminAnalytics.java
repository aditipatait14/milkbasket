package com.ecom.model;

import java.util.ArrayList;
import java.util.List;

public class AdminAnalytics {

	private long totalOrders;
	private long deliveredOrders;
	private long cancelledOrders;
	private long paidOrders;
	private long pendingOrders;
	private long onlineOrders;
	private long codOrders;
	private int totalUnitsSold;
	private double totalRevenue;
	private double totalCost;
	private double totalProfit;
	private double totalLoss;
	private double currentInventoryValue;
	private double averageOrderValue;
	private double profitMarginPercent;
	private long wishlistEntries;
	private long feedbackCount;
	private long lowStockCount;
	private List<ProductPerformanceSummary> topProducts = new ArrayList<>();
	private List<Product> lowStockProducts = new ArrayList<>();
	private List<AnalyticsChartPoint> financialHighlights = new ArrayList<>();
	private List<AnalyticsChartPoint> monthlySales = new ArrayList<>();
	private List<AnalyticsChartPoint> categoryPerformance = new ArrayList<>();
	private List<AnalyticsChartPoint> orderStatusBreakdown = new ArrayList<>();

	public long getTotalOrders() {
		return totalOrders;
	}

	public void setTotalOrders(long totalOrders) {
		this.totalOrders = totalOrders;
	}

	public long getDeliveredOrders() {
		return deliveredOrders;
	}

	public void setDeliveredOrders(long deliveredOrders) {
		this.deliveredOrders = deliveredOrders;
	}

	public long getCancelledOrders() {
		return cancelledOrders;
	}

	public void setCancelledOrders(long cancelledOrders) {
		this.cancelledOrders = cancelledOrders;
	}

	public long getPaidOrders() {
		return paidOrders;
	}

	public void setPaidOrders(long paidOrders) {
		this.paidOrders = paidOrders;
	}

	public long getPendingOrders() {
		return pendingOrders;
	}

	public void setPendingOrders(long pendingOrders) {
		this.pendingOrders = pendingOrders;
	}

	public long getOnlineOrders() {
		return onlineOrders;
	}

	public void setOnlineOrders(long onlineOrders) {
		this.onlineOrders = onlineOrders;
	}

	public long getCodOrders() {
		return codOrders;
	}

	public void setCodOrders(long codOrders) {
		this.codOrders = codOrders;
	}

	public int getTotalUnitsSold() {
		return totalUnitsSold;
	}

	public void setTotalUnitsSold(int totalUnitsSold) {
		this.totalUnitsSold = totalUnitsSold;
	}

	public double getTotalRevenue() {
		return totalRevenue;
	}

	public void setTotalRevenue(double totalRevenue) {
		this.totalRevenue = totalRevenue;
	}

	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public double getTotalProfit() {
		return totalProfit;
	}

	public void setTotalProfit(double totalProfit) {
		this.totalProfit = totalProfit;
	}

	public double getTotalLoss() {
		return totalLoss;
	}

	public void setTotalLoss(double totalLoss) {
		this.totalLoss = totalLoss;
	}

	public double getCurrentInventoryValue() {
		return currentInventoryValue;
	}

	public void setCurrentInventoryValue(double currentInventoryValue) {
		this.currentInventoryValue = currentInventoryValue;
	}

	public double getAverageOrderValue() {
		return averageOrderValue;
	}

	public void setAverageOrderValue(double averageOrderValue) {
		this.averageOrderValue = averageOrderValue;
	}

	public double getProfitMarginPercent() {
		return profitMarginPercent;
	}

	public void setProfitMarginPercent(double profitMarginPercent) {
		this.profitMarginPercent = profitMarginPercent;
	}

	public long getWishlistEntries() {
		return wishlistEntries;
	}

	public void setWishlistEntries(long wishlistEntries) {
		this.wishlistEntries = wishlistEntries;
	}

	public long getFeedbackCount() {
		return feedbackCount;
	}

	public void setFeedbackCount(long feedbackCount) {
		this.feedbackCount = feedbackCount;
	}

	public long getLowStockCount() {
		return lowStockCount;
	}

	public void setLowStockCount(long lowStockCount) {
		this.lowStockCount = lowStockCount;
	}

	public List<ProductPerformanceSummary> getTopProducts() {
		return topProducts;
	}

	public void setTopProducts(List<ProductPerformanceSummary> topProducts) {
		this.topProducts = topProducts;
	}

	public List<Product> getLowStockProducts() {
		return lowStockProducts;
	}

	public void setLowStockProducts(List<Product> lowStockProducts) {
		this.lowStockProducts = lowStockProducts;
	}

	public List<AnalyticsChartPoint> getFinancialHighlights() {
		return financialHighlights;
	}

	public void setFinancialHighlights(List<AnalyticsChartPoint> financialHighlights) {
		this.financialHighlights = financialHighlights;
	}

	public List<AnalyticsChartPoint> getMonthlySales() {
		return monthlySales;
	}

	public void setMonthlySales(List<AnalyticsChartPoint> monthlySales) {
		this.monthlySales = monthlySales;
	}

	public List<AnalyticsChartPoint> getCategoryPerformance() {
		return categoryPerformance;
	}

	public void setCategoryPerformance(List<AnalyticsChartPoint> categoryPerformance) {
		this.categoryPerformance = categoryPerformance;
	}

	public List<AnalyticsChartPoint> getOrderStatusBreakdown() {
		return orderStatusBreakdown;
	}

	public void setOrderStatusBreakdown(List<AnalyticsChartPoint> orderStatusBreakdown) {
		this.orderStatusBreakdown = orderStatusBreakdown;
	}
}
