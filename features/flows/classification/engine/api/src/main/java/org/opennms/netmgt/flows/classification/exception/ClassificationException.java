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
package org.opennms.netmgt.flows.classification.exception;

import java.util.Objects;

import org.opennms.netmgt.flows.classification.error.Error;
import org.opennms.netmgt.flows.classification.error.ErrorTemplate;

// Generic Exception related to classifications
public class ClassificationException extends RuntimeException {

    // The user-friendly error message
    private final Error error;

    public ClassificationException(String context, ErrorTemplate template, Object... arguments) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(template);
        this.error = new Error(context, template, arguments);
    }

    public Error getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return error.getFormattedMessage();
    }
}
