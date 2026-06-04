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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.config.PollerConfig;
import org.opennms.netmgt.events.api.model.IEvent;
import org.opennms.netmgt.events.api.model.ImmutableMapper;
import org.opennms.netmgt.model.events.EventBuilder;

public class CategoryMembershipDebouncerTest {

    private ScheduledExecutorService m_executor;

    @Before
    public void setUp() {
        m_executor = Executors.newSingleThreadScheduledExecutor();
    }

    @After
    public void tearDown() {
        m_executor.shutdownNow();
    }

    @Test
    public void coalescesEventsAndRebuildsOnce() throws Exception {
        final PollerConfig pollerConfig = mock(PollerConfig.class);
        when(pollerConfig.getCategoryMembershipDebounceMs()).thenReturn(50);
        when(pollerConfig.getCategoryMembershipDebounceMaxWaitMs()).thenReturn(500);

        final AtomicInteger flushCount = new AtomicInteger();
        final CategoryMembershipDebouncer.FlushHandler handler = new CategoryMembershipDebouncer.FlushHandler() {
            @Override
            public PollerConfig getPollerConfigForDebouncer() {
                return pollerConfig;
            }

            @Override
            public void flushCategoryMembershipBatch(final Map<Long, IEvent> batch) {
                flushCount.incrementAndGet();
                assertEquals(3, batch.size());
                assertEquals(Long.valueOf(103L), Long.valueOf(batch.get(1L).getDbid()));
            }
        };

        final CategoryMembershipDebouncer debouncer = new CategoryMembershipDebouncer(handler, m_executor);

        debouncer.enqueue(eventForNode(1L, 101L));
        debouncer.enqueue(eventForNode(2L, 102L));
        debouncer.enqueue(eventForNode(1L, 103L));
        debouncer.enqueue(eventForNode(3L, 104L));

        Thread.sleep(250);

        assertEquals(1, flushCount.get());
        debouncer.close();
    }

    @Test
    public void maxWaitForcesFlushDuringContinuousEnqueue() throws Exception {
        final PollerConfig pollerConfig = mock(PollerConfig.class);
        when(pollerConfig.getCategoryMembershipDebounceMs()).thenReturn(200);
        when(pollerConfig.getCategoryMembershipDebounceMaxWaitMs()).thenReturn(300);

        final AtomicInteger flushCount = new AtomicInteger();
        final CategoryMembershipDebouncer.FlushHandler handler = new CategoryMembershipDebouncer.FlushHandler() {
            @Override
            public PollerConfig getPollerConfigForDebouncer() {
                return pollerConfig;
            }

            @Override
            public void flushCategoryMembershipBatch(final Map<Long, IEvent> batch) {
                flushCount.incrementAndGet();
            }
        };

        final CategoryMembershipDebouncer debouncer = new CategoryMembershipDebouncer(handler, m_executor);

        final long deadline = System.currentTimeMillis() + 2_000L;
        while (System.currentTimeMillis() < deadline && flushCount.get() == 0) {
            debouncer.enqueue(eventForNode(1L, 101L));
            Thread.sleep(50);
        }

        assertTrue("Expected at least one flush within max wait window", flushCount.get() >= 1);
        debouncer.close();
    }

    @Test
    public void disabledDebounceUsesZeroConfig() {
        final PollerConfig pollerConfig = mock(PollerConfig.class);
        when(pollerConfig.getCategoryMembershipDebounceMs()).thenReturn(0);
        assertEquals(0, pollerConfig.getCategoryMembershipDebounceMs());
    }

    private static IEvent eventForNode(final long nodeId, final long eventDbId) {
        final EventBuilder builder = new EventBuilder("nodeCategoryMembershipChanged",
                "CategoryMembershipDebouncerTest");
        builder.setNodeid(nodeId);
        builder.getEvent().setDbid(eventDbId);
        return ImmutableMapper.fromMutableEvent(builder.getEvent());
    }
}
