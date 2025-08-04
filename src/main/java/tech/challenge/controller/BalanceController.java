package tech.challenge.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.challenge.consumer.service.BankAccountService;

/**
 * REST controller for handling balance-related API endpoints.
 * Provides an endpoint to retrieve the current account balance.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class BalanceController {

    private final BankAccountService bankAccountService;

    /**
     * Constructor for BalanceController.
     *
     * @param bankAccountService the service used to retrieve account balance
     */
    public BalanceController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    /**
     * Endpoint to retrieve the current account balance.
     *
     * @return a ResponseEntity containing the balance response
     */
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance() {
        try {
            // Retrieve the current balance from the service
            double balance = bankAccountService.retrieveBalance();

            // Build the response object with the retrieved balance
            BalanceResponse response = BalanceResponse.builder()
                    .availableBalance(balance)
                    .build();

            // Return the response with HTTP 200 status
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            // Log the error and propagate the exception
            log.error("Error retrieving balance", ex);
            throw ex; // Let it propagate to GlobalExceptionHandler
        }
    }
}