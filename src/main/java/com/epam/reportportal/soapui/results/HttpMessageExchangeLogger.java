package com.epam.reportportal.soapui.results;

import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.eviware.soapui.impl.wsdl.submit.HttpMessageExchange;
import com.eviware.soapui.impl.wsdl.submit.transports.http.HttpResponse;
import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * @author Andrei Varabyeu
 */
public class HttpMessageExchangeLogger extends ResultLogger<HttpMessageExchange> {

    public HttpMessageExchangeLogger() {
        super(HttpMessageExchange.class);
    }

    @Override
    public List<SaveLogRQ> buildLogs(String testId, HttpMessageExchange result) {
        final HttpResponse testRS = (HttpResponse) result.getResponse();
        return Arrays.asList(
                logEntity(testId, testRS.getRequestHeaders().toString(), testRS.getRequestContent()),
                logEntity(testId, testRS.getResponseHeaders().toString(), testRS.getContentAsString()));
    }

    private SaveLogRQ logEntity(String testId, String headers, String body) {
        SaveLogRQ rqRQ = new SaveLogRQ();
        rqRQ.setLevel("INFO");
        rqRQ.setTestItemId(testId);
        rqRQ.setLogTime(Calendar.getInstance().getTime());
        StringBuilder rqLog = new StringBuilder();
        rqLog
                .append("HEADERS:\n")
                .append(headers);

        if (!Strings.isNullOrEmpty(body)) {
            rqLog.append("BODY:\n")
                    .append(body);
        }

        rqRQ.setMessage(rqLog.toString());
        return rqRQ;

    }

}
