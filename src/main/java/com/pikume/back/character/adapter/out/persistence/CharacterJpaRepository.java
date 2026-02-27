package com.pikume.back.character.adapter.out.persistence;

import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterJpaRepository extends JpaRepository<Character, Long> {
	List<Character> findByType(CharacterCreationType type);
}
