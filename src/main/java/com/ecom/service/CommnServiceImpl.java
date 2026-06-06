package com.ecom.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class CommnServiceImpl implements CommonService {

	@Value("${rupee.sign}")
	public String rupeeSign;
	
	@Override
	public void removeSessionMessage() {
		HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.getRequestAttributes()))
				.getRequest();
		HttpSession session = request.getSession();
		session.removeAttribute("succMsg");
		session.removeAttribute("errorMsg");
	}
	
	@Override
	public String rupeeSign()
	{
		return rupeeSign;
	}

	@Override
	public String formatPrice(Double amount) {
		if (amount == null) {
			return "0.00";
		}
		return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	@Override
	public void removeOrderSuccessData() {
		HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.getRequestAttributes()))
				.getRequest();
		HttpSession session = request.getSession();
		session.removeAttribute("orderSuccessMessage");
		session.removeAttribute("paymentType");
		session.removeAttribute("paymentReference");
	}
}
