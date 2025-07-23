package tech.challenge.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.challenge.consumer.service.BankAccountService;
import tech.challenge.exception.GlobalExceptionHandler;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private BalanceController balanceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(balanceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void test_getBalance_success() throws Exception {
        // Arrange
        double mockBalance = 1000.0;
        when(bankAccountService.retrieveBalance()).thenReturn(mockBalance);

        // Act & Assert
        mockMvc.perform(get("/api/v1/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.availableBalance").value(mockBalance));

        verify(bankAccountService, times(1)).retrieveBalance();
    }

    @Test
    void test_getBalance_runtimeException() throws Exception {
        // Arrange
        when(bankAccountService.retrieveBalance()).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Service error"));

        verify(bankAccountService, times(1)).retrieveBalance();
    }
}