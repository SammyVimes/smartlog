package org.smartlog;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.smartlog.format.SimpleTextFormat;
import org.smartlog.output.Output;
import org.smartlog.output.Slf4JOutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.smartlog.TraceFlag.WRITE_TIME;

public class SmartLogTest {
    private final Logger logger = mock(Logger.class);

    private final Output output = Slf4JOutput.create()
            .withLogger(logger)
            .build();

    @Before
    public void setup() {
        reset(logger);

        when(logger.isDebugEnabled()).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isWarnEnabled()).thenReturn(true);
        when(logger.isErrorEnabled()).thenReturn(true);
    }


    @Test
    public void complexTest1() throws Exception {
        SmartLog.start(output)
                .level(LogLevel.DEBUG)
                .title("test-%s", "title")
                .format(new SimpleTextFormat("${title} - ${result}, var=${var}, trace: [${trace}] [${time} ms]"))
                .append("trace1")
                .append("trace2")
                .append(WRITE_TIME, "trace3")
                .append("trace%d", 4)
                .put("var", "val")
                .result("test-result");

        SmartLog.finish();

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).debug(msgCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("test-title - test-result, var=val, trace: \\[trace1; trace2; trace3 \\[\\d+ ms\\]; trace4\\] \\[\\d+ ms\\]");
    }

    @Test
    public void complexTest2() throws Exception {
        System.setProperty(LogContext.WRITE_SENSITIVE_DATA_PROPERTY_NAME, "true");
        SmartLog.start(output);

        SmartLog.format(new SimpleTextFormat("${title} - ${result}, var=${var}, trace: [${trace}] [${time} ms]"));
        SmartLog.level(LogLevel.DEBUG);
        SmartLog.title("test-title");
        SmartLog.put("var", "val");
        SmartLog.append("trace1");
        SmartLog.append("trace2");
        SmartLog.append(TraceFlag.MARK_TIME, "trace3");
        SmartLog.append(TraceFlag.WRITE_TIME, "trace4");
        SmartLog.append(TraceFlag.WRITE_AND_MARK_TIME, "trace5");
        SmartLog.append("trace%d", 6);
        SmartLog.append(TraceFlag.MARK_TIME, "trace%d", 7);
        when(logger.isDebugEnabled()).thenReturn(false);
        SmartLog.ifDebug("debug0");
        SmartLog.sensitive(TraceFlag.WRITE_TIME, "sensitive no debug");
        when(logger.isDebugEnabled()).thenReturn(true);
        SmartLog.ifDebug("debug1");
        SmartLog.ifDebug("debug%s", 2);
        SmartLog.ifDebug(TraceFlag.NONE, "debug%s", 3);
        SmartLog.ifDebug(TraceFlag.WRITE_TIME, "debug4");
        SmartLog.sensitive(TraceFlag.WRITE_TIME, "sensitive1");
        SmartLog.sensitive("sensitive2");
        SmartLog.sensitive("sensitive%s", 3);
        SmartLog.sensitive(TraceFlag.NONE, "sensitive4");
        SmartLog.sensitive(TraceFlag.WRITE_TIME, "sensitive%s", 5);

        SmartLog.result("test-result");

        Thread.sleep(5);

        SmartLog.finish();

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).debug(msgCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("test-title - test-result, var=val, trace: \\[trace1; trace2; trace3; trace4 \\[\\d+ ms\\]; trace5 \\[\\d+ ms\\]; trace6; trace7; \\[\\d+ ms\\]; debug1; debug2; debug3; debug4 \\[\\d+ ms\\]; sensitive1 \\[\\d+ ms\\]; sensitive2; sensitive3; sensitive4; sensitive5 \\[\\d+ ms\\]\\] \\[\\d+ ms\\]");
    }

    @Test
    public void testTitleWithArgs() throws Exception {
        SmartLog.start(output);

        SmartLog.format(new SimpleTextFormat("${title}"));
        SmartLog.title("test-title: %d", 42);
        SmartLog.sensitive("sensitive off");
        SmartLog.finish();

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(msgCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .isEqualTo("test-title: 42");
    }

    @Test
    public void testResultWithArgs() throws Exception {
        SmartLog.start(output);

        SmartLog.format(new SimpleTextFormat("${result}"));
        SmartLog.result("test-result: %d", 42);

        SmartLog.finish();

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(msgCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("test-result: 42");
    }

    @Test
    public void testResultWithLevel() throws Exception {
        SmartLog.start(output);

        SmartLog.format(new SimpleTextFormat("${result}"));
        SmartLog.result(LogLevel.WARN, "test-result");

        SmartLog.finish();

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).warn(msgCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("test-result");
    }

    @Test
    public void testResultWithLevelAndArgs() throws Exception {
        SmartLog.start(output);

        SmartLog.format(new SimpleTextFormat("${result}"));
        SmartLog.result(LogLevel.WARN, "test-result: %d", 42);

        SmartLog.finish();

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).warn(msgCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("test-result: 42");
    }

    @Test
    public void testResultWithLevelAndThrowable() throws Exception {
        SmartLog.start(output);

        SmartLog.format(new SimpleTextFormat("${result}"));
        final RuntimeException exception = new RuntimeException("test");
        SmartLog.result(LogLevel.WARN, exception, "test-result");

        SmartLog.finish();

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(logger).warn(msgCaptor.capture(), exceptionCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("test-result");
        assertThat(exceptionCaptor.getValue())
                .isSameAs(exception);
    }

    @Test
    public void testResultWithLevelAndThrowableAndArgs() throws Exception {
        SmartLog.start(output);

        SmartLog.format(new SimpleTextFormat("${result}"));
        final RuntimeException exception = new RuntimeException("test");
        SmartLog.result(LogLevel.WARN, exception, "test-result: %d", 42);

        SmartLog.finish();

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(logger).warn(msgCaptor.capture(), exceptionCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("test-result: 42");
        assertThat(exceptionCaptor.getValue())
                .isSameAs(exception);
    }

    @Test
    public void testWithException() throws Exception {
        final Exception e1 = new Exception("e1");
        final Exception e2 = new Exception("e2");
        final Exception e3 = new Exception("e3");

        SmartLog.start(output)
                .throwable(e1)
                .throwable(e2)
                .throwable(e3);

        SmartLog.finish();

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(logger).info(anyString(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).hasSuppressedException(e1);
        assertThat(exceptionCaptor.getValue()).hasSuppressedException(e2);
        assertThat(exceptionCaptor.getValue()).isSameAs(e3);
    }

    @Test
    public void testCallback() throws Exception {
        final LoggableCallback loggableCallback = mock(LoggableCallback.class);
        SmartLog.start(output, loggableCallback);

        Mockito.verify(loggableCallback).beforeLoggable();

        SmartLog.finish();

        Mockito.verify(loggableCallback).afterLoggable();
    }

    @Test
    public void testCallbackExceptions() throws Exception {
        final LoggableCallback loggableCallback = mock(LoggableCallback.class);
        final RuntimeException beforeLoggableException = new RuntimeException("before-loggable-exception");
        final RuntimeException afterLoggableException = new RuntimeException("after-loggable-exception");

        doThrow(beforeLoggableException)
                .when(loggableCallback).beforeLoggable();
        doThrow(afterLoggableException)
                .when(loggableCallback).afterLoggable();

        SmartLog.start(output, loggableCallback);

        SmartLog.finish();

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(logger).info(anyString(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isSameAs(afterLoggableException);
        assertThat(exceptionCaptor.getValue()).hasSuppressedException(beforeLoggableException);
    }

    @Test
    public void testDefaultOutput() throws Exception {
        final Object loggableCallback = new LoggableCallback() {
        };
        final Output output = mock(Output.class);
        SmartLogConfig.getConfig().setDefaultOutputResolver(clazz -> {
            // should be external class
            Assertions.assertThat(clazz).isEqualTo(SmartLogTest.class);
            return output;
        });

        SmartLog.start(loggableCallback);
        Assertions.assertThat(SmartLog.current().output()).isSameAs(output);
        SmartLog.finish();
    }

    @Test
    public void testMdc() throws Exception {
        MDC.put("mdc-var1", "mdc-oldval");

        SmartLog.start(output)
                .pushMDC("mdc-var1", "mdc-val1")
                .pushMDC("mdc-var2", "mdc-val2");

        assertThat(MDC.get("mdc-var1")).isEqualTo("mdc-val1");
        assertThat(MDC.get("mdc-var2")).isEqualTo("mdc-val2");

        SmartLog.finish();

        assertThat(MDC.get("mdc-var1")).isEqualTo("mdc-oldval");
        assertThat(MDC.get("mdc-var2")).isNull();
    }

    @Test
    public void testThreadName() throws Exception {
        final String oldName = Thread.currentThread().getName();

        final LogContext ctx = SmartLog.start(output)
                .threadName("new-thread-name");


        assertThat(ctx.oldThreadName()).isEqualTo(oldName);
        assertThat(Thread.currentThread().getName()).isEqualTo("new-thread-name");

        SmartLog.finish();

        assertThat(Thread.currentThread().getName()).isEqualTo(oldName);
    }

    @Test
    public void testAutoclosable() throws Exception {
        try (LogContext ctx = SmartLog.start(output)) {
            ctx.format(new SimpleTextFormat("msg"));
        }

        final ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(msgCaptor.capture());

        Assertions.assertThat(msgCaptor.getValue())
                .matches("msg");
    }

    @Test
    public void testRedefineOutput() throws Exception {
        Output output = mock(Output.class);

        LogContext ctx = SmartLog.start(this.output)
                .format(new SimpleTextFormat("${title} - ${result}, var=${var}, trace: [${trace}] [${time} ms]"))
                .output(output);

        SmartLog.finish();

        Mockito.verify(logger, never()).info(anyString());
        Mockito.verify(output).write(same(ctx));
    }

    @Test
    public void currentThrowExceptionIfNoLogContext() throws Exception {
        try {
            SmartLog.current();
        } catch (Exception e) {
            Assertions.assertThat(e).hasMessage("Loggable context is absent");
        }
    }

    @Test
    public void finishThrowExceptionIfNoLogContext() throws Exception {
        try {
            SmartLog.finish();
        } catch (Exception e) {
            Assertions.assertThat(e).hasMessage("Loggable context is absent");
        }
    }

    @Test
    public void testNestedContexts() throws Exception {
        Output output = mock(Output.class);

        final LogContext outer = SmartLog.start(output);
        final LogContext inner = SmartLog.start(output);

        assertThat(SmartLog.current()).isSameAs(inner);
        SmartLog.finish();
        assertThat(SmartLog.current()).isSameAs(outer);
        SmartLog.finish();
    }

    @Test
    public void testLogAndThrow() {
        SmartLog.start(output);

        final LogContext ctx = SmartLog.start(output);

        final Exception exception = new Exception();

        ctx.result(LogLevel.ERROR, exception, "Exception will be thrown");

        ctx.throwable(exception);

        output.write(ctx);
    }

    @Test
    public void testLogAndThrowAnotherException() {
        SmartLog.start(output);

        final LogContext ctx = SmartLog.start(output);

        final Exception exception = new Exception();
        final Exception exception2 = new Exception(exception);

        ctx.result(LogLevel.ERROR, exception, "Exception will be thrown");

        ctx.throwable(exception2);

        output.write(ctx);
    }

}