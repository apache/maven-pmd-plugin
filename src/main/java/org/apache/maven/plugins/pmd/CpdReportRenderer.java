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

import javax.swing.text.html.HTML.Attribute;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
import org.apache.maven.plugins.pmd.model.CpdFile;
import org.apache.maven.plugins.pmd.model.Duplication;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Class that generated the CPD report.
 *
 * @author mperham
 * @version $Id$
 */
public class CpdReportRenderer extends AbstractMavenReportRenderer {
    private final I18N i18n;

    private final Locale locale;

    private final Map<File, PmdFileInfo> files;

    private final Collection<Duplication> duplications;

    private final boolean aggregate;

    public CpdReportRenderer(
            Sink sink,
            I18N i18n,
            Locale locale,
            Map<File, PmdFileInfo> files,
            Collection<Duplication> duplications,
            boolean aggregate) {
        super(sink);
        this.i18n = i18n;
        this.locale = locale;
        this.files = files;
        this.duplications = duplications;
        this.aggregate = aggregate;
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
        return i18n.getString("cpd-report", locale, "report.cpd." + key);
    }

    @Override
    protected void renderBody() {
        startSection(getTitle());

        sink.paragraph();
        sink.text(getI18nString("cpdlink") + " ");
        link("https://pmd.github.io/latest/pmd_userdocs_cpd.html", "CPD");
        sink.text(" " + AbstractPmdReport.getPmdVersion() + ".");
        sink.paragraph_();

        // TODO overall summary

        if (!duplications.isEmpty()) {
            renderDuplications();
        } else {
            paragraph(getI18nString("noProblems"));
        }

        // TODO files summary

        endSection();
    }

    /**
     * Method that generates a line of CPD report according to a TokenEntry.
     */
    private void generateFileLine(CpdFile duplicationMark) {
        // Get information for report generation
        String filename = duplicationMark.getPath();
        File file = new File(filename);
        PmdFileInfo fileInfo = files.get(file);
        File sourceDirectory = fileInfo.getSourceDirectory();
        filename = StringUtils.substring(
                filename, sourceDirectory.getAbsolutePath().length() + 1);
        String xrefLocation = fileInfo.getXrefLocation();
        MavenProject projectFile = fileInfo.getProject();
        int line = duplicationMark.getLine();

        sink.tableRow();
        tableCell(filename);
        if (aggregate) {
            tableCell(projectFile.getName());
        }
        sink.tableCell();

        if (xrefLocation != null) {
            sink.link(xrefLocation + "/"
                    + filename.replaceAll("\\.java$", ".html").replace('\\', '/') + "#L" + line);
        }
        sink.text(String.valueOf(line));
        if (xrefLocation != null) {
            sink.link_();
        }

        sink.tableCell_();
        sink.tableRow_();
    }

    private void renderDuplications() {
        startSection(getI18nString("dupes"));

        for (Duplication duplication : duplications) {
            String code = duplication.getCodefragment();

            startTable();
            sink.tableRow();
            tableHeaderCell(getI18nString("column.file"));
            if (aggregate) {
                tableHeaderCell(getI18nString("column.project"));
            }
            tableHeaderCell(getI18nString("column.line"));
            sink.tableRow_();

            // Iterating on every token entry
            for (CpdFile mark : duplication.getFiles()) {
                generateFileLine(mark);
            }

            // Source snippet
            sink.tableRow();

            int colspan = 2;
            if (aggregate) {
                colspan = 3;
            }
            SinkEventAttributes att = new SinkEventAttributeSet();
            att.addAttribute(Attribute.COLSPAN, colspan);
            sink.tableCell(att);
            verbatimText(code);
            sink.tableCell_();
            sink.tableRow_();
            endTable();
        }

        endSection();
    }
}
