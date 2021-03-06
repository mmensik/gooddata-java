/*
 * Copyright (C) 2004-2017, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.report;

import com.gooddata.AbstractGoodDataIT;
import com.gooddata.gdc.UriResponse;
import com.gooddata.md.report.Report;
import com.gooddata.md.report.ReportDefinition;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.gooddata.util.ResourceUtils.OBJECT_MAPPER;
import static com.gooddata.util.ResourceUtils.readFromResource;
import static com.gooddata.util.ResourceUtils.readObjectFromResource;
import static net.jadler.Jadler.onRequest;
import static net.jadler.Jadler.port;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ReportServiceIT extends AbstractGoodDataIT {

    private static final String URI = ReportService.EXPORTING_URI + "/123";
    private static final String RESPONSE = "abc";

    @BeforeMethod
    public void setUp() throws IOException {
        onRequest()
                .havingPathEqualTo(ReportRequest.URI)
                .havingMethodEqualTo("POST")
            .respond()
                .withBody("{}");
        onRequest()
                .havingPathEqualTo(ReportService.EXPORTING_URI)
                .havingMethodEqualTo("POST")
            .respond()
                .withStatus(202)
                .withBody(OBJECT_MAPPER.writeValueAsString(new UriResponse("http://localhost:" + port() + URI)));
        onRequest()
                .havingPathEqualTo(URI)
                .havingMethodEqualTo("GET")
            .respond()
                .withStatus(202)
            .thenRespond()
                .withStatus(200)
                .withBody(RESPONSE)
        ;
    }

    @Test
    public void shouldExportReportDefinition() throws Exception {
        final ReportDefinition rd = readObjectFromResource("/md/report/gridReportDefinition.json", ReportDefinition.class);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        gd.getReportService().exportReport(rd, ReportExportFormat.CSV, output).get();
        assertThat(output.toString(StandardCharsets.US_ASCII.name()), is(RESPONSE));
    }

    @Test
    public void shouldExportReport() throws Exception {
        final Report rd = readObjectFromResource("/md/report/report.json", Report.class);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        gd.getReportService().exportReport(rd, ReportExportFormat.CSV, output).get();
        assertThat(output.toString(StandardCharsets.US_ASCII.name()), is(RESPONSE));
    }

    @Test(expectedExceptions = ReportException.class, expectedExceptionsMessageRegExp = "Unable to export report")
    public void shouldFail() throws Exception {
        onRequest()
                .havingPathEqualTo(URI)
                .havingMethodEqualTo("GET")
                .respond()
                .withStatus(400);

        final Report rd = readObjectFromResource("/md/report/report.json", Report.class);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        gd.getReportService().exportReport(rd, ReportExportFormat.CSV, output).get();
    }

    @Test(expectedExceptions = NoDataReportException.class, expectedExceptionsMessageRegExp = "Report contains no data")
    public void shouldFailNoData() throws Exception {
        onRequest()
                .havingPathEqualTo(URI)
                .havingMethodEqualTo("GET")
                .respond()
                .withStatus(204);

        final Report rd = readObjectFromResource("/md/report/report.json", Report.class);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        gd.getReportService().exportReport(rd, ReportExportFormat.CSV, output).get();
    }
}