/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.reporting.core.svclayer.support;


import org.opennms.api.reporting.ReportService;
import org.opennms.features.reporting.repository.global.GlobalReportRepository;
import org.opennms.reporting.core.svclayer.ReportServiceLocator;
import org.opennms.reporting.core.svclayer.ReportServiceLocatorException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>DefaultReportServiceLocator class.</p>
 */
public class DefaultReportServiceLocator implements ApplicationContextAware, ReportServiceLocator {

    private ApplicationContext m_applicationContext;

    private GlobalReportRepository m_globalReportRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public ReportService getReportService(final String reportServiceName) throws ReportServiceLocatorException {
        try {
            return m_applicationContext.getBean(reportServiceName, ReportService.class);
        } catch (final BeansException e) {
            throw new ReportServiceLocatorException("cannot locate report service bean: " + reportServiceName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReportService getReportServiceForId(String reportId)
            throws ReportServiceLocatorException {

        return getReportService(m_globalReportRepository.getReportService(reportId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        m_applicationContext = applicationContext;
    }

    public void setGlobalReportRepository(GlobalReportRepository globalReportRepository) {
        m_globalReportRepository = globalReportRepository;
    }
}
