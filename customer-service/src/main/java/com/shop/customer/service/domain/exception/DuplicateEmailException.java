package com.shop.customer.service.domain.exception;

import com.shop.customer.service.domain.model.Email;

public class DuplicateEmailException extends RuntimeException {
	
	public DuplicateEmailException(Email email) {
		super("Email '" + email.value() + "' is already registered");
	}

}
