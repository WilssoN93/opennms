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
package org.opennms.web.event.filter;

import org.opennms.web.filter.NotEqualOrNullFilter;
import org.opennms.web.filter.SQLType;

public class NegativeLocationFilter extends NotEqualOrNullFilter<String> {
    public static final String TYPE = "locationnot";
    private String m_location;

    public NegativeLocationFilter(String location) {
        super(TYPE, SQLType.STRING, "MONITORINGSYSTEMS.LOCATION", "distPoller.location", location);
        m_location = location;
    }

    @Override
    public String getTextDescription() {
        return ("Location is not " + m_location);
    }

    @Override
    public String toString() {
        return ("<WebEventRepository.NegativeLocationFilter: " + getDescription() + ">");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof NegativeLocationFilter)) return false;
        return (this.toString().equals(obj.toString()));
    }
}
