package stest.tron.wallet.onlinestress.sun20;

public class zz {

  public static void main(String[] args) {
//    try {
//      System.out.println(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS").parse("2021/11/11 11:11:11:111").getTime());
//    } catch (ParseException e) {
//      e.printStackTrace();
//    }
//    Calendar ca = Calendar.getInstance();
//    ca.add(Calendar.HOUR,1);
//    ca.set(Calendar.MINUTE,0);
//    ca.set(Calendar.SECOND,0);
//    ca.set(Calendar.MILLISECOND,0);
//    System.out.println(ca.getTime());

//    Long l= System.currentTimeMillis() - System.currentTimeMillis()%1800000 + 1800000;
//    System.out.println("nextHalfAHourTimestamp is:" + l);
//    System.out.println("date is:" + new Date(l));

    boolean s = 1626064488 >= 1625835600;
    if (1626064488 >= 1625835600) {

    System.out.println("currentTimestamp >= lockEndTimestamp");
    } else {
      System.out.println("currentTimestamp < lockEndTimestamp");

    }

  }
}
