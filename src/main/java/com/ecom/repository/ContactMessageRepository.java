package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Integer> {

	List<ContactMessage> findAllByOrderByCreatedAtDesc();
}
