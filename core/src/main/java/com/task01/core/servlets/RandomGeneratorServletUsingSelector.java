package com.task01.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component(service = Servlet.class)
@SlingServletPaths(value = "/bin/custom/randomgenerator/selector")
public class RandomGeneratorServletUsingSelector extends SlingSafeMethodsServlet {

    private static final String SELECTOR_NUMBER = "number";
    private static final String SELECTOR_LETTER = "letter";
    private static final String SELECTOR_RANDOM = "random";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final Random RANDOM = new Random();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        // Set response content type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Get the selector from the request
        String selector = request.getRequestPathInfo().getSelectorString();
        Map<String, String> responseMap = new HashMap<>();

        // Handle based on the selector
        if (SELECTOR_NUMBER.equals(selector)) {
            // Generate a random 6-digit number
            responseMap.put("result", generateRandomNumber());
        } else if (SELECTOR_LETTER.equals(selector)) {
            // Generate a random 6-letter string
            responseMap.put("result", generateRandomLetters());
        } else if (SELECTOR_RANDOM.equals(selector)) {
            // Generate a random 6-character string (first 3 numbers + last 3 letters)
            responseMap.put("result", generateRandomMixed());
        } else {
            responseMap.put("error", "Invalid or missing selector. Use one of: number, letter, random");
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        }

        // Convert the response map to JSON and write to the response
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(response.getWriter(), responseMap);
    }

    private String generateRandomNumber() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length())));
        }
        return sb.toString();
    }

    private String generateRandomLetters() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }
        return sb.toString();
    }

    private String generateRandomMixed() {
        // Generate 3 random numbers (first 3 characters)
        StringBuilder numbers = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            numbers.append(NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length())));
        }

        // Generate 3 random letters (last 3 characters)
        StringBuilder letters = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            letters.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }

        // Combine in the required order: first 3 numbers, then 3 letters
        return numbers.toString() + letters.toString();
    }
}