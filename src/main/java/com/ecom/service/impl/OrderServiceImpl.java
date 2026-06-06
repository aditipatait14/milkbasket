package com.ecom.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ecom.model.Cart;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderRequest;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.service.OrderService;
import com.ecom.service.PaymentService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ProductOrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PaymentService paymentService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception {

		List<Cart> carts = cartRepository.findByUserId(userid);
		if (carts.isEmpty()) {
			throw new IllegalStateException("Your cart is empty. Add products before placing an order.");
		}
		validateCartStock(carts);

		String paymentType = orderRequest.getPaymentType() == null ? "COD"
				: orderRequest.getPaymentType().trim().toUpperCase();
		String paymentStatus = "PENDING/COD";
		LocalDateTime orderTimestamp = LocalDateTime.now();
		LocalDateTime paymentRecordedAt = null;
		if ("ONLINE".equals(paymentType)) {
			if (!StringUtils.hasText(orderRequest.getRazorpayOrderId())
					|| !StringUtils.hasText(orderRequest.getRazorpayPaymentId())
					|| !StringUtils.hasText(orderRequest.getRazorpaySignature())) {
				throw new IllegalStateException(
						"Payment was not completed in Razorpay. In test mode, finish the mock bank success step after entering card details.");
			}
			boolean verified = paymentService.verifySignature(orderRequest);
			if (!verified) {
				throw new IllegalStateException(
						"Payment signature verification failed. Please confirm that the Razorpay Test Key ID and Test Secret belong to the same test account, then retry.");
			}
			paymentStatus = "PAID";
			paymentRecordedAt = orderTimestamp;
		}

		for (Cart cart : carts) {
			Product product = productRepository.findById(cart.getProduct().getId())
					.orElseThrow(() -> new IllegalStateException("Product no longer exists."));
			if (product.getStock() < cart.getQuantity()) {
				throw new IllegalStateException(product.getTitle() + " has only " + product.getStock()
						+ " units left. Please update your cart and try again.");
			}
			product.setStock(product.getStock() - cart.getQuantity());
			productRepository.save(product);

			ProductOrder order = new ProductOrder();

			order.setOrderId(UUID.randomUUID().toString());
			order.setOrderDate(orderTimestamp.toLocalDate());
			order.setOrderDateTime(orderTimestamp);

			order.setProduct(product);
			order.setPrice(resolveEffectivePrice(product));
			order.setCostPrice(resolveCostPrice(product));

			order.setQuantity(cart.getQuantity());
			order.setUser(cart.getUser());

			order.setStatus(OrderStatus.IN_PROGRESS.getName());
			order.setPaymentType(paymentType);
			order.setPaymentStatus(paymentStatus);
			order.setPaymentOrderId(orderRequest.getRazorpayOrderId());
			order.setPaymentId(orderRequest.getRazorpayPaymentId());
			order.setPaymentSignature(orderRequest.getRazorpaySignature());
			order.setPaymentRecordedAt(paymentRecordedAt);

			OrderAddress address = new OrderAddress();
			address.setFirstName(orderRequest.getFirstName());
			address.setLastName(orderRequest.getLastName());
			address.setEmail(orderRequest.getEmail());
			address.setMobileNo(orderRequest.getMobileNo());
			address.setAddress(orderRequest.getAddress());
			address.setCity(orderRequest.getCity());
			address.setState(orderRequest.getState());
			address.setPincode(orderRequest.getPincode());

			order.setOrderAddress(address);

			ProductOrder saveOrder = orderRepository.save(order);
			try {
				commonUtil.sendMailForProductOrder(saveOrder, "Order placed successfully");
			} catch (Exception ignored) {
			}
		}
		resetCart(carts.get(0).getUser());
	}

	private void resetCart(UserDtls user) {
		cartRepository.deleteByUser(user);
	}

	@Override
	public List<ProductOrder> getOrdersByUser(Integer userId) {
		List<ProductOrder> orders = orderRepository.findByUserId(userId);
		return orders;
	}

	@Override
	@Transactional
	public ProductOrder updateOrderStatus(Integer id, String status) {
		Optional<ProductOrder> findById = orderRepository.findById(id);
		if (findById.isPresent()) {
			ProductOrder productOrder = findById.get();
			if (status == null || status.isBlank()) {
				return productOrder;
			}
			if (OrderStatus.CANCEL.getName().equalsIgnoreCase(status)
					&& !OrderStatus.CANCEL.getName().equalsIgnoreCase(productOrder.getStatus())) {
				Product product = productOrder.getProduct();
				product.setStock(product.getStock() + productOrder.getQuantity());
				productRepository.save(product);
			}
			productOrder.setStatus(status);
			ProductOrder updateOrder = orderRepository.save(productOrder);
			return updateOrder;
		}
		return null;
	}

	@Override
	public List<ProductOrder> getAllOrders() {
		return orderRepository.findAll();
	}

	@Override
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return orderRepository.findAll(pageable);

	}

	@Override
	public ProductOrder getOrdersByOrderId(String orderId) {
		return orderRepository.findByOrderId(orderId);
	}

	@Override
	public ResponseEntity<byte[]> downloadReceipt(Integer orderId, Integer userId) {
		ProductOrder order = orderRepository.findByIdAndUserId(orderId, userId);
		if (order == null) {
			return ResponseEntity.notFound().build();
		}

		String fileName = "receipt-" + order.getOrderId() + ".pdf";
		byte[] receiptBytes = buildReceiptPdf(order);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
				.contentType(MediaType.APPLICATION_PDF).body(receiptBytes);
	}

	private void validateCartStock(List<Cart> carts) {
		for (Cart cart : carts) {
			Product product = productRepository.findById(cart.getProduct().getId())
					.orElseThrow(() -> new IllegalStateException("A product in your cart no longer exists."));
			if (Boolean.FALSE.equals(product.getIsActive())) {
				throw new IllegalStateException(product.getTitle() + " is no longer available.");
			}
			if (product.getStock() <= 0) {
				throw new IllegalStateException(product.getTitle() + " is out of stock.");
			}
			if (cart.getQuantity() > product.getStock()) {
				throw new IllegalStateException(product.getTitle() + " has only " + product.getStock()
						+ " units available.");
			}
		}
	}

	private double resolveEffectivePrice(Product product) {
		if (product.getDiscountPrice() != null && product.getDiscountPrice() > 0) {
			return product.getDiscountPrice();
		}
		return product.getPrice() == null ? 0.0 : product.getPrice();
	}

	private double resolveCostPrice(Product product) {
		if (product.getCostPrice() != null && product.getCostPrice() >= 0) {
			return product.getCostPrice();
		}
		return product.getPrice() == null ? 0.0 : product.getPrice();
	}

	private byte[] buildReceiptPdf(ProductOrder order) {
		double totalAmount = (order.getPrice() == null ? 0.0 : order.getPrice())
				* (order.getQuantity() == null ? 0 : order.getQuantity());
		String paymentLabel = "PAID".equalsIgnoreCase(order.getPaymentStatus()) ? "Paid Amount" : "Amount Due";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
		OrderAddress address = order.getOrderAddress();
		Product product = order.getProduct();

		List<ReceiptTextLine> lines = new ArrayList<>();
		lines.add(new ReceiptTextLine("F2", 24, 48, 800, "MilkBasket Receipt"));
		lines.add(new ReceiptTextLine("F1", 11, 48, 782, "Thanks for shopping with us."));
		lines.add(new ReceiptTextLine("F2", 12, 400, 800, "Status: " + safeText(order.getStatus())));
		lines.add(new ReceiptTextLine("F1", 10, 48, 758, "Order Summary"));

		int y = 736;
		for (String detailLine : buildSummaryLines(order, address, product, paymentLabel, totalAmount, formatter)) {
			lines.add(new ReceiptTextLine("F1", 11, 48, y, detailLine));
			y -= 18;
		}

		y -= 8;
		lines.add(new ReceiptTextLine("F2", 13, 48, y, "Delivery Details"));
		y -= 22;

		for (String deliveryLine : buildDeliveryLines(address)) {
			lines.add(new ReceiptTextLine("F1", 11, 48, y, deliveryLine));
			y -= 18;
		}

		y -= 12;
		lines.add(new ReceiptTextLine("F2", 15, 48, y, "Total Paid: Rs. " + formatAmount(totalAmount)));
		lines.add(new ReceiptTextLine("F1", 10, 48, Math.max(64, y - 40),
				"Generated on " + formatter.format(LocalDateTime.now())));

		return renderSimplePdf(lines);
	}

	private List<String> buildSummaryLines(ProductOrder order, OrderAddress address, Product product, String paymentLabel,
			double totalAmount, DateTimeFormatter formatter) {
		List<String> lines = new ArrayList<>();
		lines.add("Receipt For: " + safeText(fullName(address == null ? null : address.getFirstName(),
				address == null ? null : address.getLastName())));
		lines.add("Order ID: " + safeText(order.getOrderId()));
		lines.add("Order Date: " + formatDate(order.getOrderDateTime(), order.getOrderDate(), formatter));
		lines.add("Product: " + safeText(product == null ? null : product.getTitle()));
		lines.add("Category: " + safeText(product == null ? null : product.getCategory()));
		lines.add("Quantity: " + (order.getQuantity() == null ? 0 : order.getQuantity()));
		lines.add("Unit Price: Rs. " + formatAmount(order.getPrice()));
		lines.add(paymentLabel + ": Rs. " + formatAmount(totalAmount));
		lines.add("Payment Type: " + safeText(order.getPaymentType()));
		lines.add("Payment Status: " + safeText(order.getPaymentStatus()));
		lines.add("Payment Reference: " + safeText(order.getPaymentId()));
		return wrapLines(lines, 80);
	}

	private List<String> buildDeliveryLines(OrderAddress address) {
		List<String> lines = new ArrayList<>();
		if (address == null) {
			lines.add("No delivery address saved for this order.");
			return lines;
		}
		lines.addAll(wrapSingleLine(fullName(address.getFirstName(), address.getLastName()), 80));
		lines.addAll(wrapSingleLine(joinAddress(address), 80));
		lines.addAll(wrapSingleLine(safeText(address.getEmail()) + " | " + safeText(address.getMobileNo()), 80));
		return lines;
	}

	private byte[] renderSimplePdf(List<ReceiptTextLine> lines) {
		try {
			StringBuilder content = new StringBuilder();
			for (ReceiptTextLine line : lines) {
				appendPdfText(content, line.fontName, line.fontSize, line.x, line.y, line.text);
			}

			byte[] contentBytes = content.toString().getBytes(StandardCharsets.US_ASCII);
			List<byte[]> objects = new ArrayList<>();
			objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
			objects.add("2 0 obj\n<< /Type /Pages /Count 1 /Kids [3 0 R] >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
			objects.add(("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
					+ "/Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> /Contents 4 0 R >>\nendobj\n")
					.getBytes(StandardCharsets.US_ASCII));
			objects.add(("4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n")
					.getBytes(StandardCharsets.US_ASCII));
			objects.add(contentBytes);
			objects.add("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
			objects.add("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
					.getBytes(StandardCharsets.US_ASCII));
			objects.add("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n"
					.getBytes(StandardCharsets.US_ASCII));

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			writeAscii(output, "%PDF-1.4\n");
			int[] offsets = new int[7];

			offsets[1] = output.size();
			output.write(objects.get(0));
			offsets[2] = output.size();
			output.write(objects.get(1));
			offsets[3] = output.size();
			output.write(objects.get(2));
			offsets[4] = output.size();
			output.write(objects.get(3));
			output.write(objects.get(4));
			output.write(objects.get(5));
			offsets[5] = output.size();
			output.write(objects.get(6));
			offsets[6] = output.size();
			output.write(objects.get(7));

			int xrefStart = output.size();
			writeAscii(output, "xref\n0 7\n");
			writeAscii(output, "0000000000 65535 f \n");
			for (int i = 1; i <= 6; i++) {
				writeAscii(output, String.format("%010d 00000 n \n", offsets[i]));
			}
			writeAscii(output, "trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n");
			writeAscii(output, String.valueOf(xrefStart));
			writeAscii(output, "\n%%EOF");
			return output.toByteArray();
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to generate order receipt PDF", ex);
		}
	}

	private void appendPdfText(StringBuilder content, String fontName, int fontSize, int x, int y, String text) {
		content.append("BT\n/");
		content.append(fontName);
		content.append(" ");
		content.append(fontSize);
		content.append(" Tf\n1 0 0 1 ");
		content.append(x);
		content.append(" ");
		content.append(y);
		content.append(" Tm\n(");
		content.append(toPdfText(text));
		content.append(") Tj\nET\n");
	}

	private List<String> wrapLines(List<String> lines, int maxLineLength) {
		List<String> wrappedLines = new ArrayList<>();
		for (String line : lines) {
			wrappedLines.addAll(wrapSingleLine(line, maxLineLength));
		}
		return wrappedLines;
	}

	private List<String> wrapSingleLine(String line, int maxLineLength) {
		List<String> wrappedLines = new ArrayList<>();
		String sanitized = safeText(line);
		if (sanitized.length() <= maxLineLength) {
			wrappedLines.add(sanitized);
			return wrappedLines;
		}

		int start = 0;
		while (start < sanitized.length()) {
			int end = Math.min(sanitized.length(), start + maxLineLength);
			if (end < sanitized.length()) {
				int splitAt = sanitized.lastIndexOf(' ', end);
				if (splitAt > start + 15) {
					end = splitAt;
				}
			}
			wrappedLines.add(sanitized.substring(start, end).trim());
			start = end;
			while (start < sanitized.length() && sanitized.charAt(start) == ' ') {
				start++;
			}
		}
		return wrappedLines;
	}

	private String joinAddress(OrderAddress address) {
		return safeText(address.getAddress()) + ", " + safeText(address.getCity()) + ", " + safeText(address.getState())
				+ " - " + safeText(address.getPincode());
	}

	private String fullName(String firstName, String lastName) {
		return (safeText(firstName) + " " + safeText(lastName)).trim();
	}

	private String formatDate(LocalDateTime orderDateTime, LocalDate orderDate, DateTimeFormatter formatter) {
		if (orderDateTime != null) {
			return formatter.format(orderDateTime);
		}
		return orderDate == null ? "-" : orderDate.toString();
	}

	private String formatAmount(Double amount) {
		return formatAmount(amount == null ? 0.0 : amount.doubleValue());
	}

	private String formatAmount(double amount) {
		return String.format("%.2f", amount);
	}

	private String safeText(String value) {
		return value == null || value.isBlank() ? "-" : value.trim();
	}

	private String toPdfText(String value) {
		String normalized = Normalizer.normalize(safeText(value), Normalizer.Form.NFKD).replaceAll("[^\\x20-\\x7E]", "");
		return normalized.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
	}

	private void writeAscii(ByteArrayOutputStream output, String value) throws IOException {
		output.write(value.getBytes(StandardCharsets.US_ASCII));
	}

	private static class ReceiptTextLine {
		private final String fontName;
		private final int fontSize;
		private final int x;
		private final int y;
		private final String text;

		private ReceiptTextLine(String fontName, int fontSize, int x, int y, String text) {
			this.fontName = fontName;
			this.fontSize = fontSize;
			this.x = x;
			this.y = y;
			this.text = text;
		}
	}

}
