/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.pmd.exec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.sourceforge.pmd.cpd.CPDReport;
import net.sourceforge.pmd.cpd.CPDReportRenderer;
import net.sourceforge.pmd.cpd.Match;
import net.sourceforge.pmd.cpd.XMLRenderer;
import org.apache.maven.plugins.pmd.ExcludeDuplicationsFromFile;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CpdReportConsumer implements Consumer<CPDReport> {
    private static final Logger LOG = LoggerFactory.getLogger(CpdReportConsumer.class);

    private final CpdRequest request;
    private final ExcludeDuplicationsFromFile excludeDuplicationsFromFile;

    CpdReportConsumer(CpdRequest request, ExcludeDuplicationsFromFile excludeDuplicationsFromFile) {
        this.request = request;
        this.excludeDuplicationsFromFile = excludeDuplicationsFromFile;
    }

    @Override
    public void accept(CPDReport report) {
        try {
            // always create XML format. we need to output it even if the file list is empty or we have no
            // duplications so that the "check" goals can check for violations
            writeXmlReport(report);

            // HTML format is handled by maven site report, XML format has already been rendered.
            // a renderer is only needed for other formats
            String format = request.getFormat();
            if (!"html".equals(format) && !"xml".equals(format)) {
                writeFormattedReport(report);
            }
        } catch (IOException | MavenReportException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeXmlReport(CPDReport cpd) throws IOException {
        File targetFile = writeReport(cpd, new XMLRenderer(request.getOutputEncoding()), "xml");
        if (request.isIncludeXmlInSite()) {
            File siteDir = new File(request.getReportOutputDirectory());
            if (!siteDir.exists() && !siteDir.mkdirs()) {
                throw new IOException("Couldn't create report output directory: " + siteDir);
            }
            FileUtils.copyFile(targetFile, new File(siteDir, "cpd.xml"));
        }
    }

    private void writeFormattedReport(CPDReport cpd) throws IOException, MavenReportException {
        CPDReportRenderer r = CpdExecutor.createRenderer(request.getFormat(), request.getOutputEncoding());
        writeReport(cpd, r, request.getFormat());
    }

    private File writeReport(CPDReport cpd, CPDReportRenderer renderer, String extension) throws IOException {
        if (renderer == null) {
            return null;
        }

        File targetDir = new File(request.getTargetDirectory());
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Couldn't create report output directory: " + targetDir);
        }

        File targetFile = new File(targetDir, "cpd." + extension);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(targetFile), request.getOutputEncoding())) {
            renderer.render(cpd.filterMatches(filterMatches()), writer);
            writer.flush();
        }
        return targetFile;
    }

    private Predicate<Match> filterMatches() {
        return (Match match) -> {
            LOG.debug(
                    "Filtering duplications. Using {} configured exclusions.",
                    excludeDuplicationsFromFile.countExclusions());

            if (excludeDuplicationsFromFile.isExcludedFromFailure(match)) {
                LOG.debug("Excluded {} duplications.", match);
                return false;
            } else {
                return true;
            }
        };
    }
}
