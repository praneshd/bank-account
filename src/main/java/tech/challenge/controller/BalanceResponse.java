package tech.challenge.controller;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BalanceResponse {

    private final String availableBalance;

    public static class BalanceResponseBuilder {
        public BalanceResponseBuilder availableBalance(double availableBalance) {
            this.availableBalance = String.format("%.2f", availableBalance);
            return this;
        }
    }
}