package com.pikume.back.character.application.port.in;

import com.pikume.back.character.application.dto.CharacterImageReferenceQuery;
import com.pikume.back.character.application.dto.CharacterImageReferenceResult;

import java.util.Collection;
import java.util.List;

public interface QueryCharacterImageReferencesUseCase {

	List<CharacterImageReferenceResult> queryCharacterImageReferences(
			Collection<CharacterImageReferenceQuery> queries);
}
