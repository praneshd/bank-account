package tech.challenge.domain;


import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class Transaction {
    private final String id;
    private final double amount;

    public static Transaction credit(double amount) {
        return Transaction.builder()
                .id(UUID.randomUUID().toString())
                .amount(Math.abs(amount))
                .build();
    }

    public static Transaction debit(double amount) {
        return Transaction.builder()
                .id(UUID.randomUUID().toString())
                .amount(-Math.abs(amount))
                .build();
    }
}