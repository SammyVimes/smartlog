package org.smartlog;

import org.smartlog.format.SimpleTextFormat;
import org.smartlog.output.Output;
import org.smartlog.output.Slf4JOutput;

/**
 *
 */
public class ExampleNoAspect {
    private static final Output SL_OUTPUT = Slf4JOutput.create()
            .withLoggerFor(ExampleNoAspect.class)
            .withFormat(new SimpleTextFormat("${title}, result: [${result}], var=${var}, trace: [${trace}] [${time} ms]"))
            .build();

    public static void example1() {
        SmartLog.start(SL_OUTPUT)
                .title("main");

        try {
            SmartLog.append("test");
            SmartLog.append("hello %s", "alice");

            SmartLog.put("var", 5);
            SmartLog.ifDebug("debug");

            SmartLog.sensitive("sensitive data");

            SmartLog.result("OK");

        } finally {
            SmartLog.finish();
        }
    }

    public static void example2() {
        try (LogContext log = SmartLog.start(SL_OUTPUT).title("main")) {
            log.append("test");
            log.append("hello %s", "alice");

            log.put("var", 5);

            log.result("OK");
        }
    }

    public static void main(final String[] args) {
        ExampleNoAspect.example1();
        ExampleNoAspect.example2();
    }
}