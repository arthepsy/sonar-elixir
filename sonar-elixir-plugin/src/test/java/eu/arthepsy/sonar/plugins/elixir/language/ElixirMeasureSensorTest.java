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

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class ElixirMeasureSensorTest {

    @Mock
    private SensorContext context;

    @Mock
    private DefaultFileSystem fileSystem;

    @Mock
    private MetricFinder metricFinder;

    @Mock
    private DefaultIndexedFile indexedFile;

    @Mock
    private Consumer<DefaultInputFile> metadataGenerator;

    @Mock
    private FilePredicates filePredicates;

    @Mock
    private Metric<Serializable> metric;

    @Mock
    private NewMeasure<Serializable> newMeasure;

    private ElixirMeasureSensor sensor;

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);

        when(fileSystem.predicates()).thenReturn(filePredicates);
        sensor = new ElixirMeasureSensor(fileSystem, metricFinder);
    }

    @Test
    public void testDocAnnotation() throws IOException {
        String fileName = "test_doc.ex";
        InputStream testInputStream = getClass().getResourceAsStream("/" + fileName);

        String testFileContents = IOUtils.toString(testInputStream, Charset.defaultCharset());
        DefaultInputFile inputFile = new DefaultInputFile(indexedFile, metadataGenerator, testFileContents);
        inputFile.setCharset(Charset.defaultCharset());
        when(indexedFile.language()).thenReturn(Elixir.KEY);

        List<InputFile> inputFiles = new ArrayList<>();
        inputFiles.add(inputFile);
        when(fileSystem.inputFiles(any(FilePredicate.class))).thenReturn(inputFiles);

        when(metricFinder.findByKey(CoreMetrics.LINES_KEY)).thenReturn(metric);
        when(metricFinder.findByKey(CoreMetrics.NCLOC_KEY)).thenReturn(metric);
        when(metricFinder.findByKey(CoreMetrics.COMMENT_LINES_KEY)).thenReturn(metric);
        when(metricFinder.findByKey(CoreMetrics.FUNCTIONS_KEY)).thenReturn(metric);
        when(metricFinder.findByKey(CoreMetrics.CLASSES_KEY)).thenReturn(metric);
        when(context.newMeasure()).thenReturn(newMeasure);
        when(newMeasure.forMetric(Matchers.any())).thenReturn(newMeasure);
        when(newMeasure.on(any(InputFile.class))).thenReturn(newMeasure);

        sensor.execute(context);

        verify(newMeasure).withValue(eq(37));
        verify(newMeasure).withValue(eq(15));
        verify(newMeasure).withValue(eq(14));
        verify(newMeasure).withValue(eq(1));
        verify(newMeasure).withValue(eq(6));
    }
}