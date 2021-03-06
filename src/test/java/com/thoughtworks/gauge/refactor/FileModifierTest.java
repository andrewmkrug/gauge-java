// Copyright 2015 ThoughtWorks, Inc.

// This file is part of Gauge-Java.

// Gauge-Java is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Gauge-Java is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Gauge-Java.  If not, see <http://www.gnu.org/licenses/>.

package com.thoughtworks.gauge.refactor;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.apache.commons.io.FileUtils.copyFile;

public class FileModifierTest extends TestCase {

    private File file;

    protected void setUp() throws Exception {
        this.file = createFile();
    }

    private File createFile() throws IOException {
        File file = new File("src/test/resources/StepImpl.java");
        File destFile = new File(FileUtils.getTempDirectory(), file.getName());
        copyFile(file, destFile);
        return destFile;
    }

    public void testRefactorFileChange() throws Exception {
        String text = "New File "  + lineSeparator() + " Content "  + lineSeparator() + " in 3 lines";
        new FileModifier(new JavaRefactoringElement(3, 5, 0, text, file)).refactor();
        assertEquals(text, readFileLines(file, 3, 5));

        text = "@Step(\"step <abcd> and a table <table>\")"  + lineSeparator() +
                "public void stepWithTable(float abcd, Table table) {"  + lineSeparator() +
                "}";
        new FileModifier(new JavaRefactoringElement(13, 15, 5, text, file)).refactor();
        assertEquals("     @Step(\"step <abcd> and a table <table>\")"  + lineSeparator() +
                "     public void stepWithTable(float abcd, Table table) {"  + lineSeparator() +
                "     }", readFileLines(file, 13, 15));
    }

    private String lineSeparator() {
        return System.getProperty("line.separator");

    }

    private String readFileLines(File file, int startLine, int endLine) throws IOException {
        List<String> lines = FileUtils.readLines(file);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            int index = i + 1;
            if (index > startLine && index < endLine) {
                builder.append(lines.get(i)).append(lineSeparator());
            } else if (index == startLine) {
                builder.append(lines.get(i));
                if (endLine > startLine) builder.append(lineSeparator());
            } else if (index == endLine) {
                builder.append(lines.get(i));
            }
        }
        return builder.toString();
    }

    protected void tearDown() throws Exception {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

}
