package com.shop.customer.service.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CustomerStatusTest {

	@Test
	void shouldContainExactlyActiveAndInactive() {
		assertThat(CustomerStatus.values())
				.containsExactlyInAnyOrder(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE);
	}
}
