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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.mock.MockPollContext;

public class PollablePollLockScopeTest {

    @Test
    public void deleteCompletesWhileRemotePollBlocksOutsideTreeLock() throws Exception {
        final MockPollContext context = new MockPollContext() {
            @Override
            public long getEventTreeLockTimeoutMs() {
                return 2_000L;
            }
        };
        context.setNodeProcessingEnabled(true);

        final PollableNetwork network = new PollableNetwork(context);
        final PollableNode node = new PollableNode(network, 1, "node1", null);
        network.addMember(node);
        final PollableInterface iface = new PollableInterface(node, java.net.InetAddress.getByName("192.168.1.1"));
        node.addMember(iface);
        final PollableService svc = new PollableService(iface, "ICMP", 1);

        final CountDownLatch pollRpcEntered = new CountDownLatch(1);
        final CountDownLatch allowPollRpcComplete = new CountDownLatch(1);
        final CountDownLatch deleteFinished = new CountDownLatch(1);

        final PollableServiceConfig pollConfig = mock(PollableServiceConfig.class);
        when(pollConfig.getCurrentTime()).thenReturn(0L);
        when(pollConfig.poll()).thenAnswer(invocation -> {
            pollRpcEntered.countDown();
            assertTrue(allowPollRpcComplete.await(10, TimeUnit.SECONDS));
            return PollStatus.up();
        });
        when(pollConfig.asyncPoll()).thenReturn(CompletableFuture.completedFuture(PollStatus.up()));
        svc.setPollConfig(pollConfig);

        final Thread pollThread = new Thread(svc::doRun);
        pollThread.start();

        assertTrue(pollRpcEntered.await(5, TimeUnit.SECONDS));

        final Thread deleteThread = new Thread(() -> {
            try {
                svc.delete();
            } catch (final LockUnavailable e) {
                fail("Unexpected LockUnavailable: " + e.getMessage());
            } finally {
                deleteFinished.countDown();
            }
        });
        deleteThread.start();

        assertTrue(deleteFinished.await(5, TimeUnit.SECONDS));
        assertTrue(svc.isDeleted());

        allowPollRpcComplete.countDown();
        pollThread.join(10_000L);
    }

    @Test
    public void deleteThrowsLockUnavailableWhenTreeLockHeldForAdminWork() throws Exception {
        final MockPollContext context = new MockPollContext() {
            @Override
            public long getEventTreeLockTimeoutMs() {
                return 100L;
            }
        };
        final PollableNetwork network = new PollableNetwork(context);
        final PollableNode node = new PollableNode(network, 1, "node1", null);
        network.addMember(node);

        final CountDownLatch holderReady = new CountDownLatch(1);
        final AtomicBoolean holderRunning = new AtomicBoolean(true);
        final Thread holder = new Thread(() -> {
            node.obtainTreeLock();
            holderReady.countDown();
            while (holderRunning.get()) {
                try {
                    Thread.sleep(50);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            node.releaseTreeLock();
        });
        holder.start();
        assertTrue(holderReady.await(5, TimeUnit.SECONDS));

        try {
            node.delete();
            fail("Expected LockUnavailable when tree lock is held");
        } catch (final LockUnavailable expected) {
            // expected
        } finally {
            holderRunning.set(false);
            holder.join(5_000L);
        }
        assertFalse(node.isDeleted());
    }
}
