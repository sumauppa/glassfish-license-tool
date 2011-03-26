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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.jvnet.licensetool.file.FileWrapper;
import org.jvnet.licensetool.file.Versioned;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


public class LicenseToolTask extends MatchingTask {
    boolean validate = true;
    boolean verbose = false;
    List<File> roots = new ArrayList<File>();
    List<String> skipdirs;
    List<String> options   = new ArrayList<String>();
    FileWrapper copyright;
    String startyear;
    String endyear;
    boolean dryrun = false;
    boolean uselastmodified = false;
    String vcs = "";
    private File srcDir;

    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDryrun(boolean dryrun) {
        this.dryrun = dryrun;
    }

    public void setCopyright(File copyright) {
        this.copyright = new FileWrapper(copyright);
    }

    public void setStartyear(String startyear) {
        this.startyear = startyear;
    }

    public void setEndyear(String endyear) {
        this.endyear = endyear;
    }

    public void setSkipdirs(String skipdirs) {
        this.skipdirs = Arrays.asList(skipdirs.split(","));
    }

    public void setVcs(String vcs) {
            this.vcs = vcs;
    }

    public void setUselastmodified(boolean uselastmodified) {
            this.uselastmodified = uselastmodified;
        }


    public void setOptions(String options) {
        this.options = Arrays.asList(options.split(","));
    }

    @Override
    protected DirectoryScanner getDirectoryScanner(File baseDir) {
        fileset.setDir(baseDir);
        return fileset.getDirectoryScanner(getProject());
    }

    public void execute() throws BuildException {
        log("Running LicenseToolTask ", Project.MSG_INFO);
        String[] files = getDirectoryScanner(srcDir).getIncludedFiles();

        for (String relPath : files) {
            File sfile = new File(srcDir, relPath);
            roots.add(sfile);            
        }
        LicenseTool.Arguments args = new LicenseTool.Arguments() {
            public boolean validate() {
                return validate;
            }
            public boolean verbose() {
                return verbose;
            }
            public boolean dryrun() {
                return dryrun;
            }
            public List<File> roots() {
                return roots;
            }
            public List<String> skipdirs() {
                return skipdirs;
            }
            public FileWrapper copyright() {
                return copyright;
            }
            public String startyear() {
                return startyear;
            }

            public String endyear() {
                return endyear;
            }

            public List<String> options() {
                return options;
            }

            public String vcs() {
                return vcs;
            }

            public boolean uselastmodified() {
                return false;
            }
        };

       LicenseTool.process(args);
        
    }
}

