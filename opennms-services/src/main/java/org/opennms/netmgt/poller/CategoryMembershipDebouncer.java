/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.poller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.opennms.core.concurrent.LogPreservingThreadFactory;
import org.opennms.netmgt.config.PollerConfig;
import org.opennms.netmgt.events.api.model.IEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coalesces {@code nodeCategoryMembershipChanged} events off the EventIpc listener thread.
 */
final class CategoryMembershipDebouncer implements AutoCloseable {

    interface FlushHandler {
        PollerConfig getPollerConfigForDebouncer();

        void flushCategoryMembershipBatch(java.util.Map<Long, IEvent> eventsByNodeId);
    }

    private static final Logger LOG = LoggerFactory.getLogger(CategoryMembershipDebouncer.class);

    private final FlushHandler m_handler;
    private final ConcurrentHashMap<Long, IEvent> m_pendingByNodeId = new ConcurrentHashMap<>();
    private final AtomicLong m_firstPendingAtMs = new AtomicLong(0L);
    private final AtomicBoolean m_flushInProgress = new AtomicBoolean(false);
    private final ScheduledExecutorService m_executor;
    private final Object m_scheduleLock = new Object();
    private volatile ScheduledFuture<?> m_scheduledFlush;

    CategoryMembershipDebouncer(final FlushHandler handler) {
        m_handler = handler;
        m_executor = new ScheduledThreadPoolExecutor(1,
                new LogPreservingThreadFactory("Poller:CategoryMembershipDebouncer", 1));
    }

    CategoryMembershipDebouncer(final FlushHandler handler,
            final ScheduledExecutorService executor) {
        m_handler = handler;
        m_executor = executor;
    }

    void enqueue(final IEvent event) {
        if (event == null || event.getNodeid() == null || event.getNodeid() <= 0) {
            return;
        }
        m_pendingByNodeId.put(event.getNodeid(), event);
        m_firstPendingAtMs.compareAndSet(0L, System.currentTimeMillis());
        LOG.debug("Queued nodeCategoryMembershipChanged for node {} (eventId={}), pending nodes={}",
                event.getNodeid(), event.getDbid(), m_pendingByNodeId.size());
        scheduleFlush();
    }

    private void scheduleFlush() {
        final int debounceMs = m_handler.getPollerConfigForDebouncer().getCategoryMembershipDebounceMs();
        final int maxWaitMs = m_handler.getPollerConfigForDebouncer().getCategoryMembershipDebounceMaxWaitMs();
        final long firstPending = m_firstPendingAtMs.get();
        final long now = System.currentTimeMillis();
        final long delayMs;
        if (firstPending > 0 && (now - firstPending) >= maxWaitMs) {
            delayMs = 0L;
        } else {
            delayMs = debounceMs;
        }

        synchronized (m_scheduleLock) {
            if (m_scheduledFlush != null) {
                m_scheduledFlush.cancel(false);
            }
            m_scheduledFlush = m_executor.schedule(this::flushSafely, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void flushSafely() {
        if (!m_flushInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            flushPending();
        } finally {
            m_flushInProgress.set(false);
            if (!m_pendingByNodeId.isEmpty()) {
                scheduleFlush();
            }
        }
    }

    private void flushPending() {
        if (m_pendingByNodeId.isEmpty()) {
            return;
        }
        final Map<Long, IEvent> batch = drainPending();
        if (batch.isEmpty()) {
            return;
        }
        LOG.info("Flushing {} coalesced nodeCategoryMembershipChanged event(s) for node ids: {}",
                batch.size(),
                batch.keySet().stream().sorted().map(String::valueOf).collect(Collectors.joining(",")));
        m_handler.flushCategoryMembershipBatch(batch);
    }

    private Map<Long, IEvent> drainPending() {
        final Map<Long, IEvent> batch = new LinkedHashMap<>();
        for (final Long nodeId : new ArrayList<>(m_pendingByNodeId.keySet())) {
            final IEvent removed = m_pendingByNodeId.remove(nodeId);
            if (removed != null) {
                batch.put(nodeId, removed);
            }
        }
        if (m_pendingByNodeId.isEmpty()) {
            m_firstPendingAtMs.set(0L);
            synchronized (m_scheduleLock) {
                if (m_scheduledFlush != null) {
                    m_scheduledFlush.cancel(false);
                    m_scheduledFlush = null;
                }
            }
        }
        return batch;
    }

    @Override
    public void close() {
        synchronized (m_scheduleLock) {
            if (m_scheduledFlush != null) {
                m_scheduledFlush.cancel(false);
                m_scheduledFlush = null;
            }
        }
        m_executor.shutdown();
        try {
            if (!m_executor.awaitTermination(5, TimeUnit.SECONDS)) {
                m_executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            m_executor.shutdownNow();
        }
        flushPending();
    }
}
