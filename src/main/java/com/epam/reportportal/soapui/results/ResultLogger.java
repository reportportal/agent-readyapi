package com.epam.reportportal.soapui.results;

import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.eviware.soapui.model.testsuite.TestStepResult;

import java.util.List;

/**
 * @author Andrei Varabyeu
 */
public abstract class ResultLogger<T> {

    private Class<T> resultsType;

    public ResultLogger(Class<T> resultsType) {
        this.resultsType = resultsType;
    }

    abstract protected List<SaveLogRQ> buildLogs(String testId, T result);

    public final List<SaveLogRQ> buildLogs(String testId, TestStepResult result) {
        //noinspection unchecked
        return buildLogs(testId, (T) result);
    }

    public boolean supports(TestStepResult result) {
        return resultsType.isAssignableFrom(result.getClass());
    }

}
