package com.softcrm.service;

import com.softcrm.dto.request.CompanyRequest;
import com.softcrm.dto.response.CompanyResponse;
import com.softcrm.entity.Company;
import com.softcrm.repository.CompanyRepository;
import com.softcrm.repository.RoleRepository;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ============ Company CRUD Operations ============

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        log.info("Creating new company: {}", request.getName());

        // Check if company name already exists
        if (companyRepository.existsByName(request.getName())) {
            throw new RuntimeException("Company name already exists: " + request.getName());
        }

        // ✅ AUTO-GENERATE COMPANY CODE FROM NAME (removed request.getCode() validation)
        String generatedCode = generateCompanyCode(request.getName());
        String uniqueCode = ensureUniqueCode(generatedCode);
        log.info("Auto-generated company code: {} -> {}", generatedCode, uniqueCode);

        Company company = Company.builder()
                .name(request.getName())
                .code(uniqueCode)  // ✅ Using auto-generated code, NOT from request
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .gstNumber(request.getGstNumber())
                .panNumber(request.getPanNumber())
                .subscriptionPlan(request.getSubscriptionPlan() != null ? request.getSubscriptionPlan() : "BASIC")
                .maxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 10)
                .maxStorageGb(request.getMaxStorageGb() != null ? request.getMaxStorageGb() : 5)
                .isActive(true)
                .subscriptionExpiry(LocalDateTime.now().plusYears(1))
                // Set feature flags
                .enableLeadManagement(request.getEnableLeadManagement() != null ? request.getEnableLeadManagement() : true)
                .enableCustomerManagement(request.getEnableCustomerManagement() != null ? request.getEnableCustomerManagement() : true)
                .enableSalesPipeline(request.getEnableSalesPipeline() != null ? request.getEnableSalesPipeline() : true)
                .enableMarketingAutomation(request.getEnableMarketingAutomation() != null ? request.getEnableMarketingAutomation() : false)
                .enableSupportTickets(request.getEnableSupportTickets() != null ? request.getEnableSupportTickets() : true)
                .enableHRModule(request.getEnableHRModule() != null ? request.getEnableHRModule() : false)
                .enableInventory(request.getEnableInventory() != null ? request.getEnableInventory() : false)
                .enableReports(request.getEnableReports() != null ? request.getEnableReports() : true)
                .enableEmailIntegration(request.getEnableEmailIntegration() != null ? request.getEnableEmailIntegration() : true)
                .enableWhatsAppIntegration(request.getEnableWhatsAppIntegration() != null ? request.getEnableWhatsAppIntegration() : false)
                .enableAIFeatures(request.getEnableAIFeatures() != null ? request.getEnableAIFeatures() : false)
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("Company created successfully with ID: {} and Code: {}", savedCompany.getId(), savedCompany.getCode());

        return convertToResponse(savedCompany);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getActiveCompanies() {
        return companyRepository.findByIsActive(true).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        return convertToResponse(company);
    }

    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));

        company.setName(request.getName());
        company.setAddress(request.getAddress());
        company.setCity(request.getCity());
        company.setState(request.getState());
        company.setCountry(request.getCountry());
        company.setPincode(request.getPincode());
        company.setPhone(request.getPhone());
        company.setEmail(request.getEmail());
        company.setWebsite(request.getWebsite());
        company.setGstNumber(request.getGstNumber());
        company.setPanNumber(request.getPanNumber());
        company.setSubscriptionPlan(request.getSubscriptionPlan());
        company.setMaxUsers(request.getMaxUsers());
        company.setMaxStorageGb(request.getMaxStorageGb());

        Company updatedCompany = companyRepository.save(company);
        log.info("Company updated successfully: {}", updatedCompany.getName());

        return convertToResponse(updatedCompany);
    }

    @Transactional
    public void activateCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        company.setIsActive(true);
        companyRepository.save(company);
        log.info("Company activated: {}", company.getName());
    }

    @Transactional
    public void deactivateCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        company.setIsActive(false);
        companyRepository.save(company);
        log.info("Company deactivated: {}", company.getName());
    }

    @Transactional
    public void deleteCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Check if there are users in this company
        long userCount = companyRepository.countUsersByCompanyId(id);
        if (userCount > 0) {
            throw new RuntimeException("Cannot delete company with " + userCount + " users. Deactivate instead.");
        }

        companyRepository.delete(company);
        log.info("Company deleted: {}", company.getName());
    }

    // ============ Helper Methods ============

    /**
     * Generate company code from company name
     * Examples:
     * "Acme Corporation" -> "ACME"
     * "Tech Solutions" -> "TECH"
     * "IBM" -> "IBMX"
     * "ABC Corp" -> "ABCX"
     * "D.K. Enterprises" -> "DKEX"
     */
    private String generateCompanyCode(String companyName) {
        // Convert to uppercase and remove special characters
        String cleanName = companyName.toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", "")
                .trim();

        if (cleanName.isEmpty()) {
            return "COMP";
        }

        // Split into words
        String[] words = cleanName.split("\\s+");
        String firstWord = words[0];

        // If first word has 4 or more letters, take first 4 letters
        if (firstWord.length() >= 4) {
            return firstWord.substring(0, 4);
        }

        // If first word is shorter, take full first word + first letters of next words
        StringBuilder code = new StringBuilder(firstWord);
        for (int i = 1; i < words.length && code.length() < 4; i++) {
            if (words[i].length() > 0) {
                code.append(words[i].charAt(0));
            }
        }

        // Pad with 'X' if still less than 4 characters
        while (code.length() < 4) {
            code.append('X');
        }

        return code.toString();
    }

    /**
     * Ensure the generated code is unique
     * If code exists, appends numbers (ACME, ACM1, ACM2, etc.)
     */
    private String ensureUniqueCode(String baseCode) {
        String code = baseCode;
        int counter = 1;

        while (companyRepository.existsByCode(code)) {
            if (baseCode.length() >= 3) {
                // Take first 3 letters + counter number
                code = baseCode.substring(0, 3) + counter;
            } else {
                // Add counter at the end
                code = baseCode + counter;
            }
            counter++;
        }

        return code;
    }

    private CompanyResponse convertToResponse(Company company) {
        long userCount = companyRepository.countUsersByCompanyId(company.getId());

        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .address(company.getAddress())
                .city(company.getCity())
                .state(company.getState())
                .country(company.getCountry())
                .pincode(company.getPincode())
                .phone(company.getPhone())
                .email(company.getEmail())
                .website(company.getWebsite())
                .gstNumber(company.getGstNumber())
                .panNumber(company.getPanNumber())
                .logoUrl(company.getLogoUrl())
                .isActive(company.getIsActive())
                .subscriptionPlan(company.getSubscriptionPlan())
                .subscriptionExpiry(company.getSubscriptionExpiry())
                .maxUsers(company.getMaxUsers())
                .maxStorageGb(company.getMaxStorageGb())
                .usedStorageGb(company.getUsedStorageGb())
                .totalUsers(userCount)
                .createdAt(company.getCreatedAt())
                .build();
    }
}