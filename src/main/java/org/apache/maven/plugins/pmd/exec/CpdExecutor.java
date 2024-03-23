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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Predicate;

import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.CPDReport;
import net.sourceforge.pmd.cpd.CPDReportRenderer;
import net.sourceforge.pmd.cpd.CSVRenderer;
import net.sourceforge.pmd.cpd.CpdAnalysis;
import net.sourceforge.pmd.cpd.Match;
import net.sourceforge.pmd.cpd.SimpleRenderer;
import net.sourceforge.pmd.cpd.XMLRenderer;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.ecmascript.EcmascriptLanguageModule;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.lang.jsp.JspLanguageModule;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.ExcludeDuplicationsFromFile;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes CPD with the configuration provided via {@link CpdRequest}.
 */
public class CpdExecutor extends Executor {
    private static final Logger LOG = LoggerFactory.getLogger(CpdExecutor.class);

    public static CpdResult execute(CpdRequest request) throws MavenReportException {
        if (request.getJavaExecutable() != null) {
            return fork(request);
        }

        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(CpdExecutor.class.getClassLoader());
            CpdExecutor cpdExecutor = new CpdExecutor(request);
            return cpdExecutor.run();
        } finally {
            Thread.currentThread().setContextClassLoader(origLoader);
        }
    }

    private static CpdResult fork(CpdRequest request) throws MavenReportException {
        File basePmdDir = new File(request.getTargetDirectory(), "pmd");
        basePmdDir.mkdirs();
        File cpdRequestFile = new File(basePmdDir, "cpdrequest.bin");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cpdRequestFile))) {
            out.writeObject(request);
        } catch (IOException e) {
            throw new MavenReportException(e.getMessage(), e);
        }

        String classpath = buildClasspath();
        ProcessBuilder pb = new ProcessBuilder();
        // note: using env variable instead of -cp cli arg to avoid length limitations under Windows
        pb.environment().put("CLASSPATH", classpath);
        pb.command().add(request.getJavaExecutable());
        pb.command().add(CpdExecutor.class.getName());
        pb.command().add(cpdRequestFile.getAbsolutePath());

        LOG.debug("Executing: CLASSPATH={}, command={}", classpath, pb.command());
        try {
            final Process p = pb.start();
            // Note: can't use pb.inheritIO(), since System.out/System.err has been modified after process start
            // and inheritIO would only inherit file handles, not the changed streams.
            ProcessStreamHandler.start(p.getInputStream(), System.out);
            ProcessStreamHandler.start(p.getErrorStream(), System.err);
            int exit = p.waitFor();
            LOG.debug("CpdExecutor exit code: {}", exit);
            if (exit != 0) {
                throw new MavenReportException("CpdExecutor exited with exit code " + exit);
            }
            return new CpdResult(new File(request.getTargetDirectory(), "cpd.xml"), request.getOutputEncoding());
        } catch (IOException e) {
            throw new MavenReportException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MavenReportException(e.getMessage(), e);
        }
    }

    /**
     * Execute CPD analysis from CLI.
     *
     * <p>
     * Single arg with the filename to the serialized {@link CpdRequest}.
     *
     * <p>
     * Exit-code: 0 = success, 1 = failure in executing
     *
     * @param args
     */
    public static void main(String[] args) {
        File requestFile = new File(args[0]);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(requestFile))) {
            CpdRequest request = (CpdRequest) in.readObject();
            CpdExecutor cpdExecutor = new CpdExecutor(request);
            cpdExecutor.setupLogLevel(request.getLogLevel());
            cpdExecutor.run();
            System.exit(0);
        } catch (IOException | ClassNotFoundException | MavenReportException e) {
            LOG.error(e.getMessage(), e);
        }
        System.exit(1);
    }

    private final CpdRequest request;

    /** Helper to exclude duplications from the result. */
    private final ExcludeDuplicationsFromFile excludeDuplicationsFromFile = new ExcludeDuplicationsFromFile();

    public CpdExecutor(CpdRequest request) {
        this.request = Objects.requireNonNull(request);
    }

    private CpdResult run() throws MavenReportException {
        try {
            excludeDuplicationsFromFile.loadExcludeFromFailuresData(request.getExcludeFromFailureFile());
        } catch (MojoExecutionException e) {
            throw new MavenReportException("Error loading exclusions", e);
        }

        CPDConfiguration cpdConfiguration = new CPDConfiguration();
        cpdConfiguration.setMinimumTileSize(request.getMinimumTokens());
        cpdConfiguration.setIgnoreAnnotations(request.isIgnoreAnnotations());
        cpdConfiguration.setIgnoreLiterals(request.isIgnoreLiterals());
        cpdConfiguration.setIgnoreIdentifiers(request.isIgnoreIdentifiers());

        Language cpdLanguage;
        if ("java".equals(request.getLanguage()) || null == request.getLanguage()) {
            cpdLanguage = new JavaLanguageModule();
        } else if ("javascript".equals(request.getLanguage())) {
            cpdLanguage = new EcmascriptLanguageModule();
        } else if ("jsp".equals(request.getLanguage())) {
            cpdLanguage = new JspLanguageModule();
        } else {
            cpdLanguage = cpdConfiguration.getLanguageRegistry().getLanguageById(request.getLanguage());
        }

        cpdConfiguration.setOnlyRecognizeLanguage(cpdLanguage);
        cpdConfiguration.setSourceEncoding(Charset.forName(request.getSourceEncoding()));

        request.getFiles().forEach(f -> cpdConfiguration.addInputPath(f.toPath()));

        LOG.debug("Executing CPD...");

        // always create XML format. we need to output it even if the file list is empty or we have no duplications
        // so the "check" goals can check for violations
        try (CpdAnalysis cpd = CpdAnalysis.create(cpdConfiguration)) {
            cpd.performAnalysis(report -> {
                try {
                    writeXmlReport(report);

                    // html format is handled by maven site report, xml format has already been rendered
                    String format = request.getFormat();
                    if (!"html".equals(format) && !"xml".equals(format)) {
                        writeFormattedReport(report);
                    }
                } catch (MavenReportException e) {
                    LOG.error(e.getMessage(), e);
                }
            });
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.debug("CPD finished.");

        return new CpdResult(new File(request.getTargetDirectory(), "cpd.xml"), request.getOutputEncoding());
    }

    private void writeXmlReport(CPDReport cpd) throws MavenReportException {
        File targetFile = writeReport(cpd, new XMLRenderer(request.getOutputEncoding()), "xml");
        if (request.isIncludeXmlInSite()) {
            File siteDir = new File(request.getReportOutputDirectory());
            siteDir.mkdirs();
            try {
                FileUtils.copyFile(targetFile, new File(siteDir, "cpd.xml"));
            } catch (IOException e) {
                throw new MavenReportException(e.getMessage(), e);
            }
        }
    }

    private File writeReport(CPDReport cpd, CPDReportRenderer r, String extension) throws MavenReportException {
        if (r == null) {
            return null;
        }

        File targetDir = new File(request.getTargetDirectory());
        targetDir.mkdirs();
        File targetFile = new File(targetDir, "cpd." + extension);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(targetFile), request.getOutputEncoding())) {
            r.render(cpd.filterMatches(filterMatches()), writer);
            writer.flush();
        } catch (IOException ioe) {
            throw new MavenReportException(ioe.getMessage(), ioe);
        }
        return targetFile;
    }

    private void writeFormattedReport(CPDReport cpd) throws MavenReportException {
        CPDReportRenderer r = createRenderer(request.getFormat(), request.getOutputEncoding());
        writeReport(cpd, r, request.getFormat());
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException if no renderer found for the output type
     */
    public static CPDReportRenderer createRenderer(String format, String outputEncoding) throws MavenReportException {
        CPDReportRenderer renderer = null;
        if ("xml".equals(format)) {
            renderer = new XMLRenderer(outputEncoding);
        } else if ("csv".equals(format)) {
            renderer = new CSVRenderer();
        } else if ("txt".equals(format)) {
            renderer = new SimpleRenderer();
        } else if (!"".equals(format) && !"none".equals(format)) {
            try {
                renderer = (CPDReportRenderer)
                        Class.forName(format).getConstructor().newInstance();
            } catch (Exception e) {
                throw new MavenReportException(
                        "Can't find CPD custom format " + format + ": "
                                + e.getClass().getName(),
                        e);
            }
        }

        return renderer;
    }

    private Predicate<Match> filterMatches() {
        return (Match match) -> {
            LOG.debug("Filtering duplications. Using " + excludeDuplicationsFromFile.countExclusions()
                    + " configured exclusions.");

            if (excludeDuplicationsFromFile.isExcludedFromFailure(match)) {
                LOG.debug("Excluded " + match + " duplications.");
                return false;
            } else {
                return true;
            }
        };
    }
}
