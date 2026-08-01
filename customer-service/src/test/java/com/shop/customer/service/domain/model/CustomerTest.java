package com.shop.customer.service.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.shop.customer.service.domain.exception.CustomerValidationException;

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

        @Test
        void shouldRejectEmptyFullName() {
            assertThatThrownBy(() -> Customer.create(
                    "", DocumentType.CE, "99887766", email, "+573002222222", "Carrera 45"))
                    .isInstanceOf(CustomerValidationException.class);
        }

        @Test
        void shouldRejectBlankFullName() {
            assertThatThrownBy(() -> Customer.create(
                    "   ", DocumentType.CE, "99887766", email, "+573002222222", "Carrera 45"))
                    .isInstanceOf(CustomerValidationException.class);
        }

        @Test
        void shouldRejectFullNameWithOneCharacter() {
            assertThatThrownBy(() -> Customer.create(
                    "A", DocumentType.CE, "99887766", email, "+573002222222", "Carrera 45"))
                    .isInstanceOf(CustomerValidationException.class);
        }

        @Test
        void shouldAcceptFullNameWithExactly120Characters() {
            String fullName = "A".repeat(120);

            Customer customer = Customer.create(
                    fullName, DocumentType.CE, "99887766", email, "+573002222222", "Carrera 45");

            assertThat(customer.getFullName()).hasSize(120);
        }

        @Test
        void shouldRejectFullNameWith121Characters() {
            String fullName = "A".repeat(121);

            assertThatThrownBy(() -> Customer.create(
                    fullName, DocumentType.CE, "99887766", email, "+573002222222", "Carrera 45"))
                    .isInstanceOf(CustomerValidationException.class);
        }

        @Test
        void shouldRejectDocumentNumberWithFourCharacters() {
            assertThatThrownBy(() -> Customer.create(
                    "Ana García", DocumentType.CE, "1234", email, "+573002222222", "Carrera 45"))
                    .isInstanceOf(CustomerValidationException.class);
        }

        @Test
        void shouldRejectDocumentNumberWith21Characters() {
            String documentNumber = "1".repeat(21);

            assertThatThrownBy(() -> Customer.create(
                    "Ana García", DocumentType.CE, documentNumber, email, "+573002222222", "Carrera 45"))
                    .isInstanceOf(CustomerValidationException.class);
        }

	}
	
	@Nested
	class Reconstruct {

		private UUID id;
		private Instant createdAt;
		private Instant updatedAt;
		private Customer customer;

		@BeforeEach
		void setUp() {
			id = UUID.randomUUID();
			Email email = new Email("ana@x.com");
			createdAt = Instant.parse("2024-01-10T10:00:00Z");
			updatedAt = Instant.parse("2024-03-05T15:30:00Z");

			customer = Customer.reconstruct(
					id, "Ana García", DocumentType.CE, "99887766", email,
					"+573002222222", "Carrera 45",
					CustomerStatus.INACTIVE, createdAt, updatedAt);
		}

		@Test
		void shouldRespectHistoricalInactiveStatus() {
			assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
		}

		@Test
		void shouldPreserveGivenIdAndTimestamps() {
			assertThat(customer.getId()).isEqualTo(id);
			assertThat(customer.getCreatedAt()).isEqualTo(createdAt);
			assertThat(customer.getUpdatedAt()).isEqualTo(updatedAt);
		}
	}

    @Nested
	class Activate {

		@Test
		void shouldActivateInactiveCustomerAndUpdateTimestamp() {
			Instant pastUpdatedAt = Instant.parse("2024-01-10T10:00:00Z");
			Customer customer = Customer.reconstruct(
					UUID.randomUUID(), "Ana García", DocumentType.CE, "99887766",
					new Email("ana@x.com"), "+573002222222", "Carrera 45",
					CustomerStatus.INACTIVE, pastUpdatedAt, pastUpdatedAt);

			customer.activate();

			assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
			assertThat(customer.getUpdatedAt()).isAfter(pastUpdatedAt);
		}

        @Test
		void shouldRemainActiveAndUpdateTimestampWhenAlreadyActive() throws InterruptedException {
			Customer customer = Customer.create(
					"Ana García", DocumentType.CE, "99887766",
					new Email("ana@x.com"), "+573002222222", "Carrera 45");
			Instant beforeActivate = customer.getUpdatedAt();

			// Pausa mínima: sin ella, dos Instant.now() consecutivos (en create() y en
			// activate()) pueden caer en el mismo instante en relojes de alta resolución,
			// haciendo el test flaky sin que el código esté mal.
			Thread.sleep(1);
			customer.activate();

			assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
			assertThat(customer.getUpdatedAt()).isAfter(beforeActivate);
		}
	}

    @Nested
	class Deactivate {

		@Test
		void shouldInactivateActiveCustomerAndUpdateTimestamp() {
			Instant pastUpdatedAt = Instant.parse("2024-01-10T10:00:00Z");
			Customer customer = Customer.reconstruct(
					UUID.randomUUID(), "Ana García", DocumentType.CE, "99887766",
					new Email("ana@x.com"), "+573002222222", "Carrera 45",
					CustomerStatus.ACTIVE, pastUpdatedAt, pastUpdatedAt);

			customer.deactivate();

			assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
			assertThat(customer.getUpdatedAt()).isAfter(pastUpdatedAt);
		}

        @Test
		void shouldRemainInactiveAndUpdateTimestampWhenAlreadyInactive() throws InterruptedException {
			Instant pastUpdatedAt = Instant.parse("2024-01-10T10:00:00Z");
			Customer customer = Customer.reconstruct(
					UUID.randomUUID(), "Ana García", DocumentType.CE, "99887766",
					new Email("ana@x.com"), "+573002222222", "Carrera 45",
					CustomerStatus.INACTIVE, pastUpdatedAt, pastUpdatedAt);

			// Pausa mínima: sin ella, dos Instant.now() consecutivos pueden caer en el
			// mismo instante en relojes de alta resolución, haciendo el test flaky sin
			// que el código esté mal.
			Thread.sleep(1);
			customer.deactivate();

			assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
			assertThat(customer.getUpdatedAt()).isAfter(pastUpdatedAt);
		}
	}

	@Nested
	class UpdateContact {

		private UUID id;
		private Instant createdAt;
		private Customer customer;

		@BeforeEach
		void setUp() {
			Instant pastInstant = Instant.parse("2024-01-10T10:00:00Z");
			customer = Customer.reconstruct(
					UUID.randomUUID(), "Ana García", DocumentType.CE, "99887766",
					new Email("ana@x.com"), "+573002222222", "Carrera 45",
					CustomerStatus.ACTIVE, pastInstant, pastInstant);
			id = customer.getId();
			createdAt = customer.getCreatedAt();
		}

		@Test
		void shouldUpdateContactFieldsAndTimestamp() throws InterruptedException {
			Instant beforeUpdate = customer.getUpdatedAt();
			Email newEmail = new Email("nueva@x.com");

			Thread.sleep(1);
			customer.updateContact("Ana Ruiz", newEmail, "+573009999999", "Nueva Dirección");

			assertThat(customer.getFullName()).isEqualTo("Ana Ruiz");
			assertThat(customer.getEmail()).isEqualTo(newEmail);
			assertThat(customer.getPhone()).isEqualTo("+573009999999");
			assertThat(customer.getAddress()).isEqualTo("Nueva Dirección");
			assertThat(customer.getUpdatedAt()).isAfter(beforeUpdate);
		}

		@Test
		void shouldNotChangeStatusIdOrCreatedAt() {
			customer.updateContact("Ana Ruiz", new Email("nueva@x.com"), "+573009999999", "Nueva Dirección");

			assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
			assertThat(customer.getId()).isEqualTo(id);
			assertThat(customer.getCreatedAt()).isEqualTo(createdAt);
		}

		@Test
		void shouldReplaceEmailValueObjectRatherThanMutateIt() {
			Email originalEmail = customer.getEmail();
			Email newEmail = new Email("nueva@x.com");

			customer.updateContact("Ana Ruiz", newEmail, "+573009999999", "Nueva Dirección");

			assertThat(customer.getEmail()).isEqualTo(newEmail);
			assertThat(customer.getEmail()).isNotEqualTo(originalEmail);
		}
	}
}
