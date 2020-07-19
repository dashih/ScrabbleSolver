import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

public final class StatusReporter implements AutoCloseable {
    private static final int STATUS_UPDATE_MS = 200;
    private static final int STATUS_BAR_SIZE = 8;

    private final ScheduledExecutorService m_executor;
    private int m_cursor;
    private boolean m_direction;

    StatusReporter() {
        m_executor = Executors.newSingleThreadScheduledExecutor();
        m_cursor = 0;
        m_direction = true;
        m_executor.scheduleAtFixedRate(() -> {
            int leftPad = m_cursor;
            int rightPad = STATUS_BAR_SIZE - leftPad - 1;
            System.out.printf("|%s=%s|\r",
                StringUtils.repeat('-', leftPad),
                StringUtils.repeat('-', rightPad));

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
        }, 0, STATUS_UPDATE_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        m_executor.shutdown();
    }
}
