package com.softcrm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softcrm.dto.request.CreateCustomerRequest;
import com.softcrm.dto.response.CustomerResponse;
import com.softcrm.entity.Customer;
import com.softcrm.entity.user.User;
import com.softcrm.repository.CustomerRepository;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ============ Helper Methods ============

    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return "System";
    }

    private User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email).orElse(null);
    }

    private Long getCurrentUserCompanyId() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return null;
        if ("SUPER_ADMIN".equals(currentUser.getUserType())) return null;
        return currentUser.getCompanyId();
    }

    private boolean isSuperAdmin() {
        User currentUser = getCurrentUser();
        return currentUser != null && "SUPER_ADMIN".equals(currentUser.getUserType());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private <T> List<T> fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return Collections.singletonList(objectMapper.readValue(json, typeRef));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    // ============ Get Customer Entity ============

    private Customer getCustomerEntity(Long id) {
        Long companyId = getCurrentUserCompanyId();
        if (companyId == null && isSuperAdmin()) {
            return customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        }
        return customerRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    // ============ Customer CRUD ============

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer: {}", request.getName());

        Long companyId = getCurrentUserCompanyId();
        if (companyId == null && !isSuperAdmin()) {
            throw new RuntimeException("No company assigned");
        }

        // Check duplicates
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail(), companyId)) {
            throw new RuntimeException("Customer with this email already exists");
        }
        if (request.getPhone() != null && customerRepository.existsByPhone(request.getPhone(), companyId)) {
            throw new RuntimeException("Customer with this phone already exists");
        }

        Customer customer = new Customer();
        customer.setCompanyId(companyId);
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAlternativePhone(request.getAlternativePhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setCountry(request.getCountry());
        customer.setPincode(request.getPincode());
        customer.setCustomerCompanyName(request.getCustomerCompanyName());
        customer.setGstNumber(request.getGstNumber());
        customer.setPanNumber(request.getPanNumber());
        customer.setIndustry(request.getIndustry());
        customer.setWebsite(request.getWebsite());
        customer.setLinkedinUrl(request.getLinkedinUrl());
        customer.setFacebookUrl(request.getFacebookUrl());
        customer.setTwitterUrl(request.getTwitterUrl());
        customer.setInstagramUrl(request.getInstagramUrl());
        customer.setCustomerType(request.getCustomerType() != null ? request.getCustomerType() : "REGULAR");
        customer.setCustomerTier(request.getCustomerTier() != null ? request.getCustomerTier() : "BRONZE");
        customer.setCustomerStatus(request.getCustomerStatus() != null ? request.getCustomerStatus() : "ACTIVE");
        customer.setSource(request.getSource() != null ? request.getSource() : "DIRECT");
        customer.setSourceDetails(request.getSourceDetails());

        // Initialize JSON arrays
        customer.setTags(toJson(request.getTags() != null ? request.getTags() : new ArrayList<>()));
        customer.setNotes(toJson(new ArrayList<>()));
        customer.setContacts(toJson(new ArrayList<>()));
        customer.setDocuments(toJson(new ArrayList<>()));
        customer.setInteractions(toJson(new ArrayList<>()));

        // Set assigned sales person
        if (request.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            customer.setAssignedTo(assignedUser);
        }

        User currentUser = getCurrentUser();
//        if (currentUser != null) {
//            customer.setCreatedBy(currentUser);
//        }


        if (currentUser != null) {
            customer.setCreatedBy(currentUser.getUsername());  // ✅ Set username as String
        }

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: {}", saved.getName());

        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        Long companyId = getCurrentUserCompanyId();
        List<Customer> customers;

        if (companyId == null && isSuperAdmin()) {
            customers = customerRepository.findAll();
        } else {
            customers = customerRepository.findAllByCompanyId(companyId);
        }

        return customers.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = getCustomerEntity(id);
        return convertToResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CreateCustomerRequest request) {
        log.info("Updating customer: {}", id);
        Customer customer = getCustomerEntity(id);

        // Only update fields that are provided (not null)
        if (request.getName() != null) customer.setName(request.getName());
        if (request.getEmail() != null) customer.setEmail(request.getEmail());
        if (request.getPhone() != null) customer.setPhone(request.getPhone());
        if (request.getAlternativePhone() != null) customer.setAlternativePhone(request.getAlternativePhone());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getCity() != null) customer.setCity(request.getCity());
        if (request.getState() != null) customer.setState(request.getState());
        if (request.getCountry() != null) customer.setCountry(request.getCountry());
        if (request.getPincode() != null) customer.setPincode(request.getPincode());
        if (request.getCustomerCompanyName() != null) customer.setCustomerCompanyName(request.getCustomerCompanyName());
        if (request.getGstNumber() != null) customer.setGstNumber(request.getGstNumber());
        if (request.getPanNumber() != null) customer.setPanNumber(request.getPanNumber());
        if (request.getIndustry() != null) customer.setIndustry(request.getIndustry());
        if (request.getWebsite() != null) customer.setWebsite(request.getWebsite());
        if (request.getLinkedinUrl() != null) customer.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getFacebookUrl() != null) customer.setFacebookUrl(request.getFacebookUrl());
        if (request.getTwitterUrl() != null) customer.setTwitterUrl(request.getTwitterUrl());
        if (request.getInstagramUrl() != null) customer.setInstagramUrl(request.getInstagramUrl());
        if (request.getCustomerType() != null) customer.setCustomerType(request.getCustomerType());
        if (request.getCustomerTier() != null) customer.setCustomerTier(request.getCustomerTier());
        if (request.getCustomerStatus() != null) customer.setCustomerStatus(request.getCustomerStatus());
        if (request.getTags() != null) customer.setTags(toJson(request.getTags()));
        if (request.getSource() != null) customer.setSource(request.getSource());
        if (request.getSourceDetails() != null) customer.setSourceDetails(request.getSourceDetails());

        if (request.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            customer.setAssignedTo(assignedUser);
        }

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated: {}", updated.getName());
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerEntity(id);
        customer.setIsDeleted(true);
        customer.setCustomerStatus("INACTIVE");
        customerRepository.save(customer);
        log.info("Customer deleted: {}", customer.getName());
    }

    // ============ Customer Notes ============

    @Transactional
    public CustomerResponse addNote(Long id, String note) {
        Customer customer = getCustomerEntity(id);

        List<Map<String, Object>> notes = fromJson(customer.getNotes(), new TypeReference<>() {});

        Map<String, Object> noteObj = new LinkedHashMap<>();
        noteObj.put("id", UUID.randomUUID().toString());
        noteObj.put("note", note);
        noteObj.put("createdBy", getCurrentUser() != null ? getCurrentUser().getUsername() : "System");
        noteObj.put("createdAt", LocalDateTime.now().toString());
        notes.add(noteObj);

        customer.setNotes(toJson(notes));
        return convertToResponse(customerRepository.save(customer));
    }

    // ============ Customer Contacts ============

    @Transactional
    public CustomerResponse addContact(Long id, String contactName, String phone, String email, String designation) {
        Customer customer = getCustomerEntity(id);

        List<Map<String, Object>> contacts = fromJson(customer.getContacts(), new TypeReference<>() {});

        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("id", UUID.randomUUID().toString());
        contact.put("name", contactName);
        contact.put("phone", phone);
        contact.put("email", email);
        contact.put("designation", designation);
        contact.put("createdAt", LocalDateTime.now().toString());
        contacts.add(contact);

        customer.setContacts(toJson(contacts));
        return convertToResponse(customerRepository.save(customer));
    }

    // ============ Purchase Tracking ============

    @Transactional
    public CustomerResponse recordPurchase(Long id, BigDecimal amount) {
        Customer customer = getCustomerEntity(id);
        customer.addPurchase(amount);

        // Add to interactions
        List<Map<String, Object>> interactions = fromJson(customer.getInteractions(), new TypeReference<>() {});
        Map<String, Object> interaction = new LinkedHashMap<>();
        interaction.put("type", "PURCHASE");
        interaction.put("amount", amount);
        interaction.put("date", LocalDateTime.now().toString());
        interactions.add(interaction);
        customer.setInteractions(toJson(interactions));

        return convertToResponse(customerRepository.save(customer));
    }

    // ============ KYC Verification ============

    @Transactional
    public CustomerResponse verifyKYC(Long id, String documentUrl) {
        Customer customer = getCustomerEntity(id);
        customer.setKycStatus("VERIFIED");
        customer.setKycDocumentUrl(documentUrl);
        customer.setKycVerifiedAt(LocalDateTime.now());
        customer.setKycVerifiedBy(getCurrentUser() != null ? getCurrentUser().getId() : null);

        return convertToResponse(customerRepository.save(customer));
    }

    // ============ Search ============

    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String keyword) {
        Long companyId = getCurrentUserCompanyId();
        List<Customer> customers;

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }

        if (companyId == null && isSuperAdmin()) {
            customers = customerRepository.findAll().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .filter(c -> c.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                            (c.getEmail() != null && c.getEmail().toLowerCase().contains(keyword.toLowerCase())) ||
                            (c.getPhone() != null && c.getPhone().contains(keyword)))
                    .toList();
        } else {
            customers = customerRepository.findAllByCompanyId(companyId).stream()
                    .filter(c -> c.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                            (c.getEmail() != null && c.getEmail().toLowerCase().contains(keyword.toLowerCase())) ||
                            (c.getPhone() != null && c.getPhone().contains(keyword)))
                    .toList();
        }

        return customers.stream().map(this::convertToResponse).toList();
    }

    // ============ Filters ============

    public Long getCurrentUserCompanyIdForFilter() {
        return getCurrentUserCompanyId();
    }

    public List<CustomerResponse> getCustomersByTier(Long companyId, String tier) {
        List<Customer> customers;

        if (companyId == null && isSuperAdmin()) {
            customers = customerRepository.findAll().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .filter(c -> tier.equals(c.getCustomerTier()))
                    .toList();
        } else {
            customers = customerRepository.findByTier(companyId, tier);
        }

        return customers.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(this::convertToResponse)
                .toList();
    }

    // ============ Convert to Response ============

    private CustomerResponse convertToResponse(Customer customer) {
        List<String> tags = fromJson(customer.getTags(), new TypeReference<>() {});
        List<Map<String, Object>> notes = fromJson(customer.getNotes(), new TypeReference<>() {});
        List<Map<String, Object>> contacts = fromJson(customer.getContacts(), new TypeReference<>() {});
        List<Map<String, Object>> documents = fromJson(customer.getDocuments(), new TypeReference<>() {});
        List<Map<String, Object>> interactions = fromJson(customer.getInteractions(), new TypeReference<>() {});

        return CustomerResponse.builder()
                .id(customer.getId())
                .companyId(customer.getCompanyId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .alternativePhone(customer.getAlternativePhone())
                .fullAddress(customer.getFullAddress())
                .customerCompanyName(customer.getCustomerCompanyName())
                .gstNumber(customer.getGstNumber())
                .panNumber(customer.getPanNumber())
                .industry(customer.getIndustry())
                .website(customer.getWebsite())
                .linkedinUrl(customer.getLinkedinUrl())
                .facebookUrl(customer.getFacebookUrl())
                .twitterUrl(customer.getTwitterUrl())
                .instagramUrl(customer.getInstagramUrl())
                .customerType(customer.getCustomerType())
                .customerTier(customer.getCustomerTier())
                .customerStatus(customer.getCustomerStatus())
                .tags(tags)
                .totalPurchases(customer.getTotalPurchases())
                .totalOrders(customer.getTotalOrders())
                .averageOrderValue(customer.getAverageOrderValue())
                .lastPurchaseDate(customer.getLastPurchaseDate())
                .lifetimeValue(customer.getLifetimeValue())
                .source(customer.getSource())
                .sourceDetails(customer.getSourceDetails())
                .notes(notes)
                .contacts(contacts)
                .documents(documents)
                .interactions(interactions)
                .kycStatus(customer.getKycStatus())
                .kycDocumentUrl(customer.getKycDocumentUrl())
                .assignedToId(customer.getAssignedTo() != null ? customer.getAssignedTo().getId() : null)
                .assignedToName(customer.getAssignedTo() != null ? customer.getAssignedTo().getUsername() : null)
                .createdAt(customer.getCreatedAt())
                .build();
    }
}