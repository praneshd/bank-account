package tech.challenge.audit.submission;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Batch {
    private int transactionCount;

    private double totalValue;

    @Builder
    public Batch(int transactionCount, double totalValue) {
        this.transactionCount = transactionCount;
        this.totalValue = totalValue;
    }

    public void addTransaction(double value) {
        this.totalValue += value;
        this.transactionCount++;
    }
}