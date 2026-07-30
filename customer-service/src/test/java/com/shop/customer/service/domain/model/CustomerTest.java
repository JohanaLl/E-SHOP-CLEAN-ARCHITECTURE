package com.shop.customer.service.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CustomerTest {

	@Nested
	class Create {
		
		private Email email;
		private Customer customer;
		
		@BeforeEach
	    void setUp() {
	        email = new Email("ana@x.com");
	        customer = Customer.create("Ana García", DocumentType.CE, "99887766",
	                                    email, "+573002222222", "Carrera 45");
	    }
		
		@Test
		void shouldStoreAllFieldsExactly() {			
			assertThat(customer.getFullName()).isEqualTo("Ana García");
			assertThat(customer.getDocumentType()).isEqualTo(DocumentType.CE);
            assertThat(customer.getDocumentNumber()).isEqualTo("99887766");
            assertThat(customer.getEmail()).isEqualTo(email);
            assertThat(customer.getPhone()).isEqualTo("+573002222222");
            assertThat(customer.getAddress()).isEqualTo("Carrera 45");

		}
		
		@Test
		void shouldReturnActiveStatus() {
			assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
		}
		
        @Test
        void shouldGenerateNonNullId() {
            assertThat(customer.getId()).isNotNull();
        }
        
        @Test
        void shouldSetCreatedAtAndUpdatedAtToSameInstant() {
			assertThat(customer.getCreatedAt()).isNotNull();
			assertThat(customer.getUpdatedAt()).isNotNull();
			assertThat(customer.getCreatedAt()).isEqualTo(customer.getUpdatedAt());
        }
        
        @Test
        void shouldAcceptNullPhoneAndAddress() {
			Customer customer = Customer.create(
                    "Ana García", DocumentType.CE, "99887766",
                    email, null, null);

            assertThat(customer.getPhone()).isNull();
            assertThat(customer.getAddress()).isNull();
        }
        
        @Test
        void shouldGenerateUniqueIdForEachCustomer() {

            Customer customer1 = customer;
            Customer customer2 = Customer.create(
                    "Luis Pérez", DocumentType.CE, "55667788",
                    email, "+573004444444", "Carrera 10");

            assertThat(customer1.getId()).isNotEqualTo(customer2.getId());
        }

	}
}
