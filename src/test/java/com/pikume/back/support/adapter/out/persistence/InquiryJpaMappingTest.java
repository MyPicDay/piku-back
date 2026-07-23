package com.pikume.back.support.adapter.out.persistence;

import com.pikume.back.support.domain.Inquiry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Inquiry JPA mapping")
class InquiryJpaMappingTest {

	@Autowired
	private InquiryJpaRepository inquiryJpaRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("첨부 저장 참조를 기존 image_url 열에 기록하고 다시 읽는다")
	void mapsAttachmentReferenceToExistingColumn() {
		Inquiry recorded = inquiryJpaRepository.saveAndFlush(
				Inquiry.submit("user-1", "문의 내용", "inquiry/path/image.png"));
		entityManager.clear();

		Inquiry reloaded = inquiryJpaRepository.findById(recorded.getId()).orElseThrow();

		assertThat(reloaded.getAttachmentReference()).isEqualTo("inquiry/path/image.png");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT image_url FROM inquiry WHERE id = ?",
				String.class,
				recorded.getId())).isEqualTo("inquiry/path/image.png");
	}
}
