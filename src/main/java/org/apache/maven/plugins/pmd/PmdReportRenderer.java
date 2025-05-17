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
package org.apache.maven.plugins.pmd;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.pmd.lang.rule.RulePriority;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.pmd.model.ProcessingError;
import org.apache.maven.plugins.pmd.model.SuppressedViolation;
import org.apache.maven.plugins.pmd.model.Violation;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Render the PMD violations into Doxia events.
 *
 * @author Brett Porter
 * @version $Id$
 */
public class PmdReportRenderer extends AbstractMavenReportRenderer {
    private final Log log;

    private final I18N i18n;

    private final Locale locale;

    private final Map<File, PmdFileInfo> files;

    // TODO Should not share state
    private String currentFilename;

    private final Collection<Violation> violations;

    private boolean renderRuleViolationPriority;

    private final boolean renderViolationsByPriority;

    private final boolean aggregate;

    private Collection<SuppressedViolation> suppressedViolations = new ArrayList<>();

    private Collection<ProcessingError> processingErrors = new ArrayList<>();

    public PmdReportRenderer(
            Log log,
            Sink sink,
            I18N i18n,
            Locale locale,
            Map<File, PmdFileInfo> files,
            Collection<Violation> violations,
            boolean renderRuleViolationPriority,
            boolean renderViolationsByPriority,
            boolean aggregate) {
        super(sink);
        this.log = log;
        this.i18n = i18n;
        this.locale = locale;
        this.files = files;
        this.violations = violations;
        this.renderRuleViolationPriority = renderRuleViolationPriority;
        this.renderViolationsByPriority = renderViolationsByPriority;
        this.aggregate = aggregate;
    }

    public void setSuppressedViolations(Collection<SuppressedViolation> suppressedViolations) {
        this.suppressedViolations = suppressedViolations;
    }

    public void setProcessingErrors(Collection<ProcessingError> processingErrors) {
        this.processingErrors = processingErrors;
    }

    @Override
    public String getTitle() {
        return getI18nString("title");
    }

    /**
     * @param key The key.
     * @return The translated string.
     */
    private String getI18nString(String key) {
        return i18n.getString("pmd-report", locale, "report.pmd." + key);
    }

    protected void renderBody() {
        startSection(getTitle());

        sink.paragraph();
        sink.text(getI18nString("pmdlink") + " ");
        link("https://pmd.github.io", "PMD");
        sink.text(" " + AbstractPmdReport.getPmdVersion() + ".");
        sink.paragraph_();

        if (!violations.isEmpty()) {
            renderViolationsByPriority();

            renderViolations();
        } else {
            paragraph(getI18nString("noProblems"));
        }

        renderSuppressedViolations();

        renderProcessingErrors();

        endSection();
    }

    private void startFileSection(String currentFilename, PmdFileInfo fileInfo) {
        // prepare the filename
        this.currentFilename = shortenFilename(currentFilename, fileInfo);

        startSection(makeFileSectionName(this.currentFilename, fileInfo));

        startTable();
        sink.tableRow();
        tableHeaderCell(getI18nString("column.rule"));
        tableHeaderCell(getI18nString("column.violation"));
        if (this.renderRuleViolationPriority) {
            tableHeaderCell(getI18nString("column.priority"));
        }
        tableHeaderCell(getI18nString("column.line"));
        sink.tableRow_();
    }

    private void endFileSection() {
        endTable();
        endSection();
    }

    private void addRuleName(Violation ruleViolation) {
        boolean hasUrl = StringUtils.isNotBlank(ruleViolation.getExternalInfoUrl());

        if (hasUrl) {
            sink.link(ruleViolation.getExternalInfoUrl());
        }

        sink.text(ruleViolation.getRule());

        if (hasUrl) {
            sink.link_();
        }
    }

    private void renderSingleRuleViolation(Violation ruleViolation, PmdFileInfo fileInfo) {
        sink.tableRow();
        sink.tableCell();
        addRuleName(ruleViolation);
        sink.tableCell_();
        // May contain content not legit for #tableCell()
        sink.tableCell();
        sink.text(ruleViolation.getText());
        sink.tableCell_();

        if (this.renderRuleViolationPriority) {
            tableCell(String.valueOf(
                    RulePriority.valueOf(ruleViolation.getPriority()).getPriority()));
        }

        sink.tableCell();

        int beginLine = ruleViolation.getBeginline();
        outputLineLink(beginLine, fileInfo);
        int endLine = ruleViolation.getEndline();
        if (endLine != beginLine) {
            sink.text("&#x2013;"); // \u2013 is a medium long dash character
            outputLineLink(endLine, fileInfo);
        }

        sink.tableCell_();
        sink.tableRow_();
    }

    // PMD might run the analysis multi-threaded, so the violations might be reported
    // out of order. We sort them here by filename and line number before writing them to
    // the report.
    private void renderViolations() {
        startSection(getI18nString("files"));

        // TODO files summary
        renderViolationsTable(violations);

        endSection();
    }

    private void renderViolationsByPriority() {
        if (!renderViolationsByPriority) {
            return;
        }

        boolean oldPriorityColumn = this.renderRuleViolationPriority;
        this.renderRuleViolationPriority = false;

        startSection(getI18nString("violationsByPriority"));

        Map<RulePriority, List<Violation>> violationsByPriority = new HashMap<>();
        for (Violation violation : violations) {
            RulePriority priority = RulePriority.valueOf(violation.getPriority());
            List<Violation> violationSegment = violationsByPriority.get(priority);
            if (violationSegment == null) {
                violationSegment = new ArrayList<>();
                violationsByPriority.put(priority, violationSegment);
            }
            violationSegment.add(violation);
        }

        for (RulePriority priority : RulePriority.values()) {
            List<Violation> violationsWithPriority = violationsByPriority.get(priority);
            if (violationsWithPriority == null || violationsWithPriority.isEmpty()) {
                continue;
            }

            startSection(getI18nString("priority") + " " + priority.getPriority());

            renderViolationsTable(violationsWithPriority);

            endSection();
        }

        if (violations.isEmpty()) {
            paragraph(getI18nString("noProblems"));
        }

        endSection();

        this.renderRuleViolationPriority = oldPriorityColumn;
    }

    private void renderViolationsTable(Collection<Violation> violationSegment) {
        List<Violation> violationSegmentCopy = new ArrayList<>(violationSegment);
        Collections.sort(violationSegmentCopy, new Comparator<Violation>() {
            /** {@inheritDoc} */
            public int compare(Violation o1, Violation o2) {
                int filenames = o1.getFileName().compareTo(o2.getFileName());
                if (filenames == 0) {
                    return o1.getBeginline() - o2.getBeginline();
                } else {
                    return filenames;
                }
            }
        });

        boolean fileSectionStarted = false;
        String previousFilename = null;
        for (Violation ruleViolation : violationSegmentCopy) {
            String currentFn = ruleViolation.getFileName();
            PmdFileInfo fileInfo = determineFileInfo(currentFn);

            if (!currentFn.equalsIgnoreCase(previousFilename) && fileSectionStarted) {
                endFileSection();
                fileSectionStarted = false;
            }
            if (!fileSectionStarted) {
                startFileSection(currentFn, fileInfo);
                fileSectionStarted = true;
            }

            renderSingleRuleViolation(ruleViolation, fileInfo);

            previousFilename = currentFn;
        }

        if (fileSectionStarted) {
            endFileSection();
        }
        logExcludeFromFailureFileSuppressions();
    }

    private void logExcludeFromFailureFileSuppressions() {
        log.info("logExcludeFromFailureFileSuppressions");
        log.info(violations.stream()
                .map(violation -> String.format("%s:%s", violation.getViolationClass(), violation.getRule()))
                .toString());
    }

    private void outputLineLink(int line, PmdFileInfo fileInfo) {
        String xrefLocation = null;
        if (fileInfo != null) {
            xrefLocation = fileInfo.getXrefLocation();
        }

        if (xrefLocation != null) {
            sink.link(xrefLocation + "/" + currentFilename.replaceAll("\\.java$", ".html") + "#L" + line);
        }
        sink.text(String.valueOf(line));
        if (xrefLocation != null) {
            sink.link_();
        }
    }

    // PMD might run the analysis multi-threaded, so the suppressed violations might be reported
    // out of order. We sort them here by filename before writing them to
    // the report.
    private void renderSuppressedViolations() {
        if (suppressedViolations.isEmpty()) {
            return;
        }

        startSection(getI18nString("suppressedViolations.title"));

        List<SuppressedViolation> suppressedViolationsCopy = new ArrayList<>(suppressedViolations);
        Collections.sort(suppressedViolationsCopy, new Comparator<SuppressedViolation>() {
            @Override
            public int compare(SuppressedViolation o1, SuppressedViolation o2) {
                return o1.getFilename().compareTo(o2.getFilename());
            }
        });

        startTable();
        tableHeader(new String[] {
            getI18nString("suppressedViolations.column.filename"),
            getI18nString("suppressedViolations.column.ruleMessage"),
            getI18nString("suppressedViolations.column.suppressionType"),
            getI18nString("suppressedViolations.column.userMessage")
        });

        for (SuppressedViolation suppressedViolation : suppressedViolationsCopy) {
            String filename = suppressedViolation.getFilename();
            PmdFileInfo fileInfo = determineFileInfo(filename);
            filename = shortenFilename(filename, fileInfo);

            // May contain content not legit for #tableCell()
            sink.tableRow();
            tableCell(filename);
            sink.tableCell();
            sink.text(suppressedViolation.getRuleMessage());
            sink.tableCell_();
            tableCell(suppressedViolation.getSuppressionType());
            sink.tableCell();
            sink.text(suppressedViolation.getUserMessage());
            sink.tableCell_();
            sink.tableRow_();
        }

        endTable();
        endSection();
    }

    private void renderProcessingErrors() {
        if (processingErrors.isEmpty()) {
            return;
        }

        // sort the problem by filename first, since PMD is executed multi-threaded
        // and might reports the results unsorted
        List<ProcessingError> processingErrorsCopy = new ArrayList<>(processingErrors);
        Collections.sort(processingErrorsCopy, new Comparator<ProcessingError>() {
            @Override
            public int compare(ProcessingError e1, ProcessingError e2) {
                return e1.getFilename().compareTo(e2.getFilename());
            }
        });

        startSection(getI18nString("processingErrors.title"));

        startTable();
        tableHeader(new String[] {
            getI18nString("processingErrors.column.filename"), getI18nString("processingErrors.column.problem")
        });

        for (ProcessingError error : processingErrorsCopy) {
            renderSingleProcessingError(error);
        }

        endTable();
        endSection();
    }

    private void renderSingleProcessingError(ProcessingError error) {
        String filename = error.getFilename();
        PmdFileInfo fileInfo = determineFileInfo(filename);
        filename = makeFileSectionName(shortenFilename(filename, fileInfo), fileInfo);

        sink.tableRow();
        tableCell(filename);
        sink.tableCell();
        sink.text(error.getMsg());
        sink.verbatim(null);
        sink.rawText(error.getDetail());
        sink.verbatim_();
        sink.tableCell_();
        sink.tableRow_();
    }

    private String shortenFilename(String filename, PmdFileInfo fileInfo) {
        String result = filename;
        if (fileInfo != null && fileInfo.getSourceDirectory() != null) {
            result = StringUtils.substring(
                    result, fileInfo.getSourceDirectory().getAbsolutePath().length() + 1);
        }
        return StringUtils.replace(result, "\\", "/");
    }

    private String makeFileSectionName(String filename, PmdFileInfo fileInfo) {
        if (aggregate && fileInfo != null && fileInfo.getProject() != null) {
            return fileInfo.getProject().getName() + " - " + filename;
        }
        return filename;
    }

    private PmdFileInfo determineFileInfo(String filename) {
        try {
            File canonicalFilename = new File(filename).getCanonicalFile();
            PmdFileInfo fileInfo = files.get(canonicalFilename);
            if (fileInfo == null) {
                log.warn("Couldn't determine PmdFileInfo for file " + filename + " (canonical: " + canonicalFilename
                        + "). XRef links won't be available.");
            }
            return fileInfo;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
