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
package com.epam.reportportal.soapui.listeners;

import com.epam.reportportal.soapui.service.SoapUIService;
import com.epam.reportportal.soapui.service.SoapUIServiceImpl;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestRunListener;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.plugins.ListenerConfiguration;

import static com.epam.reportportal.soapui.listeners.RPProjectRunListener.RP_SERVICE;

/**
 * Report portal related implementation of {@link TestRunListener}.
 * This listener should be used only with {@link RPTestSuiteRunListener} and   {@link RPProjectRunListener}.
 *
 * @author Raman_Usik
 * @author Andrei Varabyeu
 */
@ListenerConfiguration
public class RPTestRunListener implements TestRunListener {

    private static final String TEST_ONLY = "test_only";
    private SoapUIService service;

    public void beforeRun(TestCaseRunner runner,
            TestCaseRunContext context) {
        service = (SoapUIService) context.getProperty(RP_SERVICE);
        if (null == service) {
            try {
                service = new SoapUIServiceImpl(context.getTestCase().getTestSuite().getProject());
                service.startLaunch();

                service.startTestCase(context.getTestCase());
            } catch (Throwable t) {
                SoapUI.log("ReportPortal plugin cannot be initialized. " + t.getMessage());
                service = SoapUIService.NOP_SERVICE;
            }
            context.setProperty(TEST_ONLY, true);
            context.setProperty(RP_SERVICE, service);
        }
    }

    @Override
    public void afterRun(TestCaseRunner runner,
            TestCaseRunContext context) {
        final Object testOnly = context.getProperty(TEST_ONLY);
        if (null != testOnly && ((Boolean) testOnly)) {
            final int stepsCount = context.getTestCase().getTestStepList().size();
            //all steps are passed
            if (runner.getResultCount() == stepsCount) {
                service.finishTestCase(runner);
                service.finishLaunch();
            }

        }
    }

    @Override
    public void beforeStep(TestCaseRunner paramTestCaseRunner,
            TestCaseRunContext context, TestStep testStep) {
        service.startTestStep(testStep);
    }

    @Override
    public void afterStep(TestCaseRunner paramTestCaseRunner,
            TestCaseRunContext paramTestCaseRunContext,
            TestStepResult paramTestStepResult) {
        service.finishTestStep(paramTestStepResult);
    }

    @Override
    @Deprecated
    public void beforeStep(TestCaseRunner paramTestCaseRunner,
            TestCaseRunContext paramTestCaseRunContext) {
    }

}
