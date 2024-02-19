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
package org.opennms.protocols.xml.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Class Parameter.
 * <ul>
 *   <li>get-disable-ssl-verification</li>
 *   <li>protocol-version</li>
 *   <li>retries</li>
 *   <li>timeout</li>
 * </ul>
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
@XmlRootElement(name="parameter")
@XmlAccessorType(XmlAccessType.FIELD)
public class Parameter implements Cloneable {

    /** The name. */
    @XmlAttribute(required=true)
    private String name;

    /** The value. */
    @XmlAttribute(required=true)
    private String value;

    /**
     * Instantiates a new parameter.
     */
    public Parameter() {}

    /**
     * Instantiates a new parameter.
     *
     * @param name the name
     * @param value the value
     */
    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Parameter(Parameter copy) {
        name = copy.name;
        value = copy.value;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the value.
     *
     * @param value the new value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name + "=" + value;
    }

    @Override
    public Parameter clone() {
        return new Parameter(this);
    }
}
