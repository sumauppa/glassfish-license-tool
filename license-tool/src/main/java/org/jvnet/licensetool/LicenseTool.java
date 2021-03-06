/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.jvnet.licensetool;

import org.jvnet.licensetool.argparser.ArgParser;
import org.jvnet.licensetool.argparser.DefaultValue;
import org.jvnet.licensetool.argparser.Help;
import org.jvnet.licensetool.file.PlainBlock;
import org.jvnet.licensetool.file.FileWrapper;
import org.jvnet.licensetool.util.ToolUtil;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;
import java.util.regex.Pattern;

public class LicenseTool {
    private LicenseTool() {
    }

    public interface Arguments {
        @DefaultValue("true")
        @Help("Set to true to validate copyright header; if false, generate/update/insert copyright headers as needed")
        boolean validate();

        @DefaultValue("false")
        @Help("Set to true to get information about actions taken for every file.")
        boolean verbose();

        @DefaultValue("true")
        @Help("Set to true to avoid modifying any files")
        boolean dryrun();

        @Help("List of directories to process")
        @DefaultValue("")
        List<File> roots();

        @Help("List of directory names that should be skipped")
        @DefaultValue("")
        List<String> skipdirs();

        @Help("File containing text of copyright header.  This must not include any comment characters")
        @DefaultValue("")
        FileWrapper copyright();

        @DefaultValue("1997")
        @Help("Default copyright start year, if not otherwise specified")
        String startyear();

        @DefaultValue("")
        @Help("Default copyright end year, if not otherwise specified")
        String endyear();

        @Help("Extra options")
        @DefaultValue("")
        List<String> options();

        @Help("Version Control System")
        @DefaultValue("")
        String vcs();

        @DefaultValue("false")
        @Help("Set to true to use last modified date of the file from VCS history if it is later than the end year")
        boolean uselastmodified();


    }

    private static boolean validate;
    private static boolean verbose;


    private static void trace(String msg) {
        LOGGER.fine(msg);
    }

    private static final String COPYRIGHT = "Copyright";

    // Copyright year is first non-blank after COPYRIGHT
    private static String getSunCopyrightStart(String str) {
        int index = str.indexOf(COPYRIGHT);
        if (index == -1)
            return null;

        int pos = index + COPYRIGHT.length();
        char ch = str.charAt(pos);
        while (Character.isWhitespace(ch) && (pos < str.length())) {
            ch = str.charAt(++pos);
        }

        int start = pos;
        ch = str.charAt(pos);
        while (Character.isDigit(ch) && (pos < str.length())) {
            ch = str.charAt(++pos);
        }

        if (pos == start)
            return null;

        return str.substring(start, pos);
    }

    private static final String START_YEAR = "StartYear";
    private static final Logger LOGGER = Logger.getLogger(LicenseTool.class.getName());

    private static PlainBlock makeCopyrightPatternBlock(PlainBlock copyrightTextBlock) {
        StringBuilder sb = new StringBuilder();
        String copyrightText = Pattern.quote(copyrightTextBlock.contents());

        for(String line: ToolUtil.splitToLines(copyrightText) ) {
            if(line.contains("YYYY ")) {
                line = line.replace("YYYY ", "\\E[0-9]{4}(-[0-9]{4})? \\Q");
            } else if(line.contains("YYYY, ")) {
                line = line.replace("YYYY, ","\\E([0-9]{4}, )+\\Q");
            }
            sb.append(line);
        }
        return new PlainBlock(sb.toString());

    }
    private static PlainBlock makeCopyrightBlock(String startYear,
                                            PlainBlock copyrightText) {

        trace("makeCopyrightBlock: startYear = " + startYear);
        trace("makeCopyrightBlock: copyrightText = " + copyrightText);

        trace("Contents of copyrightText block:");
        trace(copyrightText.contents());

        Map<String, String> map = new HashMap<String, String>();
        map.put(START_YEAR, startYear);
        PlainBlock withStart = copyrightText.instantiateTemplate(map);

        trace("Contents of copyrightText block withStart date:");
        trace(withStart.contents());

        return withStart;
    }

    public static void process(Arguments args) {
        String startYear = args.startyear();
        verbose = args.verbose();
        validate = args.validate();
        Formatter formatter = new Formatter() {
            private String lineSeparator = (String) java.security.AccessController.doPrivileged(
                    new sun.security.action.GetPropertyAction("line.separator"));

            public String format(LogRecord record) {
                StringBuffer sb = new StringBuffer();
                String message = formatMessage(record);
                sb.append(record.getLevel().getLocalizedName());
                sb.append(": ");
                sb.append(message);
                sb.append(lineSeparator);
                if (record.getThrown() != null) {
                    try {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        pw.close();
                        sb.append(sw.toString());
                    } catch (Exception ex) {
                    }
                }
                return sb.toString();
            }

        };
        java.util.logging.StreamHandler sh = new StreamHandler(System.out,formatter);
        Logger domainLogger = Logger.getLogger("org.jvnet.licensetool");
        domainLogger.setUseParentHandlers(false);
        
        if(verbose) {
            domainLogger.setLevel(Level.FINE);
            sh.setLevel(Level.FINE);
        }
        domainLogger.addHandler(sh);
        trace("Main: args:\n" + args);
//        Versioned.VCS vcs = null;
//        if(!args.vcs().equals("")) {
//            vcs = Versioned.VCS.valueOf(args.vcs());
//        } else {
//            vcs= VCSHelper.sniffVCS(args.roots().get(0));
//        }

        try {
            // Create the blocks needed for different forms of the
            // copyright comment template                                                                   n ddr
            final PlainBlock copyrightText = new PlainBlock(args.copyright());

            //TODO remove 123
            //PlainBlock copyrightTemplate = makeCopyrightBlock(startYear, copyrightText);
            PlainBlock copyrightTemplate = makeCopyrightPatternBlock(copyrightText);


            Scanner scanner = new Scanner(args, args.roots());
            for (String str : args.skipdirs())
                scanner.addDirectoryToSkip(str);

            Scanner.Action action;
            if(validate) {
                action = new ActionFactory().getValidateCopyrightAction(copyrightText, copyrightTemplate, args);
            } else {
                action = new ActionFactory().getModifyCopyrightAction(copyrightText, copyrightTemplate, args);
                //action = new ActionFactory(verbose).getReWriteCopyrightAction();
            }
            // Finally, we process all files
            scanner.scan(new RecognizerFactory().getDefaultRecognizer(), action);
        } catch (Exception ex) {
            LOGGER.warning("Exception while processing: " + ex);
            ex.printStackTrace();
        }
    }

    public static void main(String[] strs) {
        ArgParser<Arguments> ap = new ArgParser(Arguments.class);
        Arguments args = ap.parse(strs);
        LicenseTool.process(args);        
    }

}