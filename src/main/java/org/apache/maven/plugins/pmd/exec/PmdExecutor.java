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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleSetLoadException;
import net.sourceforge.pmd.RuleSetLoader;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.benchmark.TextTimingReportRenderer;
import net.sourceforge.pmd.benchmark.TimeTracker;
import net.sourceforge.pmd.benchmark.TimingReport;
import net.sourceforge.pmd.benchmark.TimingReportRenderer;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.Predicate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.ExcludeViolationsFromFile;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes PMD with the configuration provided via {@link PmdRequest}.
 */
public class PmdExecutor extends Executor {
    private static final Logger LOG = LoggerFactory.getLogger(PmdExecutor.class);

    public static PmdResult execute(PmdRequest request) throws MavenReportException {
        if (request.getJavaExecutable() != null) {
            return fork(request);
        }

        // make sure the class loaders are correct and call this in the same JVM
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(PmdExecutor.class.getClassLoader());
            PmdExecutor executor = new PmdExecutor(request);
            return executor.run();
        } finally {
            Thread.currentThread().setContextClassLoader(origLoader);
        }
    }

    private static PmdResult fork(PmdRequest request) throws MavenReportException {
        File basePmdDir = new File(request.getTargetDirectory(), "pmd");
        basePmdDir.mkdirs();
        File pmdRequestFile = new File(basePmdDir, "pmdrequest.bin");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(pmdRequestFile))) {
            out.writeObject(request);
        } catch (IOException e) {
            throw new MavenReportException(e.getMessage(), e);
        }

        String classpath = buildClasspath();
        ProcessBuilder pb = new ProcessBuilder();
        // note: using env variable instead of -cp cli arg to avoid length limitations under Windows
        pb.environment().put("CLASSPATH", classpath);
        pb.command().add(request.getJavaExecutable());
        pb.command().add(PmdExecutor.class.getName());
        pb.command().add(pmdRequestFile.getAbsolutePath());

        LOG.debug("Executing: CLASSPATH={}, command={}", classpath, pb.command());
        try {
            final Process p = pb.start();
            // Note: can't use pb.inheritIO(), since System.out/System.err has been modified after process start
            // and inheritIO would only inherit file handles, not the changed streams.
            ProcessStreamHandler.start(p.getInputStream(), System.out);
            ProcessStreamHandler.start(p.getErrorStream(), System.err);
            int exit = p.waitFor();
            LOG.debug("PmdExecutor exit code: {}", exit);
            if (exit != 0) {
                throw new MavenReportException("PmdExecutor exited with exit code " + exit);
            }
            return new PmdResult(new File(request.getTargetDirectory(), "pmd.xml"), request.getOutputEncoding());
        } catch (IOException e) {
            throw new MavenReportException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MavenReportException(e.getMessage(), e);
        }
    }

    /**
     * Execute PMD analysis from CLI.
     *
     * <p>
     * Single arg with the filename to the serialized {@link PmdRequest}.
     *
     * <p>
     * Exit-code: 0 = success, 1 = failure in executing
     *
     * @param args
     */
    public static void main(String[] args) {
        File requestFile = new File(args[0]);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(requestFile))) {
            PmdRequest request = (PmdRequest) in.readObject();
            PmdExecutor pmdExecutor = new PmdExecutor(request);
            pmdExecutor.setupLogLevel(request.getLogLevel());
            pmdExecutor.run();
            System.exit(0);
        } catch (IOException | ClassNotFoundException | MavenReportException e) {
            LOG.error(e.getMessage(), e);
        }
        System.exit(1);
    }

    private final PmdRequest request;

    public PmdExecutor(PmdRequest request) {
        this.request = Objects.requireNonNull(request);
    }

    private PmdResult run() throws MavenReportException {
        setupPmdLogging(request.isShowPmdLog(), request.getLogLevel());

        PMDConfiguration configuration = new PMDConfiguration();
        LanguageVersion languageVersion = null;
        Language language = LanguageRegistry.findLanguageByTerseName(
                request.getLanguage() != null ? request.getLanguage() : "java");
        if (language == null) {
            throw new MavenReportException("Unsupported language: " + request.getLanguage());
        }
        if (request.getLanguageVersion() != null) {
            languageVersion = language.getVersion(request.getLanguageVersion());
            if (languageVersion == null) {
                throw new MavenReportException("Unsupported targetJdk value '" + request.getLanguageVersion() + "'.");
            }
        } else {
            languageVersion = language.getDefaultVersion();
        }
        LOG.debug("Using language " + languageVersion);
        configuration.setDefaultLanguageVersion(languageVersion);

        if (request.getSourceEncoding() != null) {
            configuration.setSourceEncoding(request.getSourceEncoding());
        }

        configuration.prependAuxClasspath(request.getAuxClasspath());

        if (request.getSuppressMarker() != null) {
            configuration.setSuppressMarker(request.getSuppressMarker());
        }
        if (request.getAnalysisCacheLocation() != null) {
            configuration.setAnalysisCacheLocation(request.getAnalysisCacheLocation());
            LOG.debug("Using analysis cache location: " + request.getAnalysisCacheLocation());
        } else {
            configuration.setIgnoreIncrementalAnalysis(true);
        }

        configuration.setRuleSets(request.getRulesets());
        configuration.setMinimumPriority(RulePriority.valueOf(request.getMinimumPriority()));
        if (request.getBenchmarkOutputLocation() != null) {
            configuration.setBenchmark(true);
        }
        List<File> files = request.getFiles();

        Report report = null;

        if (request.getRulesets().isEmpty()) {
            LOG.debug("Skipping PMD execution as no rulesets are defined.");
        } else {
            if (request.getBenchmarkOutputLocation() != null) {
                TimeTracker.startGlobalTracking();
            }

            try {
                report = processFilesWithPMD(configuration, files);
            } finally {
                if (request.getAuxClasspath() != null) {
                    ClassLoader classLoader = configuration.getClassLoader();
                    if (classLoader instanceof Closeable) {
                        Closeable closeable = (Closeable) classLoader;
                        try {
                            closeable.close();
                        } catch (IOException ex) {
                            // ignore
                        }
                    }
                }
                if (request.getBenchmarkOutputLocation() != null) {
                    TimingReport timingReport = TimeTracker.stopGlobalTracking();
                    writeBenchmarkReport(
                            timingReport, request.getBenchmarkOutputLocation(), request.getOutputEncoding());
                }
            }
        }

        if (report != null && !report.getProcessingErrors().isEmpty()) {
            List<Report.ProcessingError> errors = report.getProcessingErrors();
            if (!request.isSkipPmdError()) {
                LOG.error("PMD processing errors:");
                LOG.error(getErrorsAsString(errors, request.isDebugEnabled()));
                throw new MavenReportException("Found " + errors.size() + " PMD processing errors");
            }
            LOG.warn("There are {} PMD processing errors:", errors.size());
            LOG.warn(getErrorsAsString(errors, request.isDebugEnabled()));
        }

        report = removeExcludedViolations(report);
        // always write XML report, as this might be needed by the check mojo
        // we need to output it even if the file list is empty or we have no violations
        // so the "check" goals can check for violations
        writeXmlReport(report);

        // write any other format except for xml and html. xml has just been produced.
        // html format is produced by the maven site formatter. Excluding html here
        // avoids using PMD's own html formatter, which doesn't fit into the maven site
        // considering the html/css styling
        String format = request.getFormat();
        if (!"html".equals(format) && !"xml".equals(format)) {
            writeFormattedReport(report);
        }

        return new PmdResult(new File(request.getTargetDirectory(), "pmd.xml"), request.getOutputEncoding());
    }

    /**
     * Gets the errors as a single string. Each error is in its own line.
     * @param withDetails if <code>true</code> then add the error details additionally (contains e.g. the stacktrace)
     * @return the errors as string
     */
    private String getErrorsAsString(List<Report.ProcessingError> errors, boolean withDetails) {
        List<String> errorsAsString = new ArrayList<>(errors.size());
        for (Report.ProcessingError error : errors) {
            errorsAsString.add(error.getFile() + ": " + error.getMsg());
            if (withDetails) {
                errorsAsString.add(error.getDetail());
            }
        }
        return String.join(System.lineSeparator(), errorsAsString);
    }

    private void writeBenchmarkReport(TimingReport timingReport, String benchmarkOutputLocation, String encoding) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(benchmarkOutputLocation), encoding)) {
            final TimingReportRenderer renderer = new TextTimingReportRenderer();
            renderer.render(timingReport, writer);
        } catch (IOException e) {
            LOG.error("Unable to generate benchmark file: {}", benchmarkOutputLocation, e);
        }
    }

    private Report processFilesWithPMD(PMDConfiguration pmdConfiguration, List<File> files)
            throws MavenReportException {
        Report report = null;
        RuleSetLoader rulesetLoader =
                RuleSetLoader.fromPmdConfig(pmdConfiguration).warnDeprecated(true);
        try {
            // load the ruleset once to log out any deprecated rules as warnings
            rulesetLoader.loadFromResources(pmdConfiguration.getRuleSetPaths());
        } catch (RuleSetLoadException e1) {
            throw new MavenReportException("The ruleset could not be loaded", e1);
        }

        try (PmdAnalysis pmdAnalysis = PmdAnalysis.create(pmdConfiguration)) {
            for (File file : files) {
                pmdAnalysis.files().addFile(file.toPath());
            }
            LOG.debug("Executing PMD...");
            report = pmdAnalysis.performAnalysisAndCollectReport();
            LOG.debug(
                    "PMD finished. Found {} violations.", report.getViolations().size());
        } catch (Exception e) {
            String message = "Failure executing PMD: " + e.getLocalizedMessage();
            if (!request.isSkipPmdError()) {
                throw new MavenReportException(message, e);
            }
            LOG.warn(message, e);
        }
        return report;
    }

    /**
     * Use the PMD XML renderer to create the XML report format used by the
     * check mojo later on.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeXmlReport(Report report) throws MavenReportException {
        File targetFile = writeReport(report, new XMLRenderer(request.getOutputEncoding()));
        if (request.isIncludeXmlInReports()) {
            File outputDirectory = new File(request.getReportOutputDirectory());
            outputDirectory.mkdirs();
            try {
                FileUtils.copyFile(targetFile, new File(outputDirectory, "pmd.xml"));
            } catch (IOException e) {
                throw new MavenReportException(e.getMessage(), e);
            }
        }
    }

    private File writeReport(Report report, Renderer r) throws MavenReportException {
        if (r == null) {
            return null;
        }

        File targetDir = new File(request.getTargetDirectory());
        targetDir.mkdirs();
        String extension = r.defaultFileExtension();
        File targetFile = new File(targetDir, "pmd." + extension);
        LOG.debug("Target PMD output file: {}", targetFile);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(targetFile), request.getOutputEncoding())) {
            r.setWriter(writer);
            r.start();
            if (report != null) {
                r.renderFileReport(report);
            }
            r.end();
            r.flush();
        } catch (IOException ioe) {
            throw new MavenReportException(ioe.getMessage(), ioe);
        }

        return targetFile;
    }

    /**
     * Use the PMD renderers to render in any format aside from HTML and XML.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeFormattedReport(Report report) throws MavenReportException {
        Renderer renderer = createRenderer(request.getFormat(), request.getOutputEncoding());
        writeReport(report, renderer);
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException
     *             if no renderer found for the output type
     */
    public static Renderer createRenderer(String format, String outputEncoding) throws MavenReportException {
        LOG.debug("Renderer requested: {}", format);
        Renderer result = null;
        if ("xml".equals(format)) {
            result = new XMLRenderer(outputEncoding);
        } else if ("txt".equals(format)) {
            result = new TextRenderer();
        } else if ("csv".equals(format)) {
            result = new CSVRenderer();
        } else if ("html".equals(format)) {
            result = new HTMLRenderer();
        } else if (!"".equals(format) && !"none".equals(format)) {
            try {
                result = (Renderer) Class.forName(format).getConstructor().newInstance();
            } catch (Exception e) {
                throw new MavenReportException(
                        "Can't find PMD custom format " + format + ": "
                                + e.getClass().getName(),
                        e);
            }
        }

        return result;
    }

    private Report removeExcludedViolations(Report report) throws MavenReportException {
        if (report == null) {
            return null;
        }

        ExcludeViolationsFromFile excludeFromFile = new ExcludeViolationsFromFile();

        try {
            excludeFromFile.loadExcludeFromFailuresData(request.getExcludeFromFailureFile());
        } catch (MojoExecutionException e) {
            throw new MavenReportException("Unable to load exclusions", e);
        }

        LOG.debug("Removing excluded violations. Using {} configured exclusions.", excludeFromFile.countExclusions());
        int violationsBefore = report.getViolations().size();

        Report filtered = report.filterViolations(new Predicate<RuleViolation>() {
            @Override
            public boolean test(RuleViolation ruleViolation) {
                return !excludeFromFile.isExcludedFromFailure(ruleViolation);
            }
        });

        int numberOfExcludedViolations =
                violationsBefore - filtered.getViolations().size();
        LOG.debug("Excluded {} violations.", numberOfExcludedViolations);
        return filtered;
    }
}
