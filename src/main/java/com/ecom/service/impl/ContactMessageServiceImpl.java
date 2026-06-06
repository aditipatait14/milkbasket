package com.ecom.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.ContactMessage;
import com.ecom.repository.ContactMessageRepository;
import com.ecom.service.ContactMessageService;

@Service
public class ContactMessageServiceImpl implements ContactMessageService {

	@Autowired
	private ContactMessageRepository contactMessageRepository;

	@Override
	public ContactMessage save(ContactMessage contactMessage) {
		contactMessage.setCreatedAt(LocalDateTime.now());
		if (contactMessage.getStatus() == null || contactMessage.getStatus().isBlank()) {
			contactMessage.setStatus("New");
		}
		return contactMessageRepository.save(contactMessage);
	}

	@Override
	public List<ContactMessage> getAll() {
		return contactMessageRepository.findAllByOrderByCreatedAtDesc();
	}

	@Override
	public ContactMessage getById(Integer id) {
		return contactMessageRepository.findById(id).orElse(null);
	}

	@Override
	public ContactMessage markAsViewed(Integer id) {
		ContactMessage contactMessage = getById(id);
		if (contactMessage == null) {
			return null;
		}
		if (contactMessage.getStatus() == null || "New".equalsIgnoreCase(contactMessage.getStatus())) {
			contactMessage.setStatus("Viewed");
			return contactMessageRepository.save(contactMessage);
		}
		return contactMessage;
	}

	@Override
	public ContactMessage saveResponse(Integer id, String response) {
		ContactMessage contactMessage = getById(id);
		if (contactMessage == null) {
			return null;
		}
		contactMessage.setAdminResponse(response);
		contactMessage.setRespondedAt(LocalDateTime.now());
		contactMessage.setStatus("Responded");
		return contactMessageRepository.save(contactMessage);
	}

	@Override
	public void deleteById(Integer id) {
		contactMessageRepository.deleteById(id);
	}
}
