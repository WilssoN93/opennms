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
package org.opennms.core.snmp.profile.mapper.impl;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.config.snmp.SnmpProfile;
import org.opennms.netmgt.filter.api.FilterDao;
import org.opennms.netmgt.filter.api.FilterParseException;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpProfileMapper;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.proxy.LocationAwareSnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;

public class SnmpProfileMapperImpl implements SnmpProfileMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SnmpProfileMapperImpl.class);

    @Autowired
    private SnmpAgentConfigFactory agentConfigFactory;

    @Autowired
    private FilterDao filterDao;

    @Autowired
    private LocationAwareSnmpClient locationAwareSnmpClient;

    private static final String SYS_OBJECTID_INSTANCE = ".1.3.6.1.2.1.1.2.0";

    private final SnmpObjId snmpObjId = SnmpObjId.get(SYS_OBJECTID_INSTANCE);

    public SnmpProfileMapperImpl() {
    }

    public SnmpProfileMapperImpl(FilterDao filterDao, SnmpAgentConfigFactory agentConfigFactory, LocationAwareSnmpClient locationAwareSnmpClient) {
        this.agentConfigFactory = Objects.requireNonNull(agentConfigFactory);
        this.filterDao = Objects.requireNonNull(filterDao);
        this.locationAwareSnmpClient = Objects.requireNonNull(locationAwareSnmpClient);
    }

    @Override
    public CompletableFuture<Optional<SnmpAgentConfig>> getAgentConfigFromProfiles(InetAddress inetAddress, String location, String oid, boolean metaDataInterpolation) {

        CompletableFuture<Optional<SnmpAgentConfig>> future = new CompletableFuture<>();
        List<SnmpProfile> snmpProfiles = agentConfigFactory.getProfiles();
        // Get matching profiles for this IpAddress.
        List<SnmpProfile> matchedProfiles = snmpProfiles.stream()
                .filter(snmpProfile -> isFilterExpressionValid(inetAddress, snmpProfile.getFilterExpression()))
                .collect(Collectors.toList());
        // Run and collect futures.
        List<CompletableFuture<Optional<SnmpAgentConfig>>> futures = matchedProfiles.stream()
                .map(matchedProfile -> fitProfile(matchedProfile, inetAddress, location, oid))
                .collect(Collectors.toList());
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        //Join all the results.
        CompletableFuture<List<Optional<SnmpAgentConfig>>> results = allFutures.thenApply(agentConfig -> {
            return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        });
        //Complete the future with first non-empty agent config.
        results.whenComplete((configList, throwable) -> {
            if (throwable == null) {
                Optional<Optional<SnmpAgentConfig>> configOptional = configList.stream().filter(Optional::isPresent).findFirst();
                future.complete(configOptional.orElse(Optional.empty()));
            } else {
                future.complete(Optional.empty());
            }
        });
        return future;
    }

    private CompletableFuture<Optional<SnmpAgentConfig>> fitProfile(SnmpProfile snmpProfile, InetAddress inetAddress, String location, String oid) {

        SnmpObjId snmpObjectId = this.snmpObjId;
        //Get agent config from profile.
        final SnmpAgentConfig interpolatedAgentConfig = agentConfigFactory.getAgentConfigFromProfile(snmpProfile, inetAddress);
        final SnmpAgentConfig agentConfig = agentConfigFactory.getAgentConfigFromProfile(snmpProfile, inetAddress, false);
        //If OID is specified, get snmp object for that OID.
        if (!Strings.isNullOrEmpty(oid)) {
            snmpObjectId = SnmpObjId.get(oid);
        }
        CompletableFuture<Optional<SnmpAgentConfig>> future = new CompletableFuture<>();
        CompletableFuture<SnmpValue> snmpResult = locationAwareSnmpClient.get(interpolatedAgentConfig, snmpObjectId)
                .withLocation(location)
                .withDescription("Snmp-Profile:" + snmpProfile.getLabel())
                .execute();
        //Logging purposes
        final String objectId = Strings.isNullOrEmpty(oid) ? SYS_OBJECTID_INSTANCE : oid;
        snmpResult.whenComplete(((snmpValue, throwable) -> {
            if (throwable == null) {
                if (snmpValue != null && !snmpValue.isError()) {
                    future.complete(Optional.of(agentConfig));
                } else {
                    future.complete(Optional.empty());
                }
            } else {
                LOG.info("Exception while doing SNMP get on OID '{}' with profile '{}'", objectId, snmpProfile.getLabel());
                future.complete(Optional.empty());
            }
        }));
        return future;
    }


    @Override
    public CompletableFuture<Optional<SnmpAgentConfig>> getAgentConfigFromProfiles(InetAddress inetAddress, String location, boolean metaDataInterpolation) {
        return getAgentConfigFromProfiles(inetAddress, location, null, metaDataInterpolation);
    }

    @Override
    public CompletableFuture<Optional<SnmpAgentConfig>> fitProfile(String profileLabel, InetAddress inetAddress, String location, String oid) {

        Optional<SnmpAgentConfig> agentConfig = Optional.empty();
        if (Strings.isNullOrEmpty(profileLabel)) {
            return getAgentConfigFromProfiles(inetAddress, location, oid);
        }
        List<SnmpProfile> profiles = agentConfigFactory.getProfiles();
        Optional<SnmpProfile> matchingProfile = profiles.stream()
                .filter(profile -> profile.getLabel().equals(profileLabel))
                .findFirst();
        if (matchingProfile.isPresent()) {
            return fitProfile(matchingProfile.get(), inetAddress, location, oid);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private boolean isFilterExpressionValid(InetAddress inetAddress, String filterExpression) {
        // Consider profiles without filter expression or an empty one as matching.
        if (Strings.isNullOrEmpty(filterExpression)) {
            return true;
        } else {
            try {
                return filterDao.isValid(inetAddress.getHostAddress(), filterExpression);
            } catch (FilterParseException e) {
                LOG.warn("Filter expression '{}' is invalid. ", filterExpression, e);
            }
        }
        return false;
    }

    public void setLocationAwareSnmpClient(LocationAwareSnmpClient locationAwareSnmpClient) {
        this.locationAwareSnmpClient = locationAwareSnmpClient;
    }

    public SnmpAgentConfigFactory getAgentConfigFactory() {
        return agentConfigFactory;
    }

    public void setAgentConfigFactory(SnmpAgentConfigFactory agentConfigFactory) {
        this.agentConfigFactory = agentConfigFactory;
    }

    public FilterDao getFilterDao() {
        return filterDao;
    }

    public void setFilterDao(FilterDao filterDao) {
        this.filterDao = filterDao;
    }
}
