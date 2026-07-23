package com.pikume.back.creative.adapter.out.storage;

import com.pikume.back.global.port.out.LoadObjectPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReferenceImageObjectAdapter")
class ReferenceImageObjectAdapterTest {

	@Mock
	private LoadObjectPort loadObjectPort;

	@Test
	@DisplayName("같은 고정 캐릭터 Object Key는 한 번만 Storage에서 읽는다")
	void cachesFixedCharacterObject() {
		String objectKey = "public/characters/fixed/base.webp";
		given(loadObjectPort.loadObject(objectKey))
				.willReturn("image".getBytes(StandardCharsets.UTF_8));
		ReferenceImageObjectAdapter adapter = new ReferenceImageObjectAdapter(loadObjectPort);

		byte[] first = adapter.loadReferenceImage(objectKey);
		first[0] = 'X';
		byte[] second = adapter.loadReferenceImage(objectKey);

		assertThat(second).containsExactly("image".getBytes(StandardCharsets.UTF_8));
		verify(loadObjectPort, times(1)).loadObject(objectKey);
	}
}
