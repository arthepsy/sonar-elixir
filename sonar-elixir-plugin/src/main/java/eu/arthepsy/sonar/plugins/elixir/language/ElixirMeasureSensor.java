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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class ElixirMeasureSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(ElixirMeasureSensor.class);
    private static final String LOG_PREFIX = ElixirConfiguration.LOG_PREFIX;

    private final FileSystem fileSystem;
    private final FilePredicate mainFilePredicate;
    private final MetricFinder metricFinder;


    public ElixirMeasureSensor(FileSystem fileSystem, MetricFinder metricFinder) {
        this.fileSystem = fileSystem;
        this.mainFilePredicate = fileSystem.predicates().and(
                fileSystem.predicates().hasType(InputFile.Type.MAIN),
                fileSystem.predicates().hasLanguage(Elixir.KEY));
        this.metricFinder = metricFinder;
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
    }

    public void execute(SensorContext context) {
        LOG.info("[elixir] analyse");
        for (InputFile file : fileSystem.inputFiles(mainFilePredicate)) {
            processMainFile(file, context);
        }
    }

    private void processMainFile(InputFile inputFile, SensorContext context) {
        List<String> lines;
        try {
            String contents = inputFile.contents();
            lines = Arrays.asList(contents.split("\\r?\\n"));
        } catch (IOException e) {
            LOG.warn(LOG_PREFIX + "could not process file: " + inputFile.toString());
            return;
        }
        ElixirParser parser = new ElixirParser();
        parser.parse(lines);

        LOG.debug(LOG_PREFIX + "processing file: " + inputFile.toString());
        int linesOfCode = parser.getLineCount() - parser.getEmptyLineCount() - parser.getCommentLineCount();
        saveMeasure(inputFile, context, CoreMetrics.LINES_KEY, parser.getLineCount());
        saveMeasure(inputFile, context, CoreMetrics.NCLOC_KEY, linesOfCode);
        saveMeasure(inputFile, context, CoreMetrics.COMMENT_LINES_KEY, parser.getCommentLineCount());

        int functionCount = parser.getPublicFunctionCount() + parser.getPrivateFunctionCount();
        saveMeasure(inputFile, context, CoreMetrics.FUNCTIONS_KEY, functionCount);
        saveMeasure(inputFile, context, CoreMetrics.CLASSES_KEY, parser.getClassCount());

    }

    private void saveMeasure(InputFile inputFile, SensorContext context, String metricKey, Serializable value) {
        org.sonar.api.batch.measure.Metric<Serializable> metric = metricFinder.findByKey(metricKey);
        if (metric == null) {
            throw new IllegalStateException("Unknown metric with key: " + metricKey);
        }
        NewMeasure<Serializable> measure = context.newMeasure().forMetric(metric).on(inputFile);
        measure.withValue(value);
        measure.save();
    }
}
