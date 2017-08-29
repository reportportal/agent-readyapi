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

import com.epam.reportportal.guice.ConfigurationModule;
import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.guice.ReportPortalClientModule;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.testsuite.TestProperty;
import com.google.inject.Guice;
import com.google.inject.util.Modules;

import java.util.Map;

/**
 * Provide soapui client oriented guice injector. Provided injector is a child
 * injector of rp client injector with soapui's specific modules.
 *
 * @author Aliaksei_Makayed
 * @author Andrei Varabyeu
 */
public final class SoapUIInjector {

    private SoapUIInjector() {
        //no need to create new injector
    }

    public static Injector newOne(TestPropertyHolder contextProperties) {
        return Injector.create(new SoapUIListenersModule(contextProperties), new ReportPortalClientModule());

    }

}
