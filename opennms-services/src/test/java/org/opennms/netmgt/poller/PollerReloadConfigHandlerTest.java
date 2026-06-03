/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance under the License.  You may obtain a copy of the
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.opennms.netmgt.config.PollerConfig;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.EventIpcManager;
import org.opennms.netmgt.events.api.model.ImmutableMapper;
import org.opennms.netmgt.model.events.EventBuilder;

public class PollerReloadConfigHandlerTest {

    @Test
    public void reloadDaemonConfigRefreshesPackagesWithoutGlobalReconcile() throws Exception {
        final Poller poller = mock(Poller.class);
        final PollerConfig pollerConfig = mock(PollerConfig.class);
        final EventIpcManager eventManager = mock(EventIpcManager.class);

        when(poller.getPollerConfig()).thenReturn(pollerConfig);
        when(poller.getEventManager()).thenReturn(eventManager);

        final PollerEventProcessor processor = new PollerEventProcessor(poller);

        final EventBuilder builder = new EventBuilder(EventConstants.RELOAD_DAEMON_CONFIG_UEI, "test");
        builder.addParam(EventConstants.PARM_DAEMON_NAME, "Pollerd");

        processor.onEvent(ImmutableMapper.fromMutableEvent(builder.getEvent()));

        verify(pollerConfig).update();
        verify(pollerConfig).rebuildPackageIpListMap();
        verify(poller).refreshServicePackages();
        verify(poller, never()).reconcileNode(anyInt(), any(), any(), any(), anyBoolean());
        verify(poller, never()).reconcileNode(anyInt(), any(), any(), any(), eq(true));
    }
}
