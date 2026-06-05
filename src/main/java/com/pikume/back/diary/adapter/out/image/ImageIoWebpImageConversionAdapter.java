package com.pikume.back.diary.adapter.out.image;

import com.luciad.imageio.webp.WebPImageWriterSpi;
import com.pikume.back.diary.application.port.out.WebpImageConversionPort;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
public class ImageIoWebpImageConversionAdapter implements WebpImageConversionPort {

	@Override
	public byte[] convertToWebp(byte[] imageBytes, float quality) {
		try {
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
		} catch (Exception e) {
			throw new RuntimeException("WebP 변환에 실패했습니다.", e);
		}
	}

	private ImageWriter webpWriter() {
		return new WebPImageWriterSpi().createWriterInstance(null);
	}

	private float normalizeQuality(float quality) {
		if (quality < 0.0f) {
			return 0.0f;
		}
		if (quality > 1.0f) {
			return 1.0f;
		}
		return quality;
	}
}
