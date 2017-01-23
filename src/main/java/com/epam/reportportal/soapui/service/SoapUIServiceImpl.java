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
package com.epam.reportportal.soapui.service;

import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.guice.ListenerPropertyValue;
import com.epam.reportportal.listeners.ListenersUtils;
import com.epam.reportportal.listeners.ReportPortalListenerContext;
import com.epam.reportportal.service.IReportPortalService;
import com.epam.reportportal.soapui.injection.SoapUIInjector;
import com.epam.reportportal.soapui.parameters.TestItemType;
import com.epam.reportportal.soapui.parameters.TestStatus;
import com.epam.reportportal.soapui.parameters.TestStepType;
import com.epam.reportportal.utils.TagsParser;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestRunner.Status;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.model.testsuite.TestStepResult.TestStepStatus;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.model.testsuite.TestSuiteRunner;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

/**
 * Default implementation of {@link SoapUIServiceImpl}
 *
 * @author Raman_Usik
 */
public class SoapUIServiceImpl implements SoapUIService {

    private static final String LINE_SEPARATOR = StandardSystemProperty.LINE_SEPARATOR.value();
    private final Logger logger = LoggerFactory.getLogger(SoapUIServiceImpl.class);
    private static final String ID = "id";

    @Inject
    @Named("soapClientService")
    private IReportPortalService serviceClient;

    @Inject
    private SoapUIContext soapUIContext;

    @Inject
    @ListenerPropertyValue(ListenerProperty.LAUNCH_TAGS)
    @Nullable
    private String tags;

    @Inject
    @ListenerPropertyValue(ListenerProperty.MODE)
    @Nullable
    private String mode;

    private Injector injector;

    @Inject
    public SoapUIServiceImpl(TestPropertyHolder contextProperties) {
        this.injector = SoapUIInjector.newOne(contextProperties);
        injector.injectMembers(this);
    }

    @Override
    public void startLaunch() {
        StartLaunchRQ rq = new StartLaunchRQ();
        rq.setName(soapUIContext.getLaunchName());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setTags(TagsParser.parseAsSet(tags));
        rq.setMode(mode == null ? null : Mode.valueOf(mode));
        EntryCreatedRS rs = null;
        try {
            rs = serviceClient.startLaunch(rq);
        } catch (Exception e) {
            ListenersUtils
                    .handleException(e, logger, "Unable start the launch: '" + soapUIContext.getLaunchName() + "'");
        }
        if (rs != null) {
            soapUIContext.setLaunchId(rs.getId());
            soapUIContext.setLaunchFailed(false);
        }
    }

    @Override
    public void finishLaunch() {
        FinishExecutionRQ rq = new FinishExecutionRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        if (soapUIContext.isTestCanceled()) {
            rq.setStatus(TestStatus.FAILED.getResult());
        } else {
            rq.setStatus(
                    soapUIContext.isLaunchFailed() ? TestStatus.FAILED.getResult() : TestStatus.FINISHED.getResult());
        }
        try {
            serviceClient.finishLaunch(soapUIContext.getLaunchId(), rq);
        } catch (Exception e) {
            ListenersUtils
                    .handleException(e, logger, "Unable finish the launch: '" + soapUIContext.getLaunchId() + "'");
        }
    }

    @Override
    public void startTestSuite(TestSuite testSuite) {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setLaunchId(soapUIContext.getLaunchId());
        rq.setName(testSuite.getName());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType(TestItemType.TEST_SUITE.getValue());
        EntryCreatedRS rs = null;
        try {
            rs = serviceClient.startRootTestItem(rq);
        } catch (Exception e) {
            ListenersUtils.handleException(e, logger, "Unable start test suite: '" + testSuite.getName() + "'");
        }
        if (rs != null) {
            testSuite.setPropertyValue(ID, rs.getId());
        }

    }

    @Override
    public void finishTestSuite(TestSuiteRunner testSuiteContext) {
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(TestStatus.fromSoapUI(testSuiteContext.getStatus()));
        if (testSuiteContext.getStatus().equals(Status.FAILED)) {
            soapUIContext.setLaunchFailed(true);
        }
        try {
            serviceClient.finishTestItem(testSuiteContext.getTestSuite().getPropertyValue(ID), rq);
        } catch (Exception e) {
            ListenersUtils.handleException(e, logger,
                    "Unable finish test suite: '" + String.valueOf(testSuiteContext.getTestSuite().getPropertyValue(ID))
                            + "'");
        }
    }

    @Override
    public void startTestCase(TestCase testCase) {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(testCase.getName());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setLaunchId(soapUIContext.getLaunchId());
        rq.setType(TestItemType.TEST_CASE.getValue());
        EntryCreatedRS rs = null;
        try {
            final String parentId = testCase.getTestSuite().getPropertyValue(ID);
            rs = serviceClient.startTestItem(Strings.isNullOrEmpty(parentId) ? null : parentId, rq);
        } catch (Exception e) {
            SoapUI.log("Error during starting test case:\n" + e.getMessage() + "\n" + Throwables
                    .getStackTraceAsString(e));
            ListenersUtils.handleException(e, logger, "Unable start test: '" + testCase.getName() + "'");
        }
        if (rs != null) {
            testCase.setPropertyValue(ID, rs.getId());
        }
    }

    @Override
    public void finishTestCase(TestCaseRunner testCaseContext) {
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(TestStatus.fromSoapUI(testCaseContext.getStatus()));
        try {
            serviceClient.finishTestItem(testCaseContext.getTestCase().getPropertyValue(ID), rq);
        } catch (Exception e) {
            SoapUI.log("Error during finishing test case:\n" + e.getMessage() + "\n" + Throwables
                    .getStackTraceAsString(e));
            ListenersUtils.handleException(e, logger,
                    "Unable finish test: '" + testCaseContext.getTestCase().getPropertyValue(ID) + "'");
        }

    }

    @Override
    public void startTestStep(TestStep testStep) {
        if (testStep.getPropertyValue(ID) != null) {
            return;
        }
        soapUIContext.setTestCanceled(false);
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(testStep.getName());
        rq.setLaunchId(soapUIContext.getLaunchId());
        rq.setDescription(TestStepType.getStepType(testStep.getClass()));
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType(TestItemType.TEST_STEP.getValue());
        EntryCreatedRS rs = null;
        try {
            rs = serviceClient.startTestItem(testStep.getTestCase().getPropertyValue(ID), rq);
        } catch (Exception e) {
            SoapUI.log("Error during starting step:\n" + e.getMessage());
            ListenersUtils.handleException(e, logger, "Unable start test method: '" + testStep.getName() + "'");
        }
        if (rs != null) {
            ReportPortalListenerContext.setRunningNowItemId(rs.getId());
        }
    }

    @Override
    public void finishTestStep(TestStepResult testStepContext) {
        String testId = ReportPortalListenerContext.getRunningNowItemId();
        try {
            final StringWriter logData = new StringWriter();
            PrintWriter logWriter = new PrintWriter(logData);
            testStepContext.writeTo(logWriter);

            final String logString = logData.toString();
            if (!Strings.isNullOrEmpty(logString)) {
                SaveLogRQ logRQ = new SaveLogRQ();
                logRQ.setLevel("INFO");
                logRQ.setTestItemId(testId);
                logRQ.setLogTime(Calendar.getInstance().getTime());

                logRQ.setMessage(logString);
                serviceClient.log(logRQ);
            }

            if (TestStepStatus.FAILED.equals(
                    testStepContext.getStatus())) {
                logStepError(testStepContext);
            }

            FinishTestItemRQ rq = new FinishTestItemRQ();
            rq.setEndTime(Calendar.getInstance().getTime());
            if (TestStepStatus.CANCELED.equals(testStepContext.getStatus())) {
                soapUIContext.setTestCanceled(true);
            }
            rq.setStatus(TestStatus.fromSoapUIStep(testStepContext.getStatus()));

            serviceClient.finishTestItem(testId, rq);
        } catch (Exception e) {
            SoapUI.log("Error during finishing step:\n" + e.getMessage() + "\n" + Throwables
                    .getStackTraceAsString(e));
            ListenersUtils.handleException(e, logger, "Unable finish test method: '" + testId + "'");
        } finally {
            ReportPortalListenerContext.setRunningNowItemId(null);
        }
    }

    private void logStepError(TestStepResult testStepContext) {
        SaveLogRQ slrq = new SaveLogRQ();
        slrq.setTestItemId(ReportPortalListenerContext.getRunningNowItemId());
        slrq.setLevel("ERROR");
        slrq.setLogTime(Calendar.getInstance().getTime());
        String message;
        if (testStepContext.getError() != null) {
            message = "Exception: " + testStepContext.getError().getMessage() + LINE_SEPARATOR
                    + this.getStackTraceContext(testStepContext.getError());
        } else {
            StringBuilder messages = new StringBuilder();
            for (String messageLog : testStepContext.getMessages()) {
                messages.append(messageLog);
                messages.append(LINE_SEPARATOR);
            }
            message = messages.toString();
        }
        // log.info(message);
        slrq.setLogTime(Calendar.getInstance().getTime());
        slrq.setMessage(message);
        try {
            serviceClient.log(slrq);
        } catch (Exception e1) {
            SoapUI.log("Unable to send log to ReportPortal:\n" + e1.getMessage() + "\n" + Throwables
                    .getStackTraceAsString(e1));
            ListenersUtils.handleException(e1, logger, "Unable to send message to Report Portal");
        }
    }

    private String getStackTraceContext(Throwable e) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < e.getStackTrace().length; i++) {
            result.append(e.getStackTrace()[i]);
            result.append(LINE_SEPARATOR);
        }
        return result.toString();
    }

    @Override
    public IReportPortalService getServiceClient() {
        return serviceClient;
    }

    @Override
    public Injector getInjector() {
        return this.injector;
    }

}
