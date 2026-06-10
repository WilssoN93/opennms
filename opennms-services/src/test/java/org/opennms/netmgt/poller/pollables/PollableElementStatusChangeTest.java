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
package org.opennms.netmgt.poller.pollables;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.poller.AsyncPollingEngine;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.scheduler.Schedule;
import org.opennms.netmgt.scheduler.ScheduleInterval;
import org.opennms.netmgt.xml.event.Event;

/**
 * Guards against duplicate nodeDown (and nodeUp) when processStatusChange is invoked
 * multiple times without an intervening status transition — e.g. scheduling several
 * services during reconcile after a category change.
 */
public class PollableElementStatusChangeTest {

    private RecordingPollContext m_context;
    private PollableNetwork m_network;

    @Before
    public void setUp() {
        m_context = new RecordingPollContext();
        m_context.setNodeProcessingEnabled(true);
        m_network = new PollableNetwork(m_context);
    }

    @Test
    public void repeatedProcessStatusChangeOnDownNodeEmitsSingleNodeDown() {
        final PollableNode node = m_network.createNode(1, "node1", null);
        node.updateStatus(PollStatus.down());

        node.processStatusChange(new Date());
        node.processStatusChange(new Date());
        node.processStatusChange(new Date());

        assertEquals(1, m_context.getSentCount(EventConstants.NODE_DOWN_EVENT_UEI));
    }

    @Test
    public void schedulingMultipleDownServicesEmitsSingleNodeDown() throws LockUnavailable {
        final InetAddress addr = InetAddressUtils.addr("192.168.1.1");
        final PollableNode node = m_network.createNode(1, "node1", null);
        final PollableInterface iface = node.createInterface(addr);
        iface.updateStatus(PollStatus.down());
        node.recalculateStatus();
        node.processStatusChange(new Date());

        int ifServiceId = 1;
        for (final String svcName : new String[] { "ICMP", "HTTP", "SNMP" }) {
            iface.createService(svcName, ifServiceId++);
            node.recalculateStatus();
            node.processStatusChange(new Date());
        }

        assertEquals(1, m_context.getSentCount(EventConstants.NODE_DOWN_EVENT_UEI));
    }

    @Test
    public void servicePollGoingDownEmitsNodeDown() throws LockUnavailable {
        final InetAddress addr = InetAddressUtils.addr("192.168.1.1");
        final PollableNode node = m_network.createNode(1, "node1", null);
        final PollableInterface iface = node.createInterface(addr);
        final PollableService svc = iface.createService("ICMP", 1);

        final PollableServiceConfig pollConfig = mock(PollableServiceConfig.class);
        when(pollConfig.getCurrentTime()).thenReturn(0L);
        when(pollConfig.poll()).thenReturn(PollStatus.down());
        svc.setPollConfig(pollConfig);
        final Schedule schedule = mock(Schedule.class);
        final ScheduleInterval interval = mock(ScheduleInterval.class);
        when(schedule.getInterval()).thenReturn(interval);
        when(interval.scheduledSuspension()).thenReturn(false);
        svc.setSchedule(schedule);

        svc.doPoll();

        assertEquals(1, m_context.getSentCount(EventConstants.NODE_DOWN_EVENT_UEI));
    }

    @Test
    public void newDownTransitionAfterResetEmitsAnotherNodeDown() {
        final PollableNode node = m_network.createNode(1, "node1", null);
        node.updateStatus(PollStatus.down());
        node.processStatusChange(new Date());

        node.updateStatus(PollStatus.up());
        node.processStatusChange(new Date());

        node.updateStatus(PollStatus.down());
        node.processStatusChange(new Date());

        assertEquals(2, m_context.getSentCount(EventConstants.NODE_DOWN_EVENT_UEI));
    }

    private static final class RecordingPollContext implements PollContext {
        private final AtomicInteger m_nodeDownCount = new AtomicInteger();
        private boolean m_nodeProcessingEnabled;

        @Override
        public boolean isNodeProcessingEnabled() {
            return m_nodeProcessingEnabled;
        }

        void setNodeProcessingEnabled(final boolean enabled) {
            m_nodeProcessingEnabled = enabled;
        }

        int getSentCount(final String uei) {
            if (EventConstants.NODE_DOWN_EVENT_UEI.equals(uei)) {
                return m_nodeDownCount.get();
            }
            return 0;
        }

        @Override
        public PollEvent sendEvent(final Event event) {
            if (EventConstants.NODE_DOWN_EVENT_UEI.equals(event.getUei())) {
                m_nodeDownCount.incrementAndGet();
            }
            return null;
        }

        @Override
        public Event createEvent(final String uei, final int nodeId, final InetAddress address,
                final String svcName, final Date date, final String reason) {
            final Event event = new Event();
            event.setUei(uei);
            event.setNodeid((long) nodeId);
            return event;
        }

        @Override
        public String getCriticalServiceName() {
            return null;
        }

        @Override
        public boolean isPollingAllIfCritServiceUndefined() {
            return false;
        }

        @Override
        public void openOutage(final PollableService pSvc, final PollEvent svcLostEvent) {
        }

        @Override
        public void resolveOutage(final PollableService pSvc, final PollEvent svcRegainEvent) {
        }

        @Override
        public boolean isServiceUnresponsiveEnabled() {
            return false;
        }

        @Override
        public void trackPoll(final PollableService service, final PollStatus result) {
        }

        @Override
        public boolean isAsyncEngineEnabled() {
            return false;
        }

        @Override
        public AsyncPollingEngine getAsyncPollingEngine() {
            return null;
        }

        @Override
        public long getEventTreeLockTimeoutMs() {
            return 60_000L;
        }
    }
}
