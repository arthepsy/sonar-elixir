/*
 * SonarQube Elixir plugin
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
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(inputFile.absolutePath()), fileSystem.encoding());
        } catch (IOException e) {
            LOG.warn(LOG_PREFIX + "could not process file: " + inputFile.toString());
            return;
        }
        ElixirParser parser = new ElixirParser();
        parser.parse(lines);

        LOG.debug(LOG_PREFIX + "processing file: " + inputFile.toString());
        double linesOfCode = parser.getLineCount() - parser.getEmptyLineCount() - parser.getCommentLineCount();
        context.saveMeasure(inputFile, CoreMetrics.LINES, (double)parser.getLineCount());
        context.saveMeasure(inputFile, CoreMetrics.NCLOC, (double)linesOfCode);
        context.saveMeasure(inputFile, CoreMetrics.COMMENT_LINES, (double)parser.getCommentLineCount());

        double publicApi = parser.getPublicFunctionCount() + parser.getClassCount();
        double documentedApi = parser.getDocumentedPublicFunctionCount() + parser.getDocumentedClassCount();
        double undocumentedApi = publicApi - documentedApi;
        double documentedApiDensity = (publicApi == 0 ? 100.0 : ParsingUtils.scaleValue(documentedApi / publicApi * 100, 2));
        context.saveMeasure(inputFile, CoreMetrics.PUBLIC_API, publicApi);
        context.saveMeasure(inputFile, CoreMetrics.PUBLIC_UNDOCUMENTED_API, undocumentedApi);
        context.saveMeasure(inputFile, CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY, documentedApiDensity);

        double functionCount = parser.getPublicFunctionCount() + parser.getPrivateFunctionCount();
        context.saveMeasure(inputFile, CoreMetrics.CLASSES, (double)parser.getClassCount());
        context.saveMeasure(inputFile, CoreMetrics.FUNCTIONS, (double)(functionCount));
    }
}
