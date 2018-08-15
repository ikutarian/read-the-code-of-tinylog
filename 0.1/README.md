## 官方源码

https://github.com/pmwmedia/tinylog/releases/tag/0.1

## 使用示例

要使用 tinylog，要注意两点：

1. 必须指定日志输出工具（log writter），比如输出到控制台到工具或者输出到文件的工具
2. 默认日志级别是 `info`
3. 默认日志格式是 `{date:yyyy-MM-dd HH:mm:ss} [{thread}] {method}\n{level}: {message}`

```java
public class Application {

    public static void main(String[] args) {
        Logger.setWriter(new ConsoleLoggingWriter());
        Logger.info("hell world");
    }
}
```

## 源码结构

|类名|作用|
|:--|:--|
|ELoggingLevel|日志级别枚举|
|ILoggingWriter|日志输出工具接口定义类|
|ConsoleLoggingWriter|输出日志到控制台的工具|
|FileLoggingWriter|输出日志到文件的工具|
|Logger|日志输出类，提供了一些静态方法|

## 源码分析

阅读源码的入口就是从使用 demo 开始，比如上面的使用示例

```java
Logger.info("hell world")
```

这就是一个很好的入口。点进去看看源码

```java
public static void info(final String message, final Object... arguments) {
    output(ELoggingLevel.INFO, null, message, arguments);
}
```

发现 `info()` 方法内部调用了 `output()` 方法，`output()` 方法参数为

```java
output(final ELoggingLevel level, final Throwable exception, final String message, final Object... arguments)
```

该方法需要传入

- 日志级别
- 异常
- 日志文字信息
- 日志文字信息的参数（参数个数不限制）

方法实现如下

```java
private static void output(final ELoggingLevel level,
    final Throwable exception, final String message, final Object... arguments) {
    ILoggingWriter currentWriter = loggingWriter;

    // 是否有日志输出器writter
    // 日志级别是否符合
    if (currentWriter != null && loggingLevel.ordinal() <= level.ordinal()) {
        try {
            // 获取当前线程名称
            String threadName = Thread.currentThread().getName();
            // 获取输出日志的方法名称
            String methodName = getMethodName();
            Date now = new Date();

            String text;
            if (message != null) {
                // 格式化日志文字信息
                text = MessageFormat.format(message, arguments);
            } else {
                text = null;
            }

            // 根据日志格式，格式化日志
            String logEntry = createEntry(threadName, methodName, now, level, exception, text);
            // 调用日志输出器writter，输出日志
            currentWriter.write(level, logEntry);
        } catch (Exception ex) {
            error(ex, "Could not create log entry");
        }
    }
}
```

核心方法在于

- 获取输出日志的方法名称：getMethodName()
- 格式化日志文字信息：MessageFormat.format(message, arguments)
- 根据日志格式，格式化日志：createEntry(threadName, methodName, now, level, exception, text)
- 调用日志输出器writter，输出日志：currentWriter.write(level, logEntry)

现在来分别看一下

### getMethodName()

方法实现如下

```java
private static String getMethodName() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    if (stackTraceElements.length > 4) {
        StackTraceElement stackTraceElement = stackTraceElements[4];
        return stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + "()";
    } else {
        return null;
    }
}
```

原理就是获取调用栈（`StackTraceElement`），然后调用 `StackTraceElement` 的 `getClassName()` 获取类名，调用 `StackTraceElement` 的 `getMethodName()` 获取方法名。

需要注意的是调用栈的层级。从调用 `Logger.info("hell world")` 开始到 `getMethodName()` 内的 `StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();`，一共经历了 5 次的方法调用，于是调用栈层级是 5，而要获取调用 `Logger.info("hell world")` 的方法 `main()` 正好在第 4 层级，所以这里使用 `stackTraceElements[4]` 获取调用栈

### MessageFormat.format(message, arguments)

调用 `java.text.MessageFormat` 类的 `format` 方法。`message` 的格式有一定的要求，比如

```java
int planet = 7;
String event = "a disturbance in the Force";
  
String result = MessageFormat.format(
    "At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.",
    planet, new Date(), event);
```

### createEntry(threadName, methodName, now, level, exception, text)

默认的日志格式是 `{date} [{thread}] {method}\n{level}: {message}`，于是这个方法就按照这个格式对日志进行格式化并返回

```java
private static String createEntry(final String threadName, 
    final String methodName, final Date time, 
    final ELoggingLevel level, final Throwable exception, final String text) {
    StringBuilder builder = new StringBuilder();

    for (String token : loggingEntryTokens) {
        if ("{thread}".equals(token)) {
            builder.append(threadName);
        } else if ("{method}".equals(token)) {
            builder.append(methodName);
        } else if ("{level}".equals(token)) {
            builder.append(level);
        } else if ("{message}".equals(token)) {
            if (text != null) {
                builder.append(text);
            }
            if (exception != null) {
                if (text != null) {
                    builder.append(": ");
                }
                builder.append(getPrintedException(exception, 0));
            }
        } else if (token.startsWith("{date") && token.endsWith("}")) {
            String dateFormatPattern;
            if (token.length() > 6) {
                dateFormatPattern = token.substring(6, token.length() - 1);
            } else {
                dateFormatPattern = DEFAULT_DATE_FORMAT_PATTERN;
            }
            builder.append(new SimpleDateFormat(dateFormatPattern).format(time));
        } else {
            builder.append(token);
        }
    }
    builder.append("\n");

    return builder.toString().replaceAll("\n", NEW_LINE);
}
```

### currentWriter.write(level, logEntry)

调用日志输出器writter进行日志输出。这里采用的是面向接口编程的方式。`currentWritter` 是 `ILoggingWriter` 接口，需要由接口的实现类来进行日志输出。目前代码提供了两个实现类：

- ConsoleLoggingWriter
- FileLoggingWriter

### ConsoleLoggingWriter

```java
package org.pmw.tinylog;

import java.io.PrintStream;

public class ConsoleLoggingWriter implements ILoggingWriter {

	public ConsoleLoggingWriter() {
		super();
	}

	@Override
	public final void write(final ELoggingLevel level, final String logEntry) {
		getPrintStream(level).print(logEntry);
	}

	private static PrintStream getPrintStream(final ELoggingLevel level) {
		if (level == ELoggingLevel.ERROR || level == ELoggingLevel.WARNING) {
			return System.err;
		} else {
			return System.out;
		}
	}
}
```

如果日志级别是 `error` 或者 `waring` 的话，就以 `System.err` 输出日志，否则就使用 `System.out`

### FileLoggingWriter

可能会有多个线程对文件进行读写，所以需要进行加锁。代码是使用了 `Lock` 对资源进行加锁

```java
public class FileLoggingWriter implements ILoggingWriter {

	private final Lock lock;
	private final BufferedWriter writer;
	private boolean isClosed;

	public FileLoggingWriter(final File file) throws IOException {
		super();
		this.lock = new ReentrantLock();
		this.writer = new BufferedWriter(new FileWriter(file));
		this.isClosed = false;
	}

	@Override
	public final void write(final ELoggingLevel level, final String logEntry) {
        // 加锁
		lock.lock();
		try {
			if (!isClosed) {
                // 如果文件没有关闭就写入日志
				writer.write(logEntry);
				writer.flush();
			}
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
		} finally {
            // 释放锁
			lock.unlock();
		}
	}

	/**
	 * Close the log file.
	 */
	public final void close() throws IOException {
        // 加锁
		lock.lock();
		try {
			if (!isClosed) {
                // 如果文件没有关闭就关闭文件
				isClosed = true;
				writer.close();
			}
		} finally {
            // 释放锁
			lock.unlock();
		}
	}

	@Override
	protected final void finalize() throws Throwable {
        // JVM 执行 GC 时，会调用该方法
		close();
	}
}
```

对资源加锁，之前一直使用的是 `synchronized`，怎么还可以用 `Lock` ？两者有什么区别？

所谓 `synchronized`：

> 如果一个代码块被 `synchronized` 修饰了，当一个线程获取了对应的锁，并执行该代码块时，其他线程便只能一直等待，等待获取锁的线程释放锁

获取锁的线程释放锁只会有两种情况：

1. 获取锁的线程执行完了该代码块，然后线程释放对锁的占有
2. 线程执行发生异常，此时JVM会让线程自动释放锁

那么如果这个获取锁的线程由于要等待IO或者其他原因（比如调用 `sleep()` 方法）被阻塞了，但是又没有释放锁，其他线程便只能干巴巴地等待，试想一下，这多么影响程序执行效率。

因此就需要有一种机制可以不让等待的线程一直无期限地等待下去（比如只等待一定的时间或者能够响应中断），通过 `Lock` 就可以办到。

再举个例子：当有多个线程读写文件时，读操作和写操作会发生冲突现象，写操作和写操作会发生冲突现象，但是读操作和读操作不会发生冲突现象。但是采用 `synchronized` 关键字来实现同步的话，就会导致一个问题：如果多个线程都只是进行读操作，所以当一个线程在进行读操作时，其他线程只能等待无法进行读操作。

因此就需要一种机制来使得多个线程都只是进行读操作时，线程之间不会发生冲突，通过 `Lock` 就可以办到。

另外，通过Lock可以知道线程有没有成功获取到锁。这个是 `synchronized` 无法办到的。

总结一下，也就是说Lock提供了比synchronized更多的功能。但是要注意以下几点：

1. `Lock` 不是 Java 语言内置的，`synchronized` 是Java 语言的关键字，因此是内置特性。`Lock` 是一个类，通过这个类可以实现同步访问；

2. `Lock` 和 `synchronized` 有一点非常大的不同，采用 `synchronized` 不需要用户去手动释放锁，当 `synchronized` 方法或者 `synchronized` 代码块执行完之后，系统会自动让线程释放对锁的占用；而 `Lock` 则必须要用户去手动释放锁，如果没有主动释放锁，就有可能导致出现死锁现象。