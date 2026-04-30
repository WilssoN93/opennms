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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.events.api.model.ImmutableEvent;
import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.mock.MockPollerConfig;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.mock.MockPollContext;
import org.opennms.netmgt.poller.pollables.PollableNetwork;
import org.opennms.netmgt.poller.pollables.PollableService;
import org.opennms.netmgt.scheduler.Schedule;

public class PollerReconcileNodeTest {

    private static final int NODE_ID = 1;
    private static final String IP = "192.168.1.1";
    private static final String SVC = "ICMP";

    private PollableNetwork m_network;
    private MockPollerConfig m_pollerConfig;
    private QueryManager m_queryManager;
    private Poller m_poller;

    @Before
    public void setUp() {
        final MockPollContext context = new MockPollContext();
        m_network = new PollableNetwork(context);
        m_pollerConfig = new MockPollerConfig(null);
        m_queryManager = mock(QueryManager.class);
        when(m_queryManager.getNodeServices(NODE_ID)).thenReturn(
                Collections.singletonList(new String[] { IP, SVC }));

        m_poller = new Poller();
        m_poller.setNetwork(m_network);
        m_poller.setPollerConfig(m_pollerConfig);
        m_poller.setQueryManager(m_queryManager);
    }

    @Test
    public void unschedulesServiceWhenNoLongerPolled() throws Exception {
        final InetAddress addr = InetAddressUtils.addr(IP);
        final PollableService svc = m_network.createService(NODE_ID, "node1", null, addr, SVC, 42);
        svc.setSchedule(mock(Schedule.class));
        assertFalse(svc.isDeleted());

        m_poller.reconcileNode(NODE_ID, "node1", null, categoryChangeEvent(), false);

        assertTrue(svc.isDeleted());
    }

    @Test
    public void unschedulesServiceWhenIsPolledTrueButNoLocalPackage() throws Exception {
        final InetAddress addr = InetAddressUtils.addr(IP);
        final PollableService svc = m_network.createService(NODE_ID, "node1", null, addr, SVC, 42);
        svc.setSchedule(mock(Schedule.class));
        assertFalse(svc.isDeleted());

        // isPolled() can be true for perspective-only packages; pollerd schedules via findPackageForService().
        m_pollerConfig.setIsPolledPredicate((ip, name) -> true);

        m_poller.reconcileNode(NODE_ID, "node1", null, categoryChangeEvent(), false);

        assertTrue(svc.isDeleted());
    }

    @Test
    public void syncUnschedulesServiceWhenNoLocalPackageEvenIfPollableExists() throws Exception {
        final InetAddress addr = InetAddressUtils.addr(IP);
        final PollableService svc = m_network.createService(NODE_ID, "node1", null, addr, SVC, 42);
        svc.setSchedule(mock(Schedule.class));
        assertFalse(svc.isDeleted());

        m_pollerConfig.setFindPackageForServiceResult(null);

        m_poller.syncNodeServicesToPackageResolution(NODE_ID, "node1", null, categoryChangeEvent());

        assertTrue(svc.isDeleted());
        verify(m_queryManager).updateServiceStatus(NODE_ID, IP, SVC, "N");
    }

    @Test
    public void syncKeepsServiceWhenPackageResolutionUnchanged() throws Exception {
        final InetAddress addr = InetAddressUtils.addr(IP);
        final PollableService svc = m_network.createService(NODE_ID, "node1", null, addr, SVC, 42);
        svc.setSchedule(mock(Schedule.class));
        assertFalse(svc.isDeleted());

        m_pollerConfig.addPackage("default");
        m_pollerConfig.getPackage("default").addSpecific(IP);
        m_pollerConfig.addService(SVC, 300000, mock(ServiceMonitor.class));
        final Package pkg = m_pollerConfig.getPackage("default");
        m_pollerConfig.setFindPackageForServiceResult(pkg);

        m_poller.syncNodeServicesToPackageResolution(NODE_ID, "node1", null, genericSyncEvent());

        assertFalse(svc.isDeleted());
    }

    @Test
    public void syncMarksNotPolledBeforeUnschedulingWhenNoPackage() throws Exception {
        final InetAddress addr = InetAddressUtils.addr(IP);
        final PollableService svc = m_network.createService(NODE_ID, "node1", null, addr, SVC, 42);
        svc.setSchedule(mock(Schedule.class));
        m_pollerConfig.setFindPackageForServiceResult(null);

        final org.mockito.InOrder order = inOrder(m_queryManager);
        m_poller.syncNodeServicesToPackageResolution(NODE_ID, "node1", null, categoryChangeEvent());
        order.verify(m_queryManager).updateServiceStatus(NODE_ID, IP, SVC, "N");
        assertTrue(svc.isDeleted());
    }

    @Test
    public void categorySyncDoesNotScheduleWhenStalePackageAndNoPollable() throws Exception {
        m_pollerConfig.addPackage("ipo-default");
        m_pollerConfig.getPackage("ipo-default").addSpecific(IP);
        m_pollerConfig.addService(SVC, 300000, mock(ServiceMonitor.class));
        m_pollerConfig.setFindPackageForServiceResult(m_pollerConfig.getPackage("ipo-default"));

        m_poller.syncNodeServicesToPackageResolution(NODE_ID, "node1", null, categoryChangeEvent());

        verify(m_queryManager, never()).updateServiceStatus(NODE_ID, IP, SVC, "A");
        verify(m_queryManager, never()).updateServiceStatus(NODE_ID, IP, SVC, "N");
    }

    @Test
    public void categorySyncReschedulesPollableWithoutEmbeddedPackageName() throws Exception {
        final InetAddress addr = InetAddressUtils.addr(IP);
        final PollableService svc = m_network.createService(NODE_ID, "node1", null, addr, SVC, 42);
        svc.setSchedule(mock(Schedule.class));
        assertFalse(svc.isDeleted());

        m_pollerConfig.addPackage("ipo-default");
        m_pollerConfig.getPackage("ipo-default").addSpecific(IP);
        m_pollerConfig.addService(SVC, 300000, mock(ServiceMonitor.class));
        m_pollerConfig.setFindPackageForServiceResult(m_pollerConfig.getPackage("ipo-default"));

        m_poller.syncNodeServicesToPackageResolution(NODE_ID, "node1", null, categoryChangeEvent());

        assertTrue(svc.isDeleted());
    }

    @Test
    public void keepsServiceWhenStillPolled() throws Exception {
        final InetAddress addr = InetAddressUtils.addr(IP);
        final PollableService svc = m_network.createService(NODE_ID, "node1", null, addr, SVC, 42);
        svc.setSchedule(mock(Schedule.class));
        assertFalse(svc.isDeleted());

        m_pollerConfig.addPackage("default");
        m_pollerConfig.getPackage("default").addSpecific(IP);
        m_pollerConfig.addService(SVC, 300000, mock(ServiceMonitor.class));

        m_poller.reconcileNode(NODE_ID, "node1", null, categoryChangeEvent(), false);

        assertFalse(svc.isDeleted());
    }

    private static ImmutableEvent categoryChangeEvent() {
        return ImmutableEvent.newBuilder()
                .setUei("uei.opennms.org/nodes/nodeCategoryMembershipChanged")
                .setSource("test")
                .setNodeid((long) NODE_ID)
                .setTime(new Date())
                .build();
    }

    private static ImmutableEvent genericSyncEvent() {
        return ImmutableEvent.newBuilder()
                .setUei("uei.opennms.org/nodes/nodeUpdated")
                .setSource("test")
                .setNodeid((long) NODE_ID)
                .setTime(new Date())
                .build();
    }
}
