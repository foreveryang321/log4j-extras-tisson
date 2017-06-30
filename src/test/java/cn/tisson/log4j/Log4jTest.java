package cn.tisson.log4j;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log4jTest {
    // static final int TOP_OF_TROUBLE = -1;
    // static final int TOP_OF_MINUTE = 0;
    // static final int TOP_OF_HOUR = 1;
    // static final int HALF_DAY = 2;
    // static final int TOP_OF_DAY = 3;
    // static final int TOP_OF_WEEK = 4;
    // static final int TOP_OF_MONTH = 5;
    public static void main(String[] args) {
        DailyRollingFileAppender dailyRollingFileAppender = new DailyRollingFileAppender();
        dailyRollingFileAppender.setDatePattern("'.'yyyy-MM-dd");
        int type = dailyRollingFileAppender.getType();
        System.out.println("type: " + type);
        RollingCalendar rc = dailyRollingFileAppender.getRc();
        long nextCheckMillis = rc.getNextCheckMillis(new Date(), type);
        System.out.println("nextCheckMillis: " + nextCheckMillis);
        nextCheckMillis = 1498838400000L;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format2 = fmt.format(new Date(nextCheckMillis));
        System.out.println("format2: " + format2);
        System.out.println(dailyRollingFileAppender.getDeleteDateStr(new Date(), type));
    }
}
