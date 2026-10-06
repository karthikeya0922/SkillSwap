package com.skillswap.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.skillswap.common.exception.BadRequestException;

/**
 * Stores uploaded images on local disk under {@code app.uploads.dir} and serves them from {@code /uploads/**}.
 * Files are validated by their magic bytes, not just the client-supplied content type.
 */
@Service
public class FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
	private static final long MAX_BYTES = 2 * 1024 * 1024;
	private static final Map<String, String> EXTENSIONS = Map.of("image/png", "png", "image/jpeg", "jpg",
			"image/webp", "webp", "image/gif", "gif");

	private final Path root;

	public FileStorageService(@Value("${app.uploads.dir}") String uploadsDir) {
		this.root = Path.of(uploadsDir).toAbsolutePath().normalize();
	}

	public Path getRoot() {
		return root;
	}

	/** Saves an image into {@code folder} and returns its public URL path (e.g. /uploads/avatars/x.png). */
	public String storeImage(MultipartFile file, String folder) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("Choose an image to upload.");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new BadRequestException("Images must be 2 MB or smaller.");
		}
		String type = detectImageType(file);
		if (type == null) {
			throw new BadRequestException("Only PNG, JPEG, WebP or GIF images are allowed.");
		}
		String filename = UUID.randomUUID() + "." + EXTENSIONS.get(type);
		try {
			Path dir = root.resolve(folder).normalize();
			if (!dir.startsWith(root)) {
				throw new BadRequestException("Invalid upload folder.");
			}
			Files.createDirectories(dir);
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not store the uploaded file", ex);
		}
		return "/uploads/" + folder + "/" + filename;
	}

	/** Deletes a previously stored file; ignores URLs that do not point into the uploads folder. */
	public void deleteByUrl(String url) {
		if (url == null || !url.startsWith("/uploads/")) {
			return;
		}
		Path target = root.resolve(url.substring("/uploads/".length())).normalize();
		if (!target.startsWith(root)) {
			return;
		}
		try {
			Files.deleteIfExists(target);
		}
		catch (IOException ex) {
			log.warn("Could not delete old upload {}", target, ex);
		}
	}

	private static String detectImageType(MultipartFile file) {
		byte[] header = new byte[12];
		try (InputStream in = file.getInputStream()) {
			int read = in.readNBytes(header, 0, header.length);
			if (read < 4) {
				return null;
			}
		}
		catch (IOException ex) {
			return null;
		}
		if ((header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
			return "image/png";
		}
		if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}
		if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
			return "image/gif";
		}
		if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
				&& header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
			return "image/webp";
		}
		return null;
	}
}
