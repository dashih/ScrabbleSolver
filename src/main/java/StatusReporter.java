import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

public final class StatusReporter implements AutoCloseable {
    private static final int STATUS_UI_MS = 500; // milliseconds
    private static final int STATUS_BAR_SIZE = 40;
    private static final char[] SPINNER = "|/-\\".toCharArray();

    // Initial delay hardcoded to 2 seconds to not get stuck on (processed 0) where matches have clearly been found.
    private static final int STATUS_COUNT_INITIAL_DELAY = 2; // seconds
    private static final int STATUS_COUNT_S = 10; // seconds

    private final long m_goal;
    private final ScheduledExecutorService m_executor;
    private final ConcurrentMap<Long, Long> m_counts;

    private long m_count;
    private int m_spinnerPos;

    StatusReporter(long goal) {
        m_goal = goal;
        m_executor = Executors.newSingleThreadScheduledExecutor();
        m_counts = new ConcurrentHashMap<>();
        m_count = 0L;
        m_spinnerPos = 0;
    }

    void start() {
        m_executor.scheduleAtFixedRate(() -> {
            float percDone = (float)m_count / m_goal;
            int pos = Math.round(percDone * STATUS_BAR_SIZE);

            m_spinnerPos++;
            if (m_spinnerPos == SPINNER.length) {
                m_spinnerPos = 0;
            }

            String status = StringUtils.rightPad(
                String.format("[%s%c%s] %.2f%%",
                    StringUtils.repeat('=', pos == 0 ? 0 : pos - 1),
                    SPINNER[m_spinnerPos],
                    StringUtils.repeat(' ', STATUS_BAR_SIZE - pos),
                    percDone * 100.0f),
                Main.CLI_PAD);
            System.out.print(status + "\r");
        }, 0, STATUS_UI_MS, TimeUnit.MILLISECONDS);

        m_executor.scheduleAtFixedRate(this::get, STATUS_COUNT_INITIAL_DELAY, STATUS_COUNT_S, TimeUnit.SECONDS);
    }

    long get() {
        m_count = m_counts.values().stream().mapToLong(Long::longValue).sum();
        return m_count;
    }

    void increment() {
        Long tid = Thread.currentThread().getId();
        m_counts.put(tid, m_counts.getOrDefault(tid, 0L) + 1);
    }

    @Override
    public void close() {
        m_executor.shutdown();
    }
}
