package com.shop.customer.service.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.shop.customer.service.domain.exception.InvalidEmailException;

public class EmailTest {

	@Test
	void shouldNormalizeToLowercase() {
		Email email = new Email("Ana@X.com");

		assertThat(email.value()).isEqualTo("ana@x.com");
	}

	@Test
	void shouldTrimWhitespace() {
		Email email = new Email("  ana@x.com  ");

		assertThat(email.value()).isEqualTo("ana@x.com");
	}

	@Test
	void shouldBeEqualWhenNormalizedValueMatches() {
		Email email1 = new Email("ana@x.com");
		Email email2 = new Email("ANA@X.COM");

		assertThat(email1).isEqualTo(email2);
		assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
	}

	@Test
	void shouldRejectEmailWithoutAtSymbol() {
		assertThatThrownBy(() -> new Email("sinArroba.com"))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void shouldRejectEmailWithoutLocalPart() {
		assertThatThrownBy(() -> new Email("@x.com"))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void shouldRejectEmailWithoutDomain() {
		assertThatThrownBy(() -> new Email("ana@"))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void shouldRejectEmptyEmail() {
		assertThatThrownBy(() -> new Email(""))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void shouldRejectNullEmail() {
		assertThatThrownBy(() -> new Email(null))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void shouldAcceptMinimalValidEmail() {
		Email email = new Email("a@b.co");

		assertThat(email.value()).isEqualTo("a@b.co");
	}
}
