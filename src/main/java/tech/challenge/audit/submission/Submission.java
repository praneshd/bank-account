package tech.challenge.audit.submission;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents a submission containing multiple batches of transactions.
 * Each batch includes details about the total value of transactions and the count of transactions.
 */
@Getter
@Builder
public class Submission {
    /**
     * The list of batches included in this submission.
     */
    private final List<Batch> batches;

    /**
     * Converts the submission object to a JSON-like string representation.
     *
     * @return a string representation of the submission in JSON format
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\"submission\": {\"batches\": [\n");
        for (int i = 0; i < batches.size(); i++) {
            Batch b = batches.get(i);
            sb.append("  {\"totalValueOfAllTransactions\": ")
                    .append(b.getTotalValue())
                    .append(", \"countOfTransactions\": ")
                    .append(b.getTransactionCount())
                    .append("}");
            if (i < batches.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]}}\n");
        return sb.toString();
    }
}