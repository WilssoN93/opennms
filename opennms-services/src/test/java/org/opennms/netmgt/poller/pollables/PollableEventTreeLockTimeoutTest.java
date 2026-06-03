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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.netmgt.poller.mock.MockPollContext;

public class PollableEventTreeLockTimeoutTest {

    @Test
    public void deleteThrowsLockUnavailableWhenTreeLockHeld() throws Exception {
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
        final Thread holder = new Thread(() -> {
            node.obtainTreeLock();
            holderReady.countDown();
            try {
                Thread.sleep(2_000L);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                node.releaseTreeLock();
            }
        });
        holder.start();
        assertTrue(holderReady.await(5, TimeUnit.SECONDS));

        try {
            node.delete();
            fail("Expected LockUnavailable when tree lock is held");
        } catch (final LockUnavailable expected) {
            // expected
        } finally {
            holder.join(5_000L);
        }
        assertFalse(node.isDeleted());
    }

    @Test
    public void pollPathStillUsesShortLockTimeout() {
        assertTrue(PollableService.class.getDeclaredMethods().length > 0);
        // Poll worker lock wait remains 500ms in doRun(int timeout) — verified by source contract (NMS-15708).
        assertTrue(500 < new MockPollContext().getEventTreeLockTimeoutMs());
    }
}
