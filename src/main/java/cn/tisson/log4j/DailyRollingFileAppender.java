package cn.tisson.log4j;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

/**
 * log4j日志框架DailyRollingFileAppender添加maxBackupIndex参数，控制保留日志的数量
 * <p>
 * 添加maxBackupIndex参数指定保留多少个单位时间的文件。maxBackupIndex默认是30。比如yyyy-MM-dd步进天， 则默认保留30天的记录；yyyy-MM-dd
 * HH步进是小时，则默认保留30小时的记录。年、月、日、时、分都同理。不推荐按时、分滚动日志文件
 * 
 * @author yl
 */
public class DailyRollingFileAppender extends FileAppender {

    // The code assumes that the following constants are in a increasing
    // sequence.
    static final int TOP_OF_TROUBLE = -1;
    static final int TOP_OF_MINUTE = 0;
    static final int TOP_OF_HOUR = 1;
    static final int HALF_DAY = 2;
    static final int TOP_OF_DAY = 3;
    static final int TOP_OF_WEEK = 4;
    static final int TOP_OF_MONTH = 5;

    /**
     * The date pattern. By default, the pattern is set to "'.'yyyy-MM-dd" meaning daily rollover.
     */
    private String datePattern = "'.'yyyy-MM-dd";
    /**
     * 配置，保留多少个日志文件
     */
    private int maxBackupIndex = 30;
    /**
     * The log file will be renamed to the value of the scheduledFilename variable when the next
     * interval is entered. For example, if the rollover period is one hour, the log file will be
     * renamed to the value of "scheduledFilename" at the beginning of the next hour.
     * 
     * The precise time when a rollover occurs depends on logging activity.
     */
    private String nextFilename;

    /**
     * The next time we estimate a rollover should occur.
     */
    private long nextCheckTime = System.currentTimeMillis() - 1;

    Date now = new Date();
    SimpleDateFormat sdf;

    // 这里不能使用
    RollingCalendar rc = new RollingCalendar();
    int type = TOP_OF_TROUBLE;
    static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public RollingCalendar getRc() {
        return this.rc;
    }

    /**
     * The default constructor does nothing.
     */
    public DailyRollingFileAppender() {}

    /**
     * Instantiate a <code>DailyRollingFileAppender</code> and open the file designated by
     * <code>filename</code>. The opened filename will become the ouput destination for this
     * appender.
     * 
     */
    public DailyRollingFileAppender(Layout layout, String filename, String datePattern)
            throws IOException {
        super(layout, filename, true);
        this.datePattern = datePattern;
        activateOptions();
    }

    public void setDatePattern(String pattern) {
        datePattern = pattern;
    }

    /** Returns the value of the <b>DatePattern</b> option. */
    public String getDatePattern() {
        return datePattern;
    }

    public int getMaxBackupIndex() {
        return maxBackupIndex;
    }

    public void setMaxBackupIndex(int maxBackupIndex) {
        this.maxBackupIndex = maxBackupIndex;
    }

    public void activateOptions() {
        super.activateOptions();
        if (this.getDatePattern() != null && fileName != null) {
            this.maxBackupIndex = maxBackupIndex + 1;
            now.setTime(System.currentTimeMillis());
            sdf = new SimpleDateFormat(this.getDatePattern());
            type = this.getType();
            File file = new File(fileName);
            nextFilename = fileName + sdf.format(new Date(file.lastModified()));
        } else {
            LogLog.error(format.format(now)
                    + " -> Either File or DatePattern options are not set for appender [" + name
                    + "].");
        }
    }

    /**
     * Rollover the current file to a new file.
     */
    public void rollOver() throws IOException {
        /* Compute filename, but only if datePattern is specified */
        if (this.getDatePattern() == null) {
            errorHandler.error("Missing DatePattern option in rollOver().");
            return;
        }

        String datedFilename = fileName + sdf.format(now);
        // It is too early to roll over because we are still within the
        // bounds of the current interval. Rollover will occur once the
        // next interval is reached.
        if (nextFilename.equals(datedFilename)) {
            return;
        }
        // close current file, and rename it to datedFilename
        this.closeFile();
        File target = new File(nextFilename);
        if (target.exists()) {
            target.delete();
        }
        File file = new File(fileName);
        boolean result = file.renameTo(target);
        if (result) {
            LogLog.warn(format.format(now) + " -> " + fileName + " -> renameTo -> " + nextFilename);
        } else {
            LogLog.error(format.format(now) + " -> Failed to rename [" + fileName + "] to ["
                    + nextFilename + "].");
        }
        try {
            // This will also close the file. This is OK since multiple
            // close operations are safe.
            this.setFile(fileName, true, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            errorHandler.error("setFile(" + fileName + ", true) call failed.");
        }
        nextFilename = datedFilename;
        // 要删除的文件名
        String dfn = fileName + this.getDeleteDateStr(now, type);
        try {
            // 获取该目录下所有文件
            File df = new File(dfn);
            now.setTime(System.currentTimeMillis());
            LogLog.warn(format.format(now) + " -> [" + dfn + "] exists " + df.exists());
            if (df.exists()) {
                boolean flag = df.delete();
                now.setTime(System.currentTimeMillis());
                LogLog.warn(format.format(now) + " -> [" + dfn + "] delete " + flag + ".");
            }
        } catch (Exception e) {
            now.setTime(System.currentTimeMillis());
            LogLog.error(format.format(now) + " -> Failed to delete [" + dfn + "]." + e.toString());
        }
    }

    /**
     * This method differentiates DailyRollingFileAppender from its super class.
     *
     * <p>
     * Before actually logging, this method will check whether it is time to do a rollover. If it
     * is, it will schedule the next rollover time and then rollover.
     */
    protected void subAppend(LoggingEvent event) {
        long n = System.currentTimeMillis();
        if (n >= nextCheckTime) {
            now.setTime(n);
            nextCheckTime = this.getRc().getNextCheckMillis(now, type);
            try {
                rollOver();
            } catch (IOException ioe) {
                if (ioe instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
                LogLog.error(format.format(now) + " -> rollOver() failed.", ioe);
            }
        }
        super.subAppend(event);
    }

    /**
     * 获取日期格式是按什么单位滚动的（单位：年、月、周、日、时、分）
     */
    public int getType() {
        // The gmtTimeZone is used only in computeCheckPeriod() method.
        TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");
        RollingCalendar rollingCalendar = new RollingCalendar(gmtTimeZone, Locale.getDefault());
        // set sate to 1970-01-01 00:00:00 GMT
        Date epoch = new Date(0);
        if (this.getDatePattern() != null) {
            for (int i = TOP_OF_MINUTE; i <= TOP_OF_MONTH; i++) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.getDatePattern());
                simpleDateFormat.setTimeZone(gmtTimeZone); // do all date formatting in GMT
                String r0 = simpleDateFormat.format(epoch);
                // rollingCalendar.setType(i);
                Date next = new Date(rollingCalendar.getNextCheckMillis(epoch, i));
                String r1 = simpleDateFormat.format(next);
                // System.out.println("Type = "+i+", r0 = "+r0+", r1 = "+r1);
                if (r0 != null && r1 != null && !r0.equals(r1)) {
                    return i;
                }
            }
        }
        return TOP_OF_TROUBLE; // Deliberately head for trouble...
    }

    /**
     * 获取要删除的文件中的日期格式
     * 
     * @param now 当前时间
     * 
     * @return 下次滚动时间（单位：ms）
     */
    public String getDeleteDateStr(Date now, int type) {
        if (sdf == null) {
            sdf = new SimpleDateFormat(this.getDatePattern());
        }
        return sdf.format(this.getRc().getNextCheckDate(now, type, -maxBackupIndex));
    }
}
