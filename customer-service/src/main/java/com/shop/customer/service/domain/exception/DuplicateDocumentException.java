package com.shop.customer.service.domain.exception;

import com.shop.customer.service.domain.model.DocumentType;

public class DuplicateDocumentException extends RuntimeException {
	
    public DuplicateDocumentException(DocumentType documentType, String documentNumber) {
        super("Customer with document " + documentType + "-" + documentNumber + " already exists");
    }
    
}
