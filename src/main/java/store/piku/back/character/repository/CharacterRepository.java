package store.piku.back.character.repository;

import store.piku.back.character.entity.Character;
import store.piku.back.character.enums.CharacterCreationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {
    List<Character> findByType(CharacterCreationType type);
} 