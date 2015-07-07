/*
 * Elixir
 * Copyright (C) 2015 Andris Raugulis
 * moo@arthepsy.eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package eu.arthepsy.sonar.plugins.elixir.language;

import eu.arthepsy.sonar.plugins.elixir.ElixirConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.ParsingUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElixirMeasureSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(ElixirMeasureSensor.class);
    private static final String LOG_PREFIX = ElixirConfiguration.LOG_PREFIX;

    private final FileSystem fileSystem;
    private final FilePredicate mainFilePredicate;

    private final Pattern defPattern = Pattern.compile("^\\s*def(|p|module|struct)\\s(.*)$");
    private final Matcher defMatcher = defPattern.matcher("");
    private final Pattern heredocPattern = Pattern.compile("^.*\"\"\"\\s*$");
    private final Matcher heredocMatcher = heredocPattern.matcher("");
    private final Pattern docPattern = Pattern.compile("^\\s*@(moduledoc|doc)([\"\\s].*)$");
    private final Matcher docMatcher = docPattern.matcher("");

    public ElixirMeasureSensor(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        this.mainFilePredicate = fileSystem.predicates().and(
                fileSystem.predicates().hasType(InputFile.Type.MAIN),
                fileSystem.predicates().hasLanguage(Elixir.KEY));
    }

    @Override
    public void analyse(Project project, SensorContext context) {
        LOG.info("[elixir] analyse");
        for (InputFile file : fileSystem.inputFiles(mainFilePredicate )) {
            processMainFile(file, context);
        }
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return fileSystem.hasFiles(mainFilePredicate);
    }

    private void processMainFile(InputFile inputFile, SensorContext context) {
        int lineCount = 0;
        int emptyLineCount = 0;
        int commentLineCount = 0;
        int classCount = 0;
        int publicFunctionCount = 0;
        int privateFunctionCount = 0;
        int documentedClassCount = 0;
        int documentedPrivateFunctionCount = 0;
        int documentedPublicFunctionCount = 0;
        boolean hasDoc = false;
        boolean inClass = false;

        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(inputFile.absolutePath()), fileSystem.encoding());
            lineCount = lines.size();
            for (int i = 0; i < lineCount; i++) {
                String line = lines.get(i);
                if (StringUtils.isBlank(line)) {
                    emptyLineCount++;
                }
                docMatcher.reset(line);
                boolean inDoc = docMatcher.find();
                if (inDoc) {
                    String docText = docMatcher.group(2).trim();
                    if (! (StringUtils.equalsIgnoreCase(docText, "false") || StringUtils.equalsIgnoreCase(docText, "nil"))) {
                        switch (docMatcher.group(1)) {
                            case "doc":
                                hasDoc = true;
                                break;
                            case "moduledoc":
                                if (inClass) {
                                    documentedClassCount++;
                                }
                                break;
                        }
                    }
                }
                heredocMatcher.reset(line);
                if (heredocMatcher.find()) {
                    if (inDoc) {
                        commentLineCount++;
                    }
                    while (i < lineCount - 1) {
                        if (inDoc) {
                            commentLineCount++;
                        }
                        i++;
                        line = lines.get(i);
                        if (StringUtils.isBlank(line)) {
                            emptyLineCount++;
                        }
                        if (line.matches("^\\s*\"\"\"\\s*$")) {
                            break;
                        }
                    }
                    continue;
                }

                defMatcher.reset(line);
                if (defMatcher.find()) {
                    switch (defMatcher.group(1)) {
                        case "module":
                            classCount++;
                            inClass = true;
                            break;
                        case "":
                            publicFunctionCount++;
                            if (hasDoc) {
                                documentedPublicFunctionCount++;
                            }
                            break;
                        case "p":
                            privateFunctionCount++;
                            if (hasDoc) {
                                documentedPrivateFunctionCount++;
                            }
                            break;
                    }
                    hasDoc = false;
                }
                if (line.matches("^\\s*#.*$")) {
                    commentLineCount++;
                }
            }
        } catch (IOException e) {
            LOG.warn(LOG_PREFIX + "could not process file: " + inputFile.toString());
        }
        if (lines != null) {
            LOG.debug(LOG_PREFIX + "processing file: " + inputFile.toString());
            context.saveMeasure(inputFile, CoreMetrics.LINES, (double) lineCount);
            context.saveMeasure(inputFile, CoreMetrics.NCLOC, (double)(lineCount - emptyLineCount - commentLineCount));
            context.saveMeasure(inputFile, CoreMetrics.COMMENT_LINES, (double)commentLineCount);

            double publicApi = publicFunctionCount + classCount;
            double documentedApi = documentedPublicFunctionCount + documentedClassCount;
            double undocumentedApi = publicApi - documentedApi;
            double documentedApiDensity = (publicApi == 0 ? 100.0 : ParsingUtils.scaleValue(documentedApi / publicApi * 100, 2));
            context.saveMeasure(inputFile, CoreMetrics.PUBLIC_API, publicApi);
            context.saveMeasure(inputFile, CoreMetrics.PUBLIC_UNDOCUMENTED_API, undocumentedApi);
            context.saveMeasure(inputFile, CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY, documentedApiDensity);

            context.saveMeasure(inputFile, CoreMetrics.CLASSES, (double)classCount);
            context.saveMeasure(inputFile, CoreMetrics.FUNCTIONS, (double)(publicFunctionCount + privateFunctionCount));
        }
    }
}
