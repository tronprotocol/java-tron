package org.tron.stresstest.dispatch;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Getter
@Setter
@ToString
@EqualsAndHashCode(exclude = {"amount", "nice"})
public class Stats {
  private ContractType type;
  private String assetName;
  private ByteString address;
  private Long amount;
  private boolean nice;

  public static List<String> result(List<Stats> stats) {
    List<String> result = new ArrayList<>();
    result.add("*******************create trx result stats**************************");
    // trx type
    result.add("trx types:" + stats.stream()
        .map(Stats::getType)
        .distinct()
        .collect(Collectors.toList())
    );

    // trx succeed
    Map<ContractType, Long> typeSucceedMap = stats.stream()
        .filter(Stats::isNice)
        .collect(Collectors.groupingBy(Stats::getType, Collectors.counting()));
    result.add("trx succeed:" + typeSucceedMap);

    // trx succeed
    Map<ContractType, Long> typeFailMap = stats.stream()
        .filter(Stats::isNice)
        .collect(Collectors.groupingBy(Stats::getType, Collectors.counting()));
    result.add("trx fail:" + typeFailMap);

    // trx address succeed
    Map<Stats, List<Long>> addressSucceed = stats.stream()
        .filter(Stats::isNice)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.mapping(Stats::getAmount, Collectors.toList())));

    Map<Map.Entry<ContractType, ByteString>, Long> addressAmountMap = addressSucceed.entrySet().stream()
        .map(e -> Maps.immutableEntry(Maps.immutableEntry(e.getKey().getType(), e.getKey().getAddress()),
            e.getValue().stream()
                .reduce(0L, operate(e.getKey()))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    result.add("trx address succeed:" + addressAmountMap);
    result.add("********************************************************************");
    return result;
  }

  private static BinaryOperator<Long> operate(Stats stats) {
    switch (stats.getType()) {
      case TransferContract:
      case TransferAssetContract: {
        return (l1, l2) -> l1 + l2;
      }
      case VoteAssetContract:
      case VoteWitnessContract: {
        return (l1, l2) -> l2;
      }

      default:
        return (l1, l2) -> 0L;
    }
  }
}
