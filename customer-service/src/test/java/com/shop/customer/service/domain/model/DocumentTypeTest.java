package com.shop.customer.service.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DocumentTypeTest {

	@Test
	void shouldContainExactlyCcNitCeAndPassport() {
		assertThat(DocumentType.values())
				.containsExactlyInAnyOrder(
						DocumentType.CC, DocumentType.NIT, DocumentType.CE, DocumentType.PASSPORT);
	}
}
