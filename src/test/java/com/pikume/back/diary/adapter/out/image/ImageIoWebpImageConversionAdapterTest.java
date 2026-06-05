package com.pikume.back.diary.adapter.out.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImageIoWebpImageConversionAdapter")
class ImageIoWebpImageConversionAdapterTest {

	@Test
	@DisplayName("PNG bytes를 WebP bytes로 변환한다")
	void convertsPngBytesToWebpBytes() throws Exception {
		ImageIoWebpImageConversionAdapter adapter = new ImageIoWebpImageConversionAdapter();

		byte[] result = adapter.convertToWebp(pngBytes(), 0.82f);

		assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
		assertThat(new String(result, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WEBP");
	}

	@Test
	@DisplayName("이미지로 읽을 수 없는 bytes는 변환 실패로 처리한다")
	void failsWhenBytesCannotBeReadAsImage() {
		ImageIoWebpImageConversionAdapter adapter = new ImageIoWebpImageConversionAdapter();

		assertThatThrownBy(() -> adapter.convertToWebp("not-image".getBytes(StandardCharsets.UTF_8), 0.82f))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("WebP 변환에 실패했습니다");
	}

	private byte[] pngBytes() throws Exception {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, Color.RED.getRGB());
		image.setRGB(1, 0, Color.GREEN.getRGB());
		image.setRGB(0, 1, Color.BLUE.getRGB());
		image.setRGB(1, 1, Color.WHITE.getRGB());

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", outputStream);
		return outputStream.toByteArray();
	}
}
