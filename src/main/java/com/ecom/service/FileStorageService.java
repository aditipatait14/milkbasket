package com.ecom.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {

	private static final Set<String> SAFE_STATIC_FILES = Set.of("ecom.png", "ecom1.png", "ecom2.jpg", "ecom3.jpg",
			"default.jpg");

	@Value("${app.upload.dir:${user.dir}/uploads}")
	private String uploadDir;

	private Path uploadRoot;

	@PostConstruct
	public void init() {
		try {
			uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
			Files.createDirectories(uploadRoot.resolve("category"));
			Files.createDirectories(uploadRoot.resolve("product"));
			Files.createDirectories(uploadRoot.resolve("profile"));
		} catch (IOException e) {
			throw new IllegalStateException("Unable to initialize upload directories", e);
		}
	}

	public String storeCategoryImage(MultipartFile file) {
		return store(file, "category");
	}

	public String storeProductImage(MultipartFile file) {
		return store(file, "product");
	}

	public String storeProfileImage(MultipartFile file) {
		return store(file, "profile");
	}

	public void deleteCategoryImage(String fileName) {
		delete("category", fileName);
	}

	public void deleteProductImage(String fileName) {
		delete("product", fileName);
	}

	public void deleteProfileImage(String fileName) {
		delete("profile", fileName);
	}

	public String resolveCategoryImage(String fileName) {
		if ("ecom.png".equalsIgnoreCase(fileName)) {
			return "/img/ecom.png";
		}
		return resolve("category", "category_img", fileName, "/img/ecom.png");
	}

	public String resolveProductImage(String fileName) {
		if ("ecom.png".equalsIgnoreCase(fileName)) {
			return "/img/ecom.png";
		}
		return resolve("product", "product_img", fileName, "/img/ecom.png");
	}

	public String resolveProfileImage(String fileName) {
		return resolve("profile", "profile_img", fileName, "/img/ecom.png");
	}

	public String getUploadRootLocation() {
		return uploadRoot.toUri().toString();
	}

	private String store(MultipartFile file, String folder) {
		if (file == null || file.isEmpty()) {
			return null;
		}

		String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
		String extension = "";
		int extensionIndex = originalFilename.lastIndexOf('.');
		if (extensionIndex >= 0) {
			extension = originalFilename.substring(extensionIndex);
		}

		String generatedFileName = UUID.randomUUID() + extension;
		Path destination = uploadRoot.resolve(folder).resolve(generatedFileName).normalize();

		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to save uploaded file", e);
		}
		return generatedFileName;
	}

	private void delete(String folder, String fileName) {
		if (!StringUtils.hasText(fileName) || SAFE_STATIC_FILES.contains(fileName)) {
			return;
		}

		try {
			Files.deleteIfExists(uploadRoot.resolve(folder).resolve(fileName));
		} catch (IOException ignored) {
		}
	}

	private String resolve(String uploadFolder, String staticFolder, String fileName, String fallback) {
		if (!StringUtils.hasText(fileName)) {
			return fallback;
		}

		Path uploadedFile = uploadRoot.resolve(uploadFolder).resolve(fileName);
		if (Files.exists(uploadedFile)) {
			return "/uploads/" + uploadFolder + "/" + fileName;
		}
		return "/img/" + staticFolder + "/" + fileName;
	}
}
