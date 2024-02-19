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
package org.opennms.web.filter;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * <p>Abstract BetweenFilter class.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
public abstract class BetweenFilter<T> extends MultiArgFilter<T> {
    
    /**
     * <p>Constructor for BetweenFilter.</p>
     *
     * @param filterType a {@link java.lang.String} object.
     * @param type a {@link org.opennms.web.filter.SQLType} object.
     * @param fieldName a {@link java.lang.String} object.
     * @param propertyName a {@link java.lang.String} object.
     * @param first a T object.
     * @param last a T object.
     * @param <T> a T object.
     */
    public BetweenFilter(String filterType, SQLType<T> type, String fieldName, String propertyName, T first, T last) {
        super(filterType, type, fieldName, propertyName, type.createArray(first, last));
    }

    /**
     * <p>getFirst</p>
     *
     * @return a T object.
     */
    public T getFirst() { return getValues()[0]; }
    /**
     * <p>getLast</p>
     *
     * @return a T object.
     */
    public T getLast() { return getValues()[1]; }
    
    /** {@inheritDoc} */
    @Override
    public Criterion getCriterion() {
        return Restrictions.between(getPropertyName(), getFirst(), getLast());
    }
    
    /** {@inheritDoc} */
    @Override
    public String getSQLTemplate() {
        return " "+getSQLFieldName() + " BETWEEN %s AND %s ";
    }

    
}
