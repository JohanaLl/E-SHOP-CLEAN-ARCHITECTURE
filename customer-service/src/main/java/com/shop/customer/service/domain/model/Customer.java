package com.shop.customer.service.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.shop.customer.service.domain.exception.CustomerValidationException;

public class Customer {

	private UUID id;
	private String fullName;
	private DocumentType documentType;
	private String documentNumber;
	private Email email;
	private String phone;
	private String address;
	private CustomerStatus status;
	private Instant createdAt;
	private Instant updatedAt;

	private Customer(UUID id, 
			String fullName, 
			DocumentType documentType, 
			String documentNumber, 
			Email email,
			String phone, 
			String address, 
			CustomerStatus status, 
			Instant createdAt, 
			Instant updatedAt) {
		validateFullName(fullName);
		validateDocumentNumber(documentNumber);
		validatePhone(phone);
		validateAddress(address);
		if (documentType == null) throw new CustomerValidationException("documentType is mandatory");
		if (email == null) throw new CustomerValidationException("email is mandatory");
		this.id = id;
		this.fullName = fullName;
		this.documentType = documentType;
		this.documentNumber = documentNumber;
		this.email = email;
		this.phone = phone;
		this.address = address;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static Customer create(String fullName, 
			DocumentType documentType, 
			String documentNumber, 
			Email email,
			String phone, 
			String address) {
			Instant now = Instant.now();
		return new Customer(UUID.randomUUID(), fullName, documentType, documentNumber, email, phone, address,
				CustomerStatus.ACTIVE, now, now);
	}

	public static Customer reconstruct(UUID id, 
			String fullName, 
			DocumentType documentType, 
			String documentNumber,
			Email email, 
			String phone, 
			String address, 
			CustomerStatus status, 
			Instant createdAt,
			Instant updatedAt) {
		return new Customer(id, fullName, documentType, documentNumber, email, phone, address, status, createdAt,
				updatedAt);
	}
	
    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }
    
    public void activate() {
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }
    
    public void updateContact(String fullName, Email email, String phone, String address) {
		validateFullName(fullName);
		validatePhone(phone);
		validateAddress(address);
		if (email == null) throw new CustomerValidationException("email is mandatory");
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.updatedAt = Instant.now();
    } 
    
    private static void validateFullName(String fullName) {
    	if (fullName == null || fullName.isBlank()) 
            throw new CustomerValidationException("fullName is mandatory");
        
        if (fullName.length() < 2 || fullName.length() > 120) 
            throw new CustomerValidationException("fullName should have between 2 and 120 caracters");
    }
    
    private static void validateDocumentNumber(String documentNumber) {
    	if (documentNumber == null || documentNumber.isBlank()) 
            throw new CustomerValidationException("documentNumber is mandatory");
        
        if (documentNumber.length() < 5 || documentNumber.length() > 20) 
            throw new CustomerValidationException("documentNumber should have between 5 and 20 caracters");
        
        if (!documentNumber.matches("^[A-Za-z0-9]+$")) 
            throw new CustomerValidationException("documentNumber should only have lletters and numbers");
    }
    
    private static void validatePhone(String phone) {
    	if (phone == null) return;
        if (phone.length() < 7 || phone.length() > 20) 
            throw new CustomerValidationException("phone should have between 7 and 20 caracters");
    }
    
    private static void validateAddress(String address) {
    	if (address == null) return;
        if (address.length() > 200) 
            throw new CustomerValidationException("address should only have 200 caracters");
    }
    

    public UUID getId()                  { return id; }
    public String getFullName()          { return fullName; }
    public DocumentType getDocumentType(){ return documentType; }
    public String getDocumentNumber()    { return documentNumber; }
    public Email getEmail()             { return email; }
    public String getPhone()             { return phone; }
    public String getAddress()           { return address; }
    public CustomerStatus getStatus()    { return status; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }
}