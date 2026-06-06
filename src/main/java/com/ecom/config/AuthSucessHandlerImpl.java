package com.ecom.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthSucessHandlerImpl implements AuthenticationSuccessHandler {

	private final UserRepository userRepository;

	public AuthSucessHandlerImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		
		Set<String> roles = AuthorityUtils.authorityListToSet(authorities);
		UserDtls user = userRepository.findByEmail(authentication.getName());
		if (user != null) {
			user.setFailedAttempt(0);
			user.setAccountNonLocked(true);
			userRepository.save(user);
		}
		
		if(roles.contains("ROLE_ADMIN"))
		{
			response.sendRedirect("/admin/");
		}else {
			response.sendRedirect("/");
		}
		
	}

}
