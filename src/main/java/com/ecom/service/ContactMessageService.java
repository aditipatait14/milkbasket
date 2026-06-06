package com.ecom.service;

import java.util.List;

import com.ecom.model.ContactMessage;

public interface ContactMessageService {

	ContactMessage save(ContactMessage contactMessage);

	List<ContactMessage> getAll();

	ContactMessage getById(Integer id);

	ContactMessage markAsViewed(Integer id);

	ContactMessage saveResponse(Integer id, String response);

	void deleteById(Integer id);
}
