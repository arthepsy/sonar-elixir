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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.*;

public class ElixirMeasureSensorTest {

    private final Project project = new Project("project");
    private SensorContext context = mock(SensorContext.class);
    private DefaultFileSystem fileSystem;
    private ElixirMeasureSensor sensor;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    private File baseDir;
    private ResourcePerspectives perspectives;

    @Before
    public void prepare() throws IOException {
        baseDir = temp.newFolder();
        perspectives = mock(ResourcePerspectives.class);
        fileSystem = new DefaultFileSystem();
        fileSystem.setBaseDir(baseDir);
        sensor = new ElixirMeasureSensor(fileSystem);
    }

    @Test
    public void testDocAnnotation() throws IOException {
        String fileName = "test_doc.ex";
        File source = new File(baseDir, fileName);
        FileUtils.write(source, IOUtils.toString(getClass().getResourceAsStream("/" + fileName)));
        DefaultInputFile inputFile = new DefaultInputFile(fileName).setLanguage(Elixir.KEY);
        inputFile.setAbsolutePath(new File(baseDir,inputFile.relativePath()).getAbsolutePath());
        fileSystem.add(inputFile);

        sensor.analyse(project, context);

        verify(context).saveMeasure(any(InputFile.class), eq(CoreMetrics.LINES), eq(21.0));
        verify(context).saveMeasure(any(InputFile.class), eq(CoreMetrics.NCLOC), eq(14.0));
        verify(context).saveMeasure(any(InputFile.class), eq(CoreMetrics.COMMENT_LINES), eq(3.0));
        verify(context).saveMeasure(any(InputFile.class), eq(CoreMetrics.CLASSES), eq(1.0));
        verify(context).saveMeasure(any(InputFile.class), eq(CoreMetrics.FUNCTIONS), eq(4.0));
        verify(context).saveMeasure(any(InputFile.class), eq(CoreMetrics.PUBLIC_API), eq(5.0));
        verify(context).saveMeasure(any(InputFile.class), eq(CoreMetrics.PUBLIC_UNDOCUMENTED_API), eq(2.0));
    }
}