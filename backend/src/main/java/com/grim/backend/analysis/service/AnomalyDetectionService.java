package com.grim.backend.analysis.service;

import com.grim.backend.analysis.dto.AnomalyResponse;
import com.grim.backend.transaction.entity.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class AnomalyDetectionService {

    public List<AnomalyResponse> detect(List<Transaction> txs) {

        Map<String, BigDecimal> averages =
                txs.stream()
                        .collect(Collectors.groupingBy(
                                tx -> tx.getCategory().getName(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            BigDecimal sum = list.stream()
                                                    .map(Transaction::getAmount)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                                            return sum.divide(
                                                    BigDecimal.valueOf(list.size()),
                                                    2,
                                                    RoundingMode.HALF_UP
                                            );
                                        }
                                )
                        ));

        List<AnomalyResponse> anomalies = new ArrayList<>();

        for (Transaction tx : txs) {

            String categoryName = tx.getCategory().getName();
            BigDecimal avg = averages.get(categoryName);

            if (avg == null) continue;
            if (avg.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal threshold = avg.multiply(BigDecimal.valueOf(3));

            if (tx.getAmount().compareTo(threshold) > 0) {

                BigDecimal ratio = tx.getAmount().divide(avg, 2, RoundingMode.HALF_UP);
                anomalies.add(
                        AnomalyResponse.builder()
                                .transactionId(tx.getId())
                                .date(tx.getDate())
                                .amount(tx.getAmount())
                                .category(categoryName)
                                .average(avg)
                                .deviation(
                                        ratio + "x above average"
                                )
                                .reason("This is " + ratio + "x your average " + categoryName + " spend.")
                                .build()
                );
            }
        }

        return anomalies;
    }
}