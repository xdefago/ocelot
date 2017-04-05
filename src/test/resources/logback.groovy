// Logback configuration file (test)
//
// https://logback.qos.ch/manual/groovy.html
//

import ch.qos.logback.classic.filter.ThresholdFilter

def patternExpressionLong = "%date{HH:mm:ss.SSS} [%-5level] %logger{5} | %msg%n"
def patternExpressionShort = "%5relative [%-5level] %logger{0} | %msg%n"

appender("FILE", FileAppender) {
    file = "test.log"
    append = false
    filter(ThresholdFilter) {
        level = TRACE
    }
    encoder(PatternLayoutEncoder) {
        pattern = patternExpressionLong
    }
}

appender("CONSOLE", ConsoleAppender) {
    filter(ThresholdFilter) {
        level = OFF
    }
    encoder(PatternLayoutEncoder) {
        pattern = patternExpressionShort
    }
    target = "System.err"
}

logger("ocelot", TRACE)
root(OFF, ["CONSOLE", "FILE"])
