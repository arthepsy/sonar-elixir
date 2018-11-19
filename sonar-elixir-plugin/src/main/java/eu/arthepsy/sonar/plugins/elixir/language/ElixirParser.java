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

import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElixirParser {
    private int lineCount = 0;
    private int emptyLineCount = 0;
    private int commentLineCount = 0;
    private int classCount = 0;
    private int publicFunctionCount = 0;
    private int privateFunctionCount = 0;

    private final Pattern defPattern = Pattern.compile("^\\s*def(|p|module|struct)\\s(.*)$");
    private final Matcher defMatcher = defPattern.matcher("");
    private final Pattern heredocPattern = Pattern.compile("^.*\"\"\"\\s*$");
    private final Matcher heredocMatcher = heredocPattern.matcher("");
    private final Pattern docPattern = Pattern.compile("^\\s*@(doc|moduledoc|typedoc)([\"\\s].*)$");
    private final Matcher docMatcher = docPattern.matcher("");

    ElixirParser() { }

    int getLineCount() { return lineCount; }
    int getEmptyLineCount() { return emptyLineCount; }
    int getCommentLineCount() { return commentLineCount; }
    int getClassCount() { return classCount; }
    int getPublicFunctionCount() { return publicFunctionCount; }
    int getPrivateFunctionCount() { return privateFunctionCount; }

    void parse(List<String> lines) {
        this.parseLines(lines);
    }

    private void parseLines(List<String> lines) {
        lineCount = lines.size();
        for (int i = 0; i < lineCount; i++) {
            String line = lines.get(i);
            if (StringUtils.isBlank(line)) {
                emptyLineCount++;
            }
            docMatcher.reset(line);
            boolean inDoc = docMatcher.find();
            if (inDoc) {
                commentLineCount++;
            }
            heredocMatcher.reset(line);
            if (heredocMatcher.find()) {
                while (i < lineCount - 1) {
                    if (inDoc) {
                        commentLineCount++;
                    }
                    i++;
                    line = lines.get(i);
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
                        break;
                    case "":
                        publicFunctionCount++;
                        break;
                    case "p":
                        privateFunctionCount++;
                        break;
                }
            }
            if (line.matches("^\\s*#.*$")) {
                commentLineCount++;
            }
        }
    }

}
