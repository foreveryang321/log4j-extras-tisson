# log4j-extras-tisson扩展

`log4j-extras-tisson` 扩展使用说明

### 使用 `log4j` 按天滚动日志文件
```properties
log4j.appender.ROLLINGFILE=org.apache.log4j.DailyRollingFileAppender
# DatePattern 默认 "'.'yyyy-MM-dd"
log4j.appender.ROLLINGFILE.DatePattern='.'yyyy-MM-dd
```

### 使用 `log4j-extras-tisson` 扩展的 `DailyRollingFileAppender` 按天滚动日志文件
```properties
log4j.appender.ROLLINGFILE=cn.tisson.container.log4j.extensions.DailyRollingFileAppender
# DatePattern 默认 "'.'yyyy-MM-dd"
log4j.appender.ROLLINGFILE.DatePattern='.'yyyy-MM-dd
# 配置日志文件备份个数，参考o rg.apache.log4j.RollingFileAppender 的 MaxBackupIndex 配置。MaxBackupIndex默认30
log4j.appender.ROLLINGFILE.MaxBackupIndex=7
```

> 注意：DatePattern的最小单位是分钟，最大单位是月份
