package com.ecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.StoreSetting;

public interface StoreSettingRepository extends JpaRepository<StoreSetting, Integer> {

}
