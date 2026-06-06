package com.ecom.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.AdminAnalytics;
import com.ecom.model.AnalyticsChartPoint;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.ProductPerformanceSummary;
import com.ecom.repository.ProductFeedbackRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.WishlistRepository;
import com.ecom.service.AnalyticsService;
import com.ecom.util.OrderStatus;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

	private static final int LOW_STOCK_THRESHOLD = 7;
	private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM yy");

	@Autowired
	private ProductOrderRepository productOrderRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private ProductFeedbackRepository productFeedbackRepository;

	@Override
	public AdminAnalytics buildAdminAnalytics() {
		AdminAnalytics analytics = new AdminAnalytics();
		List<ProductOrder> orders = productOrderRepository.findAllByOrderByOrderDateTimeDesc();
		List<Product> allProducts = productRepository.findAll();
		List<Product> lowStockProducts = productRepository.findByIsActiveTrueAndStockLessThanEqualOrderByStockAsc(LOW_STOCK_THRESHOLD);

		analytics.setTotalOrders(orders.size());
		analytics.setCancelledOrders(orders.stream()
				.filter(order -> OrderStatus.CANCEL.getName().equalsIgnoreCase(order.getStatus())).count());
		analytics.setDeliveredOrders(orders.stream()
				.filter(order -> OrderStatus.DELIVERED.getName().equalsIgnoreCase(order.getStatus())).count());
		analytics.setPaidOrders(orders.stream().filter(order -> "PAID".equalsIgnoreCase(order.getPaymentStatus())).count());
		analytics.setPendingOrders(orders.stream().filter(this::isPendingOrder).count());
		analytics.setOnlineOrders(orders.stream().filter(order -> "ONLINE".equalsIgnoreCase(order.getPaymentType())).count());
		analytics.setCodOrders(orders.stream().filter(order -> "COD".equalsIgnoreCase(order.getPaymentType())).count());
		analytics.setWishlistEntries(wishlistRepository.count());
		analytics.setFeedbackCount(productFeedbackRepository.count());
		analytics.setLowStockCount(lowStockProducts.size());
		analytics.setLowStockProducts(lowStockProducts);
		analytics.setCurrentInventoryValue(round(allProducts.stream()
				.mapToDouble(product -> resolveCostPrice(product) * safe(product.getStock())).sum()));

		double totalRevenue = 0.0;
		double totalCost = 0.0;
		double totalProfit = 0.0;
		double totalLoss = 0.0;
		int totalUnitsSold = 0;
		Map<Integer, ProductPerformanceSummary> performanceMap = new HashMap<>();
		Map<YearMonth, SalesAggregate> monthlySalesMap = initializeMonthlySales();
		Map<String, SalesAggregate> categoryPerformanceMap = new HashMap<>();

		for (ProductOrder order : orders) {
			if (OrderStatus.CANCEL.getName().equalsIgnoreCase(order.getStatus())) {
				continue;
			}

			double lineRevenue = safe(order.getPrice()) * safe(order.getQuantity());
			double lineCost = resolveOrderCost(order) * safe(order.getQuantity());
			double margin = lineRevenue - lineCost;

			totalRevenue += lineRevenue;
			totalCost += lineCost;
			totalUnitsSold += safe(order.getQuantity());
			if (margin >= 0) {
				totalProfit += margin;
			} else {
				totalLoss += Math.abs(margin);
			}

			YearMonth orderMonth = resolveOrderMonth(order);
			if (orderMonth != null && monthlySalesMap.containsKey(orderMonth)) {
				SalesAggregate monthlyAggregate = monthlySalesMap.get(orderMonth);
				monthlyAggregate.revenue += lineRevenue;
				monthlyAggregate.units += safe(order.getQuantity());
				monthlyAggregate.orders++;
			}

			Product product = order.getProduct();
			if (product == null || product.getId() == null) {
				continue;
			}

			String categoryName = safeText(product.getCategory(), "Uncategorised");
			SalesAggregate categoryAggregate = categoryPerformanceMap.computeIfAbsent(categoryName,
					key -> new SalesAggregate());
			categoryAggregate.revenue += lineRevenue;
			categoryAggregate.units += safe(order.getQuantity());
			categoryAggregate.orders++;

			ProductPerformanceSummary summary = performanceMap.computeIfAbsent(product.getId(), key -> {
				ProductPerformanceSummary created = new ProductPerformanceSummary();
				created.setProductId(product.getId());
				created.setProductTitle(safeText(product.getTitle(), "Archived Product"));
				created.setUnitsSold(0);
				created.setRevenue(0.0);
				created.setProfit(0.0);
				created.setAverageRating(product.getAverageRating() == null ? 0.0 : product.getAverageRating());
				created.setRatingCount(product.getRatingCount() == null ? 0 : product.getRatingCount());
				return created;
			});

			summary.setUnitsSold(summary.getUnitsSold() + safe(order.getQuantity()));
			summary.setRevenue(round(summary.getRevenue() + lineRevenue));
			summary.setProfit(round(summary.getProfit() + margin));
		}

		analytics.setTotalUnitsSold(totalUnitsSold);
		analytics.setTotalRevenue(round(totalRevenue));
		analytics.setTotalCost(round(totalCost));
		analytics.setTotalProfit(round(totalProfit));
		analytics.setTotalLoss(round(totalLoss));
		analytics.setAverageOrderValue(round(totalRevenue / Math.max(1, analytics.getTotalOrders() - analytics.getCancelledOrders())));
		analytics.setProfitMarginPercent(totalRevenue <= 0 ? 0.0 : round((totalProfit / totalRevenue) * 100));
		analytics.setTopProducts(performanceMap.values().stream()
				.sorted(Comparator.comparing(ProductPerformanceSummary::getUnitsSold).reversed()
						.thenComparing(ProductPerformanceSummary::getRevenue, Comparator.reverseOrder()))
				.limit(6).toList());
		analytics.setFinancialHighlights(buildFinancialHighlights(totalRevenue, totalCost, totalProfit, totalLoss));
		analytics.setMonthlySales(buildMonthlySales(monthlySalesMap));
		analytics.setCategoryPerformance(buildCategoryPerformance(categoryPerformanceMap));
		analytics.setOrderStatusBreakdown(buildOrderBreakdown(analytics));

		return analytics;
	}

	private int safe(Integer value) {
		return value == null ? 0 : value;
	}

	private int safe(int value) {
		return Math.max(value, 0);
	}

	private double safe(Double value) {
		return value == null ? 0.0 : value;
	}

	private String safeText(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private double resolveCostPrice(Product product) {
		if (product.getCostPrice() != null && product.getCostPrice() >= 0) {
			return product.getCostPrice();
		}
		return safe(product.getPrice());
	}

	private double resolveOrderCost(ProductOrder order) {
		if (order.getCostPrice() != null && order.getCostPrice() >= 0) {
			return order.getCostPrice();
		}
		Product product = order.getProduct();
		return product == null ? 0.0 : resolveCostPrice(product);
	}

	private double round(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	private boolean isPendingOrder(ProductOrder order) {
		String status = order.getStatus();
		return status == null || (!OrderStatus.CANCEL.getName().equalsIgnoreCase(status)
				&& !OrderStatus.DELIVERED.getName().equalsIgnoreCase(status));
	}

	private YearMonth resolveOrderMonth(ProductOrder order) {
		if (order.getOrderDateTime() != null) {
			return YearMonth.from(order.getOrderDateTime());
		}
		LocalDate orderDate = order.getOrderDate();
		return orderDate == null ? null : YearMonth.from(orderDate);
	}

	private Map<YearMonth, SalesAggregate> initializeMonthlySales() {
		Map<YearMonth, SalesAggregate> monthlySales = new LinkedHashMap<>();
		YearMonth currentMonth = YearMonth.now();
		for (int i = 5; i >= 0; i--) {
			monthlySales.put(currentMonth.minusMonths(i), new SalesAggregate());
		}
		return monthlySales;
	}

	private List<AnalyticsChartPoint> buildFinancialHighlights(double totalRevenue, double totalCost, double totalProfit,
			double totalLoss) {
		List<AnalyticsChartPoint> points = new ArrayList<>();
		double maxValue = Math.max(Math.max(totalRevenue, totalCost), Math.max(totalProfit, totalLoss));
		double divisor = maxValue <= 0 ? 1.0 : maxValue;

		points.add(new AnalyticsChartPoint("Sales Revenue", round(totalRevenue),
				round((totalRevenue / divisor) * 100), "Gross earnings from active orders"));
		points.add(new AnalyticsChartPoint("Inventory Cost", round(totalCost),
				round((totalCost / divisor) * 100), "Estimated cost of goods sold"));
		points.add(new AnalyticsChartPoint("Net Profit", round(totalProfit), round((totalProfit / divisor) * 100),
				"Positive margin retained"));
		points.add(new AnalyticsChartPoint("Loss Exposure", round(totalLoss), round((totalLoss / divisor) * 100),
				"Orders sold below tracked cost"));
		return points;
	}

	private List<AnalyticsChartPoint> buildMonthlySales(Map<YearMonth, SalesAggregate> monthlySalesMap) {
		double maxRevenue = monthlySalesMap.values().stream().mapToDouble(entry -> entry.revenue).max().orElse(0.0);
		double divisor = maxRevenue <= 0 ? 1.0 : maxRevenue;
		List<AnalyticsChartPoint> chartPoints = new ArrayList<>();

		for (Map.Entry<YearMonth, SalesAggregate> entry : monthlySalesMap.entrySet()) {
			SalesAggregate aggregate = entry.getValue();
			chartPoints.add(new AnalyticsChartPoint(entry.getKey().format(MONTH_LABEL_FORMAT), round(aggregate.revenue),
					round((aggregate.revenue / divisor) * 100),
					aggregate.orders + " orders | " + aggregate.units + " units"));
		}
		return chartPoints;
	}

	private List<AnalyticsChartPoint> buildCategoryPerformance(Map<String, SalesAggregate> categoryPerformanceMap) {
		List<Map.Entry<String, SalesAggregate>> sortedCategories = new ArrayList<>(categoryPerformanceMap.entrySet());
		sortedCategories.sort((left, right) -> Double.compare(right.getValue().revenue, left.getValue().revenue));
		double maxRevenue = sortedCategories.stream().map(Map.Entry::getValue).mapToDouble(entry -> entry.revenue).max()
				.orElse(0.0);
		double divisor = maxRevenue <= 0 ? 1.0 : maxRevenue;

		List<AnalyticsChartPoint> chartPoints = new ArrayList<>();
		for (Map.Entry<String, SalesAggregate> entry : sortedCategories.stream().limit(5).toList()) {
			SalesAggregate aggregate = entry.getValue();
			chartPoints.add(new AnalyticsChartPoint(entry.getKey(), round(aggregate.revenue),
					round((aggregate.revenue / divisor) * 100),
					aggregate.units + " units | " + aggregate.orders + " orders"));
		}
		return chartPoints;
	}

	private List<AnalyticsChartPoint> buildOrderBreakdown(AdminAnalytics analytics) {
		Map<String, Double> counts = new LinkedHashMap<>();
		counts.put("Delivered", (double) analytics.getDeliveredOrders());
		counts.put("Pending", (double) analytics.getPendingOrders());
		counts.put("Cancelled", (double) analytics.getCancelledOrders());
		counts.put("Paid", (double) analytics.getPaidOrders());
		counts.put("Online", (double) analytics.getOnlineOrders());
		counts.put("Cash on Delivery", (double) analytics.getCodOrders());

		double totalOrders = Math.max(1, analytics.getTotalOrders());
		List<AnalyticsChartPoint> chartPoints = new ArrayList<>();
		for (Map.Entry<String, Double> entry : counts.entrySet()) {
			chartPoints.add(new AnalyticsChartPoint(entry.getKey(), round(entry.getValue()),
					round((entry.getValue() / totalOrders) * 100),
					Math.round(entry.getValue()) + " of " + analytics.getTotalOrders() + " orders"));
		}
		return chartPoints;
	}

	private static class SalesAggregate {
		private double revenue;
		private int units;
		private int orders;
	}
}
