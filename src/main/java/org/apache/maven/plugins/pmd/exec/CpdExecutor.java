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
import java.nio.charset.Charset;
import java.util.Objects;

import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.CPDReportRenderer;
import net.sourceforge.pmd.cpd.CSVRenderer;
import net.sourceforge.pmd.cpd.CpdAnalysis;
import net.sourceforge.pmd.cpd.SimpleRenderer;
import net.sourceforge.pmd.cpd.XMLRenderer;
import net.sourceforge.pmd.lang.Language;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.ExcludeDuplicationsFromFile;
import org.apache.maven.reporting.MavenReportException;
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
        // we are not running CPD through CLI and deal with any errors during analysis on our own
        cpdConfiguration.setSkipLexicalErrors(true);

        String languageId = request.getLanguage();
        if ("javascript".equals(languageId)) {
            languageId = "ecmascript";
        } else if (languageId == null) {
            languageId = "java"; // default
        }
        Language cpdLanguage = cpdConfiguration.getLanguageRegistry().getLanguageById(languageId);

        cpdConfiguration.setOnlyRecognizeLanguage(cpdLanguage);
        cpdConfiguration.setSourceEncoding(Charset.forName(request.getSourceEncoding()));

        request.getFiles().forEach(f -> cpdConfiguration.addInputPath(f.toPath()));

        LOG.debug("Executing CPD...");
        try (CpdAnalysis cpd = CpdAnalysis.create(cpdConfiguration)) {
            CpdReportConsumer reportConsumer = new CpdReportConsumer(request, excludeDuplicationsFromFile);
            cpd.performAnalysis(reportConsumer);
        } catch (IOException e) {
            throw new MavenReportException("Error while executing CPD", e);
        }
        LOG.debug("CPD finished.");

        // in contrast to pmd goal, we don't have a parameter for cpd like "skipPmdError" - if there
        // are any errors during CPD analysis, the maven build fails.
        int cpdErrors = cpdConfiguration.getReporter().numErrors();
        if (cpdErrors == 1) {
            throw new MavenReportException("There was 1 error while executing CPD");
        } else if (cpdErrors > 1) {
            throw new MavenReportException("There were " + cpdErrors + " errors while executing CPD");
        }

        return new CpdResult(new File(request.getTargetDirectory(), "cpd.xml"), request.getOutputEncoding());
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
}
