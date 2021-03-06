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

import static org.jvnet.licensetool.Tags.COPYRIGHT_BLOCK_TAG;
import static org.jvnet.licensetool.Tags.OWN_COPYRIGHT_TAG;

import org.jvnet.licensetool.file.*;
import org.jvnet.licensetool.util.CopyrightParser;
import org.jvnet.licensetool.util.ToolUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.List;

public class ActionFactory {
    private final String COPYRIGHT = "Copyright";

    /**
     * returns an action that returns true.  If verbose is true, the action
     * also displays the FileWrapper that was passed to it.
     */
    public Scanner.Action getSkipAction() {
        return new Scanner.Action() {
            public String toString() {
                return "SkipAction";
            }

            public boolean evaluate(ParsedFile arg) {
                trace(toString() + "called");
                return true;
            }

        };
    }

    /**
     * returns an action that returns false.  If verbose is true, the action
     * also displays the FileWrapper that was passed to it.
     */
    public Scanner.Action getStopAction() {
        return new Scanner.Action() {
            public String toString() {
                return "StopAction";
            }

            public boolean evaluate(ParsedFile arg) {
                trace(toString() + "called.");
                return false;
            }
        };
    }

    public Scanner.Action getValidateCopyrightAction(final PlainBlock copyrightBlock, final PlainBlock copyrightTemplateBlock, final LicenseTool.Arguments args) {
        trace("makeCopyrightBlockAction: copyrightText = " + copyrightBlock);


        return new Scanner.Action() {
            public String toString() {
                return "CopyrightBlockAction[copyrightText=" + copyrightBlock + "]";
            }

            // Generally always return true, because we want to see ALL validation errors.
            public boolean evaluate(ParsedFile pfile) {
                //tag blocks
                boolean hadAnOldSunCopyright = tagBlocks(pfile, args);
                if (!hadAnOldSunCopyright) {
                    validationError(null, "No Sun/Oracle Copyright header in ", pfile.getPath());
                }
                // There should be a Sun copyright block in the first block
                int countSunCopyright = 0;
                for (CommentBlock block : pfile.getComments()) {
                    if (block.hasTags(OWN_COPYRIGHT_TAG, COPYRIGHT_BLOCK_TAG)) {
                        countSunCopyright++;
                        if (countSunCopyright > 1) {
                            validationError(block, "More than one Sun/Oracle Copyright Block", pfile.getPath());
                            continue;
                        }
                        if (block.hasTag(CommentBlock.TOP_COMMENT_BLOCK)) {
                            //if (!(ToolUtil.areCommentsSimilar(copyrightBlock.contents(),block.comment()))) {
                            if (!(ToolUtil.doesCopyrightMatch(copyrightTemplateBlock.contents(),block.comment()))) {
                                // It should entirely match copyrightText
                                validationError(block, "First block has incorrect copyright text", pfile.getPath());
                            }
                        } else {
                            validationError(block, "Sun/Oracle Copyright Block is not the first comment block", pfile.getPath());
                        }
                    } else {
                        //if empty comment block, remove it.
                        if(args.options().contains("checkEmpty") && isEmpty(block.comment())){
                            validationError(block, "Empty comment block in", pfile.getPath());
                        }

                    }
                }
                return true;
            }

            private boolean isEmpty(String content) {
                return content.trim().equals("");
            }
        };
    }

    // Strip out old Sun copyright block.  Prepend new copyrightText.
    // copyrightText is a Block containing a copyright template in the correct comment format.
    // parseCall is the correct block parser for splitting the file into Blocks.
    // defaultStartYear is the default year to use in copyright comments if not
    // otherwise specified in an old copyright block.
    // afterFirstBlock is true if the copyright needs to start after the first block in the
    // file.

    public Scanner.Action getModifyCopyrightAction(final PlainBlock copyrightBlock, final PlainBlock copyrightTemplateBlock, final LicenseTool.Arguments args) {
        trace("makeCopyrightBlockAction: copyrightText = " + copyrightBlock);


        return new Scanner.Action() {
            public String toString() {
                return "CopyrightBlockAction[copyrightText=" + copyrightBlock + "]";
            }

            public boolean evaluate(ParsedFile pfile) {
                //tag blocks
                boolean hadAnOldSunCopyright = tagBlocks(pfile, args);
                trace("Updating copyright/license header on file " + pfile.getPath());
                int countSunCopyright = 0;
                for (CommentBlock block : pfile.getComments()) {
                    if (block.hasTags(OWN_COPYRIGHT_TAG, COPYRIGHT_BLOCK_TAG)) {
                        countSunCopyright++;
                        if (countSunCopyright > 1) {
                            trace("Remove: More than one Sun/Oracle Copyright Block " + pfile.getPath());
                            pfile.remove(block);
                            continue;
                        }
                        if (block.hasTag(CommentBlock.TOP_COMMENT_BLOCK)) {
                            //if (!(ToolUtil.areCommentsSimilar(copyrightBlock.contents(), block.comment()))) {
                            if (!(ToolUtil.doesCopyrightMatch(copyrightTemplateBlock.contents(), block.comment()))) {
                                // It should entirely match copyrightText
                                trace("Replace: First block has incorrect copyright text " + pfile.getPath());
                                pfile.remove(block);
                                pfile.insertCommentBlock(fixCopyright(copyrightBlock.contents(), block.getCopyright(), args, getLastModifiedDate(args, pfile)));
                            }
                        } else {
                            trace("Move: Sun/Oracle Copyright Block is not the first comment block" + pfile.getPath());
                            pfile.remove(block);
                            if (!(ToolUtil.doesCopyrightMatch(copyrightTemplateBlock.contents(), block.comment()))) {
                                pfile.insertCommentBlock(fixCopyright(copyrightBlock.contents(), block.getCopyright(), args, getLastModifiedDate(args, pfile)));
                            } else {
                                pfile.insertCommentBlock(block.comment());
                            }
                        }
                    } else {
                        //if empty comment block, remove it.
                        if (args.options().contains("checkEmpty") && isEmpty(block.comment())) {
                            trace("Remove: empty comment block in" + pfile.getPath());
                            pfile.remove(block);
                        }

                    }
                }
                if (!hadAnOldSunCopyright) {
                    trace("Insert: No Sun/Oracle Copyright header in " + pfile.getPath());
                    pfile.insertCommentBlock(fixCopyright(copyrightBlock.contents(),null, args, getLastModifiedDate(args, pfile)));
                }

                try {
                    pfile.write();
                } catch (IOException exc) {
                    trace("Exception while processing file " + pfile.getPath() + ": " + exc);
                    exc.printStackTrace();
                    return false;
                }
                return true;
            }

            private boolean isEmpty(String content) {
                return content.trim().equals("");
            }
        };
    }

    private String getLastModifiedDate(LicenseTool.Arguments args, ParsedFile pfile) {
        String lastModified = null;
        if (args != null && args.uselastmodified() && pfile.getVCS() != null) {
            lastModified = pfile.getVCS().getLastModifiedYear(pfile.getPath());
        }
        return lastModified;
    }

    //TODO Use file last changed date
    private String fixCopyright(String cr_text, CommentBlock.Copyright copyright, LicenseTool.Arguments args, String lastModified) {
        StringBuilder sb = new StringBuilder();

        String startYear = null;
        String endYear = null;
        //Use default start year, end year if there is no existing copyright
        if(copyright == null) {
            startYear = args.startyear();
            endYear=args.endyear();
        }
        
        if(startYear != null && startYear.equals("")) {
            startYear = null;
        }
        if(endYear == null || (lastModified != null && lastModified.compareTo(endYear) > 0)) {
            endYear = lastModified;
        }

        if(copyright != null) {
            if(startYear == null || (copyright.getStartYear() != null && copyright.getStartYear().compareTo(startYear) < 0)) {
                startYear = copyright.getStartYear();
            }
            if(endYear== null || (copyright.getEndYear() != null && copyright.getEndYear().compareTo(endYear) > 0)) {
                endYear = copyright.getEndYear();
            }
        }
        if(startYear == null || startYear.equals("")) {
            //TODO throw validation exception
        }
        for(String line: ToolUtil.splitToLines(cr_text)) {
            if(line.contains("YYYY ")) {
                String years = null;
                if(endYear != null && !endYear.equals("") && endYear.compareTo(startYear) > 0) {
                    years = startYear+"-"+endYear+" ";
                } else {
                    years = startYear+" ";
                }

                line = line.replace("YYYY ", years);
            } else if(line.contains("YYYY, ")) {
                String years = startYear+", ";
                if(endYear != null && !endYear.equals("") && endYear.compareTo(startYear) > 0) {
                    years = years+endYear+", ";
                }
                line = line.replace("YYYY, ", years);
            }
            sb.append(line);
        }

        return sb.toString();
    }

    //Just delete the original file and rewrite it to test if parsing and writing back works correctly.

    public Scanner.Action getReWriteCopyrightAction() {
        return new Scanner.Action() {
            public boolean evaluate(ParsedFile pfile) {
                try {
                    pfile.write();
                } catch (IOException exc) {
                    trace("Exception while processing file " + pfile.getPath() + ": " + exc);
                    exc.printStackTrace();
                    return false;
                }
                return true;
            }
        };
    }

    private boolean tagBlocks(ParsedFile pfile, LicenseTool.Arguments args) {
        boolean hadAnOldSunCopyright = false;
        String LICENSOR_KEYWORD = "licensor:";
        List<String> own_licensors = new ArrayList<String>();
        for(String option:args.options()) {
            if(option.startsWith(LICENSOR_KEYWORD)) {
                own_licensors.add(option.substring(LICENSOR_KEYWORD.length()));
            }
        }
        // Tag blocks
        for (CommentBlock cb : pfile.getComments()) {
            CopyrightParser.parseCopyright(cb, pfile);
            if(cb.hasTag(COPYRIGHT_BLOCK_TAG)) {
                String cddl = cb.find("CDDL");
                if (cddl != null) {
                    cb.addTag("CDDL_TAG");
                }
                CommentBlock.Copyright cr = cb.getCopyright();
                if (cr != null && cr.getLicensor() != null) {
                    if (cr.getLicensor().contains("Sun") ||cr.getLicensor().contains("Oracle") ) {
                        cb.addTag(OWN_COPYRIGHT_TAG);
                        hadAnOldSunCopyright = true;
                    }

                    for(String licensor: own_licensors) {
                        if(cr.getLicensor().contains(licensor)) {
                            cb.addTag(OWN_COPYRIGHT_TAG);
                            hadAnOldSunCopyright = true;    
                        }
                    }

                }
            }
        }

        /*
        trace("copyrightBlockAction: blocks in file " + pfile.getPath());
        for (Block block : pfile.getFileBlocks()) {
            traceBlock(block);
        }
        */
        return hadAnOldSunCopyright;
    }

    private void trace(String msg) {
        LOGGER.fine(msg);
    }

    private void validationError(Block block, String msg, String fw) {
        LOGGER.warning("Copyright validation error: " + msg + " for " + fw);
        if (block != null) {
            traceBlock(block);
        }
    }

    private void traceBlock(Block block) {

        LOGGER.fine("Block=" + block);
        LOGGER.fine("Block contents:");
        if (block instanceof PlainBlock) {
            LOGGER.fine(((PlainBlock) block).contents());
        } else if (block instanceof CommentBlock) {
            LOGGER.fine(((CommentBlock) block).contents());
        }

    }
    private static final Logger LOGGER = Logger.getLogger(ActionFactory.class.getName());
}