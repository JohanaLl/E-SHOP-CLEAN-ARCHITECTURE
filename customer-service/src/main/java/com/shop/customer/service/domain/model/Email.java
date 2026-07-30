package com.shop.customer.service.domain.model;

import java.util.regex.Pattern;

import com.shop.customer.service.domain.exception.InvalidEmailException;

public record Email(String value) {

	private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	
	public Email {
		if (value == null) 
			throw new InvalidEmailException(value);
		
		value = value.trim().toLowerCase();
		
		if (!FORMAT.matcher(value).matches())
			throw new InvalidEmailException(value);
		
	}
	
}
