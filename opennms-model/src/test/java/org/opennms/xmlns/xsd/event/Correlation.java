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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.17 at 12:58:43 PM EDT 
//


package org.opennms.xmlns.xsd.event;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cuei" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="cmin" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="cmax" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ctime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="state" default="off">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="(on|off)"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="path" default="suppressDuplicates">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="(suppressDuplicates|cancellingEvent|suppressAndCancel|pathOutage)"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "cuei",
    "cmin",
    "cmax",
    "ctime"
})
@XmlRootElement(name = "correlation")
public class Correlation {

    protected List<String> cuei;
    protected String cmin;
    protected String cmax;
    protected String ctime;
    @XmlAttribute(name = "state")
    @Pattern(regexp="(on|off)")
    protected String state;
    @XmlAttribute(name = "path")
    @Pattern(regexp="(suppressDuplicates|cancellingEvent|suppressAndCancel|pathOutage)")
    protected String path;

    /**
     * Gets the value of the cuei property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cuei property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCuei().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getCuei() {
        if (cuei == null) {
            cuei = new ArrayList<>();
        }
        return this.cuei;
    }

    /**
     * Gets the value of the cmin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCmin() {
        return cmin;
    }

    /**
     * Sets the value of the cmin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCmin(String value) {
        this.cmin = value;
    }

    /**
     * Gets the value of the cmax property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCmax() {
        return cmax;
    }

    /**
     * Sets the value of the cmax property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCmax(String value) {
        this.cmax = value;
    }

    /**
     * Gets the value of the ctime property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCtime() {
        return ctime;
    }

    /**
     * Sets the value of the ctime property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCtime(String value) {
        this.ctime = value;
    }

    /**
     * Gets the value of the state property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getState() {
        if (state == null) {
            return "off";
        } else {
            return state;
        }
    }

    /**
     * Sets the value of the state property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setState(String value) {
        this.state = value;
    }

    /**
     * Gets the value of the path property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPath() {
        if (path == null) {
            return "suppressDuplicates";
        } else {
            return path;
        }
    }

    /**
     * Sets the value of the path property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPath(String value) {
        this.path = value;
    }

}
