package com.pikume.back.creative.adapter.out.storage;

import com.pikume.back.creative.application.port.out.LoadReferenceImageObjectPort;
import com.pikume.back.global.port.out.LoadObjectPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReferenceImageObjectAdapter implements LoadReferenceImageObjectPort {

	private static final String FIXED_CHARACTER_PREFIX = "public/characters/fixed/";
	private static final int FIXED_CHARACTER_CACHE_MAX_SIZE = 32;

	private final LoadObjectPort loadObjectPort;
	private final Map<String, byte[]> fixedCharacterCache = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
			return size() > FIXED_CHARACTER_CACHE_MAX_SIZE;
		}
	};

	@Override
	public byte[] loadReferenceImage(String objectKey) {
		if (!objectKey.startsWith(FIXED_CHARACTER_PREFIX)) {
			return copy(loadObjectPort.loadObject(objectKey));
		}
		synchronized (fixedCharacterCache) {
			return copy(fixedCharacterCache.computeIfAbsent(objectKey, loadObjectPort::loadObject));
		}
	}

	private byte[] copy(byte[] bytes) {
		return bytes == null ? new byte[0] : bytes.clone();
	}
}
