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
package org.opennms.netmgt.dao.hibernate;

import java.util.List;

import org.opennms.netmgt.dao.api.RequisitionedCategoryAssociationDao;
import org.opennms.netmgt.model.RequisitionedCategoryAssociation;
import org.springframework.util.Assert;

public class RequisitionedCategoryAssociationDaoHibernate extends AbstractDaoHibernate<RequisitionedCategoryAssociation, Integer> implements RequisitionedCategoryAssociationDao {

    public RequisitionedCategoryAssociationDaoHibernate() {
        super(RequisitionedCategoryAssociation.class);
    }

    @Override
    public List<RequisitionedCategoryAssociation> findByNodeId(final Integer nodeId) {
        Assert.notNull(nodeId, "nodeId cannot be null");
        return find("from RequisitionedCategoryAssociation as r where r.node.id = ?", nodeId);
    }

}
