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
package org.opennms.netmgt.graph.api.generic;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class GenericElementTest {
	
	@Test
    public void ensureImmutabilty() {
    	Map<String, Object> properties = new HashMap<>();
    	properties.put(GenericProperties.NAMESPACE, "namespace");
    	GenericElement element = new GenericElement(properties) {};
    	properties.put("newKey", "newValue");
    	
    	// make sure we can't sneak a new element into the properties (size would be 2):
    	assertEquals(1, element.getProperties().size());
    }
}
