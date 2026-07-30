package com.shop.customer.service.domain.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {
	
    public CustomerNotFoundException(UUID id) {
        super("Customer with id '" + id + "' not found");
    }

    public CustomerNotFoundException(String message) {
        super(message);
    }
    
}
