package tech.challenge.audit.submission;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Submission {
    private final List<Batch> batches;

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