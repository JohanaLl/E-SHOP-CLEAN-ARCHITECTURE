package com.shop.customer.service.domain.exception;

public class InvalidEmailException extends RuntimeException {

	public InvalidEmailException(String email) {
		super("Email not valid: " + email);
	}
}
