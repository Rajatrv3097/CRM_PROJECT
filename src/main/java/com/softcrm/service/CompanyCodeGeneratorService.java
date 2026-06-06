package com.softcrm.service;

import com.softcrm.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyCodeGeneratorService {

    @Autowired
    private CompanyRepository companyRepository;

    /**
     * Generate company code from company name
     * Examples:
     * "Acme Corporation" → "ACME"
     * "Tech Solutions" → "TECH"
     * "ABC Company" → "ABC"
     */
    public String generateCode(String companyName) {
        // Convert to uppercase and remove special characters
        String cleanName = companyName
                .toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", "")
                .trim();

        String[] words = cleanName.split("\\s+");
        String code;

        // Take first 4 letters of first word
        if (words[0].length() >= 4) {
            code = words[0].substring(0, 4);
        }
        // If first word is shorter, take full first word + first letters of next words
        else {
            StringBuilder sb = new StringBuilder(words[0]);
            for (int i = 1; i < words.length && sb.length() < 4; i++) {
                if (words[i].length() > 0) {
                    sb.append(words[i].charAt(0));
                }
            }
            code = sb.toString();

            // Pad with 'X' if still less than 4 characters
            while (code.length() < 4) {
                code += "X";
            }
        }

        // Ensure uniqueness
        return makeUnique(code);
    }

    private String makeUnique(String baseCode) {
        String code = baseCode;
        int counter = 1;

        while (companyRepository.existsByCode(code)) {
            if (baseCode.length() >= 4) {
                code = baseCode.substring(0, 3) + counter;
            } else {
                code = baseCode + counter;
            }
            counter++;
        }

        return code;
    }
}