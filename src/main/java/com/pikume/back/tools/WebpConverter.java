package com.pikume.back.tools;

import com.luciad.imageio.webp.WebPImageWriterSpi;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class WebpConverter {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		try {
			System.out.println("=== WebP 이미지 변환기 ===");
			System.out.println();

			Path inputPath = readPath(scanner, "입력 이미지 경로를 입력하세요: ");
			Path outputPath = readPath(scanner, "출력 WebP 경로를 입력하세요: ");
			float quality = readQuality(scanner);

			byte[] webpBytes = convertToWebp(Files.readAllBytes(inputPath), quality);
			Path parent = outputPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.write(outputPath, webpBytes);

			System.out.println("WebP 변환 완료: " + outputPath);
			System.out.println("품질: " + normalizeQuality(quality));
			System.out.println("파일 크기: " + webpBytes.length + " bytes");
		} catch (Exception e) {
			System.err.println("WebP 변환에 실패했습니다: " + e.getMessage());
		} finally {
			scanner.close();
		}
	}

	public static byte[] convertToWebp(byte[] imageBytes, float quality) throws IOException {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
		if (image == null) {
			throw new IllegalArgumentException("이미지 bytes를 읽을 수 없습니다.");
		}

		ImageWriter writer = webpWriter();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
			writer.setOutput(imageOutputStream);
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			if (writeParam.canWriteCompressed()) {
				writeParam.setCompressionQuality(normalizeQuality(quality));
			}
			writer.write(null, new IIOImage(image, null, null), writeParam);
		} finally {
			writer.dispose();
		}

		return outputStream.toByteArray();
	}

	private static ImageWriter webpWriter() throws IOException {
		return new WebPImageWriterSpi().createWriterInstance(null);
	}

	private static Path readPath(Scanner scanner, String prompt) {
		while (true) {
			System.out.print(prompt);
			String path = scanner.nextLine().trim();
			if (!path.isEmpty()) {
				return Path.of(path);
			}

			System.out.println("경로를 입력해주세요.");
		}
	}

	private static float readQuality(Scanner scanner) {
		while (true) {
			System.out.print("품질을 입력하세요 (0.0 ~ 1.0): ");
			String rawQuality = scanner.nextLine().trim();
			try {
				float quality = Float.parseFloat(rawQuality);
				if (quality >= 0.0f && quality <= 1.0f) {
					return quality;
				}
			} catch (NumberFormatException ignored) {
			}

			System.out.println("품질은 0.0부터 1.0 사이의 숫자여야 합니다.");
		}
	}

	private static float normalizeQuality(float quality) {
		if (quality < 0.0f) {
			return 0.0f;
		}
		if (quality > 1.0f) {
			return 1.0f;
		}
		return quality;
	}

}
