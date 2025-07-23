package tech.challenge.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.challenge.consumer.service.BankAccountService;


@Slf4j
@RestController
@RequestMapping("/api/v1")

public class BalanceController {

    private final BankAccountService bankAccountService;

    public BalanceController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance() {
        try {
            double balance = bankAccountService.retrieveBalance();
            BalanceResponse response = BalanceResponse.builder()
                    .availableBalance(balance)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error retrieving balance", ex);
            throw ex; // Let it propagate to GlobalExceptionHandler
        }
    }

}