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
package org.opennms.netmgt.provision.service;

/**
 * <p>ScanProgress interface.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public interface ScanProgress {
    
    /**
     * <p>abort</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void abort(String message);
    /**
     * <p>isAborted</p>
     *
     * @return a boolean.
     */
    public boolean isAborted();

    /**
     * Record a non-fatal failure during a node scan (e.g. SNMP timeout). The scan may continue.
     * Default implementation is a no-op for callers that do not track partial failures.
     *
     * @param provisionTask short name of the step that failed
     * @param message failure detail
     */
    default void failTask(String provisionTask, String message) {
        // no-op
    }

    /**
     * @return number of non-fatal task failures recorded for this scan
     */
    default int getFailedTasksCount() {
        return 0;
    }

    /**
     * @return true if {@link #failTask} was called at least once
     */
    default boolean hasFailedTasks() {
        return false;
    }
}
