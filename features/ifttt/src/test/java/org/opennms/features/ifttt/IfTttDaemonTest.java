/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.ifttt;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.mate.api.ContextKey;
import org.opennms.core.mate.api.EntityScopeProvider;
import org.opennms.core.mate.api.MapScope;
import org.opennms.core.mate.api.Scope;
import org.opennms.core.rpc.mock.MockEntityScopeProvider;
import org.opennms.core.test.MockLogAppender;
import org.opennms.features.ifttt.config.IfTttConfig;
import org.opennms.features.ifttt.helper.DefaultVariableNameExpansion;
import org.opennms.features.ifttt.helper.IfTttTrigger;
import org.opennms.features.ifttt.helper.VariableNameExpansion;
import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionOperations;

public class IfTttDaemonTest {
    private static final Logger LOG = LoggerFactory.getLogger(IfTttDaemonTest.class);

    private class ResultEntry {
        private String oldSeverity = "null", newSeverity = "null";
        private int oldCount = 0, newCount = 0;
        private String event;

        public ResultEntry(final String event, final VariableNameExpansion variableNameExpansion) {
            this.event = event;
            if (variableNameExpansion instanceof DefaultVariableNameExpansion) {
                this.oldCount = Integer.valueOf(variableNameExpansion.replace("%oc%"));
                this.newCount = Integer.valueOf(variableNameExpansion.replace("%nc%"));
                this.oldSeverity = variableNameExpansion.replace("%os%");
                this.newSeverity = variableNameExpansion.replace("%ns%");
            }
        }

        public ResultEntry(final String event, final String oldSeverity, final Integer oldCount, final String newSeverity, final Integer newCount) {
            this.event = event;
            this.oldSeverity = oldSeverity;
            this.newSeverity = newSeverity;
            this.oldCount = oldCount;
            this.newCount = newCount;
        }

        @Override
        public String toString() {
            return String.format("%s : %s/%d -> %s/%d", event, oldSeverity, oldCount, newSeverity, newCount);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof ResultEntry)) return false;

            ResultEntry that = (ResultEntry) o;

            return Objects.equals(oldCount, that.oldCount) &&
                    Objects.equals(newCount, that.newCount) &&
                    Objects.equals(oldSeverity, that.oldSeverity) &&
                    Objects.equals(newSeverity, that.newSeverity) &&
                    Objects.equals(event, that.event);
        }

        @Override
        public int hashCode() {
            return Objects.hash(oldSeverity, newSeverity, oldCount, newCount, event);
        }
    }

    private Map<Integer, OnmsNode> nodeMap;
    private Map<Integer, OnmsAlarm> alarmMap;
    private Map<String, OnmsCategory> categoryMap;

    private void addCategory(final String category) {
        categoryMap.put(category, new OnmsCategory(category));
    }

    private void addNode(final Integer id, final String label, final String... categories) {
        final OnmsNode onmsNode = new OnmsNode();
        onmsNode.setId(id);
        onmsNode.setLabel(label);
        onmsNode.setCategories(Arrays.stream(categories).map(c -> categoryMap.get(c)).collect(Collectors.toSet()));
        nodeMap.put(id, onmsNode);
    }

    private void addAlarm(final Integer id, final Integer nodeId, final String reductionKey, final OnmsSeverity onmsSeverity, final boolean acknowledged) {
        final OnmsAlarm onmsAlarm = new OnmsAlarm(id, "dummy.uei", null, null, onmsSeverity.getId(), new Date(), new OnmsEvent()) {
            @Override
            public boolean isAcknowledged() {
                return acknowledged;
            }
        };
        onmsAlarm.setNode(nodeMap.get(nodeId));
        onmsAlarm.setReductionKey(reductionKey);
        alarmMap.put(id, onmsAlarm);
    }

    @Before
    public void setup() {
        MockLogAppender.setupLogging();

        categoryMap = new HashMap<>();
        addCategory("Foo");
        addCategory("Bar");
        addCategory("Xyz");

        nodeMap = new HashMap<>();
        addNode(1, "Node1", "Foo");
        addNode(2, "Node2", "Foo", "Bar");
        addNode(3, "Node3", "Bar");
        addNode(4, "Node4", "Foo", "Bar");
        addNode(5, "Node5", "Bar");
        addNode(6, "Node6", "Xyz");

        alarmMap = new HashMap<>();

        addAlarm(1, 1, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.NORMAL, false);
        addAlarm(2, 2, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.INDETERMINATE, false);
        addAlarm(3, 3, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.MINOR, false);

        addAlarm(4, 4, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.WARNING, false);
        addAlarm(5, 5, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.CLEARED, false);
        addAlarm(6, 6, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.CRITICAL, false);

        addAlarm(7, 1, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.MINOR, false);
        addAlarm(8, 2, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.WARNING, false);
        addAlarm(9, 3, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.MINOR, false);

        addAlarm(10, null, null, OnmsSeverity.CRITICAL, false);

        addAlarm(11, 2, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.CRITICAL, true);

        addAlarm(12, null, EventConstants.BUSINESS_SERVICE_PROBLEM_UEI, OnmsSeverity.MINOR, false);
        addAlarm(13, null, EventConstants.BUSINESS_SERVICE_PROBLEM_UEI, OnmsSeverity.CRITICAL, false);
        addAlarm(14, null, null, OnmsSeverity.CRITICAL, false);
        addAlarm(15, null, EventConstants.BUSINESS_SERVICE_PROBLEM_UEI, OnmsSeverity.MAJOR, false);
    }

    @Test
    public void ifTttDaemonTest() throws Exception {
        final Map<String, List<ResultEntry>> resultEntries = runIfTttDaemonTest(4, 4);

        List<ResultEntry> foo = resultEntries.get("Foo / uei.opennms.org/nodes/nodeLost.*");
        List<ResultEntry> bar = resultEntries.get("Bar / uei.opennms.org/nodes/nodeLostService");
        List<ResultEntry> foobar = resultEntries.get("Foo|Bar / uei.opennms.org/nodes/node.*");
        List<ResultEntry> bsm1 = resultEntries.get("Foo|Bar / uei.opennms.org/bsm/serviceProblem");
        List<ResultEntry> bsm2 = resultEntries.get(" / uei.opennms.org/bsm/serviceProb.*");

        assertEquals(new ResultEntry("ON", "null", 0, "null", 0), foo.get(0));
        assertEquals(new ResultEntry("MINOR", "INDETERMINATE", 0, "MINOR", 5), foo.get(1));
        assertEquals(new ResultEntry("MAJOR", "MINOR", 5, "MAJOR", 6), foo.get(2));
        assertEquals(new ResultEntry("OFF", "null", 0, "null", 0), foo.get(3));

        assertEquals(new ResultEntry("ON", "null", 0, "null", 0), bar.get(0));
        assertEquals(new ResultEntry("CRITICAL", "INDETERMINATE", 0, "CRITICAL", 7), bar.get(1));
        assertEquals(new ResultEntry("CRITICAL", "CRITICAL", 7, "CRITICAL", 8), bar.get(2));
        assertEquals(new ResultEntry("OFF", "null", 0, "null", 0), bar.get(3));

        assertEquals(new ResultEntry("ON", "null", 0, "null", 0), foobar.get(0));
        assertEquals(new ResultEntry("MINOR", "INDETERMINATE", 0, "MINOR", 8), foobar.get(1));
        assertEquals(new ResultEntry("MAJOR", "MINOR", 8, "MAJOR", 9), foobar.get(2));
        assertEquals(new ResultEntry("OFF", "null", 0, "null", 0), foobar.get(3));

        assertEquals(new ResultEntry("ON", "null", 0, "null", 0), bsm1.get(0));
        assertEquals(new ResultEntry("NORMAL", "INDETERMINATE", 0, "NORMAL", 0), bsm1.get(1));
        assertEquals(new ResultEntry("MAJOR", "NORMAL", 0, "MAJOR", 1), bsm1.get(2));
        assertEquals(new ResultEntry("OFF", "null", 0, "null", 0), bsm1.get(3));

        assertEquals(new ResultEntry("ON", "null", 0, "null", 0), bsm2.get(0));
        assertEquals(new ResultEntry("CRITICAL", "INDETERMINATE", 0, "CRITICAL", 3), bsm2.get(1));
        assertEquals(new ResultEntry("CRITICAL", "CRITICAL", 3, "CRITICAL", 4), bsm2.get(2));
        assertEquals(new ResultEntry("OFF", "null", 0, "null", 0), bsm2.get(3));
    }

    public Map<String, List<ResultEntry>> runIfTttDaemonTest(final int timeout, final int entryCount) throws Exception {
        final AlarmDao alarmDao = mock(AlarmDao.class);
        when(alarmDao.findMatching((Criteria) any())).thenReturn(new ArrayList<>(alarmMap.values()));

        final TransactionOperations transactionOperations = mock(TransactionOperations.class);
        when(transactionOperations.execute(any())).thenAnswer((Answer<Void>) invocationOnMock -> {
            TransactionCallbackWithoutResult transactionCallbackWithoutResult = invocationOnMock.getArgument(0);
            transactionCallbackWithoutResult.doInTransaction(null);
            return null;
        });

        final Map<String, List<ResultEntry>> receivedEntries = new HashMap<>();

        final IfTttDaemon ifTttDaemon = new IfTttDaemon(alarmDao, transactionOperations, new MockEntityScopeProvider(), new File("src/test/resources/etc/ifttt-config.xml")) {
            @Override
            protected void fireIfTttTriggerSet(IfTttConfig ifTttConfig, String filterKey, String name, VariableNameExpansion variableNameExpansion) {
                if (!receivedEntries.containsKey(filterKey)) {
                    receivedEntries.put(filterKey, new ArrayList<>());
                }
                receivedEntries.get(filterKey).add(new ResultEntry(name, variableNameExpansion));
            }
        };

        ifTttDaemon.start();

        await().atMost(timeout, SECONDS).until(() -> allEntrySizesMatch(receivedEntries, entryCount - 2));
        LOG.debug("#1: {}", receivedEntries);

        addAlarm(100, 4, EventConstants.NODE_LOST_SERVICE_EVENT_UEI, OnmsSeverity.MAJOR, false);
        addAlarm(101, 4, "uei.opennms.org/bsm/serviceProblem", OnmsSeverity.MAJOR, false);
        when(alarmDao.findMatching((Criteria) any())).thenReturn(new ArrayList<>(alarmMap.values()));

        await().atMost(timeout, SECONDS).until(() -> allEntrySizesMatch(receivedEntries, entryCount - 1));
        LOG.debug("#2: {}", receivedEntries);

        ifTttDaemon.stop();

        await().atMost(timeout, SECONDS).until(() -> allEntrySizesMatch(receivedEntries, entryCount));
        LOG.debug("#3: {}", receivedEntries);

        return receivedEntries;
    }

    private boolean allEntrySizesMatch(final Map<String, List<ResultEntry>> entries, final int expectedSize) {
        for (final Map.Entry<String, List<ResultEntry>> entry : entries.entrySet()) {
            if (entry.getValue().size() != expectedSize) {
                return false;
            }
        }

        return true;
    }

    @Test
    public void testMetadataInterpolation() {
        final AlarmDao alarmDao = mock(AlarmDao.class);
        when(alarmDao.findMatching((Criteria) any())).thenReturn(new ArrayList<>(alarmMap.values()));

        final TransactionOperations transactionOperations = mock(TransactionOperations.class);
        when(transactionOperations.execute(any())).thenAnswer((Answer<Void>) invocationOnMock -> {
            TransactionCallbackWithoutResult transactionCallbackWithoutResult = invocationOnMock.getArgument(0);
            transactionCallbackWithoutResult.doInTransaction(null);
            return null;
        });

        final List<String> apiKey = new ArrayList<>();

        mockConstruction(IfTttTrigger.class, (ifTttTrigger, context) -> {
            doAnswer(invocationOnMock -> {
                apiKey.add(invocationOnMock.getArgument(0));
                return invocationOnMock.getMock();
            }).when(ifTttTrigger).key(any());
            when(ifTttTrigger.event(any())).thenReturn(ifTttTrigger);
            when(ifTttTrigger.value1(any())).thenReturn(ifTttTrigger);
            when(ifTttTrigger.value2(any())).thenReturn(ifTttTrigger);
            when(ifTttTrigger.value3(any())).thenReturn(ifTttTrigger);
        });

        final Map<ContextKey, String> scopeMap = new TreeMap();
        scopeMap.put(new ContextKey("scv", "ifttt:password"), "the-secret-api-key");

        final EntityScopeProvider entityScopeProvider = mock(EntityScopeProvider.class);
        when(entityScopeProvider.getScopeForScv()).thenReturn(new MapScope(Scope.ScopeName.DEFAULT, scopeMap));

        final IfTttDaemon ifTttDaemon = new IfTttDaemon(alarmDao, transactionOperations, entityScopeProvider, new File("src/test/resources/etc/ifttt-config.xml"));

        ifTttDaemon.fireIfTttTriggerSet(
                ifTttDaemon.getFileReloadContainer().getObject(),
                "Bar / uei.opennms.org/nodes/nodeLostService",
                "CRITICAL",
                new DefaultVariableNameExpansion(OnmsSeverity.CLEARED, OnmsSeverity.CLEARED, 1, 1)
        );

        assertEquals(1, apiKey.size());
        assertEquals("the-secret-api-key", apiKey.get(0));
    }
}
