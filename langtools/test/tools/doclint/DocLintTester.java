/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sun.tools.doclint.DocLint;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DocLintTester {

    public static void main(String... args) throws Exception {
        new DocLintTester().run(args);
    }

    public void run(String... args) throws Exception {
        String testSrc = System.getProperty("test.src");

        File refFile = null;
        List<String> opts = new ArrayList<String>();
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-ref")) {
                refFile = new File(testSrc, args[++i]);
            } else if (arg.startsWith("-Xmsgs")) {
                opts.add(arg);
            } else
                files.add(new File(testSrc, arg));
        }

        check(opts, files, refFile);

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void check(List<String> opts, List<File> files, File refFile) throws Exception {
        List<String> args = new ArrayList<String>();
        args.addAll(opts);
        for (File file: files)
            args.add(file.getPath());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        new DocLint().run(pw, args.toArray(new String[args.size()]));
        pw.flush();
        String out = normalizeNewlines(removeFileNames(sw.toString())).trim();
        if (out != null)
            System.err.println("Output:\n" + out);

        if (refFile == null) {
            if (!out.isEmpty())
                error("unexpected output");
        } else {
            String expect = readFile(refFile);
            if (!expect.equals(out)) {
                error("expected output not found");
                System.err.println("EXPECT>>" + expect + "<<");
                System.err.println(" FOUND>>" + out    + "<<");
            }
        }
    }

    String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        Reader in = new BufferedReader(new FileReader(file));
        try {
            char[] buf = new char[1024];
            int n;
            while ((n = in.read(buf)) != -1)
                sb.append(buf, 0, n);
        } finally {
            in.close();
        }
        return sb.toString().trim();
    }

    private static final Pattern dirFileLine = Pattern.compile(
            "(?m)"                          // multi-line mode
            + "^([^: ]+?)"                  // directory part of file name
            + "([A-Za-z0-9.]+:[0-9]+:)");   // file name and line number

    String removeFileNames(String s) {
        Matcher m = dirFileLine.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "$2");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final String nl = System.getProperty("line.separator");
    String normalizeNewlines(String s) {
        return (nl.equals("\n") ? s : s.replace(nl, "\n"));
    }


    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}