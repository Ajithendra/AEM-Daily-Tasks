package com.task01.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Random;

@Component(service = Servlet.class)
@SlingServletPaths(value = "/bin/custom/randomgenerator")
public class RandomGeneratorServlet extends SlingSafeMethodsServlet {

    private static final String PARAM_NUMBER = "number";
    private static final String PARAM_LETTER = "letter";
    private static final String PARAM_RANDOM = "random";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final Random RANDOM = new Random();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        // Set response content type to plain text
        response.setContentType("text/plain");

        // Get the parameter from the request
        String numberParam = request.getParameter(PARAM_NUMBER);
        String letterParam = request.getParameter(PARAM_LETTER);
        String randomParam = request.getParameter(PARAM_RANDOM);

        String result;

        // Handle based on which parameter is present
        if (numberParam != null) {
            // Generate a random 6-digit number
            result = generateRandomNumber();
        } else if (letterParam != null) {
            // Generate a random 6-letter string
            result = generateRandomLetters();
        } else if (randomParam != null) {
            // Generate a random 6-character string (first 3 numbers + last 3 letters)
            result = generateRandomMixed();
        } else {
            result = "Error: Please provide one of the parameters - number, letter, or random";
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        }

        // Write the result to the response
        response.getWriter().write(result);
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