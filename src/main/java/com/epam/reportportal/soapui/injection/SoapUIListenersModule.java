/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-soapui
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.soapui.injection;

import com.epam.reportportal.guice.ListenerPropertyBinder;
import com.epam.reportportal.guice.ListenerPropertyValue;
import com.epam.reportportal.guice.ReportPortalClientModule;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.restclient.endpoint.RestEndpoint;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.epam.reportportal.service.IReportPortalService;
import com.epam.reportportal.soapui.results.HttpMessageExchangeLogger;
import com.epam.reportportal.soapui.results.ResultLogger;
import com.epam.reportportal.soapui.service.SoapUIContext;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.SoapUIPropertiesHolder;
import com.eviware.soapui.model.TestPropertyHolder;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Guice module with soapUI client beans.
 *
 * @author Raman_Usik
 */

class SoapUIListenersModule extends AbstractModule {

    private Properties properties;

    SoapUIListenersModule(TestPropertyHolder contextProperties) {
        this.properties = convertProperties(contextProperties);

        Map<String, String> propertiesMap = new HashMap<>();
        for (Map.Entry<Object, Object> prop : this.properties.entrySet()) {
            propertiesMap.put(prop.getKey().toString(), prop.getValue().toString());
        }
        SoapUIPropertiesHolder.setSoapUIProperties(propertiesMap);
    }

    @Override
    protected void configure() {
        binder().bind(ListenerParameters.class)
                .toInstance(new ListenerParameters(properties));

        Names.bindProperties(binder(), properties);
        for (final ListenerProperty listenerProperty : ListenerProperty.values()) {
            binder().bind(Key.get(String.class, ListenerPropertyBinder
                    .named(listenerProperty))).toProvider(new Provider<String>() {
                @Override
                public String get() {
                    return properties.getProperty(listenerProperty.getPropertyName());
                }
            });
        }
    }

    /**
     * Provide particularly initialized soapUI context
     *
     * @param parameters ReportPortal listener parameters
     * @return SoapUIContext
     */
    @Provides
    public SoapUIContext provideSoapUIContext(ListenerParameters parameters) {
        SoapUIContext soapUIContext = new SoapUIContext();
        soapUIContext.setLaunchName(parameters.getLaunchName());
        return soapUIContext;
    }

    /*
     * In SoapUI context this bean should be prototype because in each launch
     * run properties can be reloaded so new service should be build
     */
    @Provides
    @Named("soapClientService")
    public IReportPortalService provideJUnitStyleService(RestEndpoint restEndpoint,
            @ListenerPropertyValue(ListenerProperty.PROJECT_NAME) String project,
            @Nullable @ListenerPropertyValue(ListenerProperty.BATCH_SIZE_LOGS) String batchLogsSize) {
        return new BatchedReportPortalService(restEndpoint, ReportPortalClientModule.API_BASE, project,
                Strings.isNullOrEmpty(batchLogsSize) ? 1 : Integer.parseInt(batchLogsSize));
    }

    @Provides
    @Named("resultLoggers")
    @Singleton
    public List<ResultLogger<?>> provideResultLoggers() {
        return Collections.<ResultLogger<?>>singletonList(new HttpMessageExchangeLogger());
    }

    private Properties convertProperties(TestPropertyHolder params) {
        Properties properties = new Properties();
        for (String key : params.getPropertyNames()) {
            final String value = params.getPropertyValue(key);
            if (null != value) {
                properties.put(key, value);
            }

        }
        return properties;
    }

}
