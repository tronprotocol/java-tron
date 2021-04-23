package org.tron.common.utils;

import org.tron.protos.contract.CrossChain;

public class AuctionConfigParser {
  private static Integer ROUND_LENGH = 2;//round

  private static Integer SLOT_COUNT_LENGH = 2;//slot count for every round

  private static Integer END_TIME_LENGH = 10;//auction end time

  private static Integer DURATION_LENGH = 3;//slot duration


  public static CrossChain.AuctionRoundContract parseAuctionConfig(Long auctionConfig) {
    try {
      String auctionConfigStr = String.valueOf(auctionConfig);
      int configLength = auctionConfigStr.length();
      if (!(configLength == 14 ||configLength == 15 || configLength == 16 || configLength == 17)) {
        throw new RuntimeException("invalid auction config");
      }
      //  eg:01 08 1617162101 023 or 11 08 1617162101 023
      Integer duration = Integer.valueOf(auctionConfigStr.substring(configLength - 3, configLength));
      Long endTime = Long.valueOf(auctionConfigStr.substring(configLength - 13, configLength - 3));
      Integer slotCount = 0;
      Integer round = 0;
      if (configLength <= 15) {
        slotCount = Integer.valueOf(auctionConfigStr.substring(0, configLength - 13));
        round = 0;
      } else {
        slotCount = Integer.valueOf(auctionConfigStr.substring(configLength - 15, configLength - 13));
        round = Integer.valueOf(auctionConfigStr.substring(0, configLength - 15));
      }

      CrossChain.AuctionRoundContract.Builder builder = CrossChain.AuctionRoundContract.newBuilder()
          .setDuration(duration)
          .setRound(round)
          .setEndTime(endTime)
          .setSlotCount(slotCount);
      return builder.build();
    } catch (Exception e) {
      e.printStackTrace();
      return CrossChain.AuctionRoundContract.newBuilder().build();
    }
  }

  public static Long getAuctionEndTime(long value) {
    return parseAuctionConfig(value).getEndTime();
  }

  public static Integer getAuctionRound(long value) {
    return parseAuctionConfig(value).getRound();
  }

  public static Long getAuctionDuration(long value) {
    return parseAuctionConfig(value).getDuration();
  }

  public static Integer getSlotCount(long value) {
    return parseAuctionConfig(value).getSlotCount();
  }
}
