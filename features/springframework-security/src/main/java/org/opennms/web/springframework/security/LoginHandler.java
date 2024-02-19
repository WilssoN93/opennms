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
package org.opennms.web.springframework.security;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;

import org.opennms.netmgt.config.api.UserConfig;
import org.springframework.security.core.GrantedAuthority;

public interface LoginHandler {

    public CallbackHandler callbackHandler();
    public UserConfig userConfig();
    public SpringSecurityUserDao springSecurityUserDao();

    public Set<Principal> createPrincipals(final GrantedAuthority authority);
    public String user();
    public void setUser(final String user);

    public Set<Principal> principals();
    public void setPrincipals(final Set<Principal> principals);

}
