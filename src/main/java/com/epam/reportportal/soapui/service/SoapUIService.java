package com.epam.reportportal.soapui.service;

import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.service.IReportPortalService;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.model.testsuite.TestSuiteRunner;

/**
 * Wrapper around ReportPortal Client
 *
 * @author Andrei Varabyeu
 */
public interface SoapUIService {

    void startLaunch();

    void finishLaunch();

    void startTestSuite(TestSuite testSuite);

    void finishTestSuite(TestSuiteRunner testSuiteContext);

    void startTestCase(TestCase testCase);

    void finishTestCase(TestCaseRunner testCaseContext);

    void startTestStep(TestStep testStep);

    void finishTestStep(TestStepResult testStepContext);

    IReportPortalService getServiceClient();

    Injector getInjector();

    /**
     * NOP implementation for the cases when ReportPortal client cannot be initialized
     */
    SoapUIService NOP_SERVICE = new SoapUIService() {
        @Override
        public void startLaunch() {

        }

        @Override
        public void finishLaunch() {

        }

        @Override
        public void startTestSuite(TestSuite testSuite) {

        }

        @Override
        public void finishTestSuite(TestSuiteRunner testSuiteContext) {

        }

        @Override
        public void startTestCase(TestCase testCase) {

        }

        @Override
        public void finishTestCase(TestCaseRunner testCaseContext) {

        }

        @Override
        public void startTestStep(TestStep testStep) {

        }

        @Override
        public void finishTestStep(TestStepResult testStepContext) {

        }

        @Override
        public IReportPortalService getServiceClient() {
            return null;
        }

        @Override
        public Injector getInjector() {
            return null;
        }
    };
}
