import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

public final class StatusReporter implements AutoCloseable {
    private static final int STATUS_UI_MS = 500; // milliseconds
    private static final int STATUS_BAR_SIZE = 8;

    // Initial delay hardcoded to 2 seconds to not get stuck on (processed 0) where matches have clearly been found.
    private static final int STATUS_COUNT_INITIAL_DELAY = 2; // seconds
    private static final int STATUS_COUNT_S = 2; // seconds

    private final ScheduledExecutorService m_executor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<Long, Long> m_counts = new ConcurrentHashMap<>();

    private int m_cursor = 0;
    private boolean m_direction = true;
    private long m_count;

    void start() {
        m_executor.scheduleAtFixedRate(() -> {
            int leftPad = m_cursor;
            int rightPad = STATUS_BAR_SIZE - leftPad - 1;
            System.out.printf("|%s=%s| (processed %,d)\r",
                StringUtils.repeat('-', leftPad),
                StringUtils.repeat('-', rightPad),
                m_count);

            if (m_direction) {
                if (m_cursor == STATUS_BAR_SIZE - 1) {
                    m_cursor--;
                    m_direction = false;
                } else {
                    m_cursor++;
                }
            } else {
                if (m_cursor == 0) {
                    m_cursor++;
                    m_direction = true;
                } else {
                    m_cursor--;
                }
            }
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
