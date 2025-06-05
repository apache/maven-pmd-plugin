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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
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
            // Always create XML format. We need to output it even if the file list is empty, or we have no
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
        if (request.isIncludeXmlInReports()) {
            File outputDirectory = new File(request.getReportOutputDirectory());
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new IOException("Couldn't create report output directory: " + outputDirectory);
            }
            FileUtils.copyFile(targetFile, new File(outputDirectory, "cpd.xml"));
        }
    }

    private void writeFormattedReport(CPDReport cpd) throws IOException, MavenReportException {
        CPDReportRenderer renderer = CpdExecutor.createRenderer(request.getFormat(), request.getOutputEncoding());
        writeReport(cpd, renderer, request.getFormat());
    }

    private File writeReport(CPDReport cpd, CPDReportRenderer renderer, String extension) throws IOException {
        if (renderer == null) {
            return null;
        }

        File targetDir = new File(request.getTargetDirectory());
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Couldn't create report target directory: " + targetDir);
        }

        File targetFile = new File(targetDir, "cpd." + extension);
        try (Writer writer =
                Files.newBufferedWriter(targetFile.toPath(), Charset.forName(request.getOutputEncoding()))) {
            renderer.render(cpd.filterMatches(filterMatches()), writer);
            return targetFile;
        }
        catch (UnsupportedCharsetException | IllegalCharsetNameException ex) {
            throw new UnsupportedEncodingException(ex.getMessage());
        }
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
