////////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2001-2018 Checkstyle authors
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package uk.co.michaelboyles.checkstyle.annotationfilter;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Maintains a set of suppressed checks which are in the scope of the annotation names given to setAnnotation.
 */
public class AnnotationCheck extends AbstractCheck
{
    /**
     * A thread-local holder for the list of suppression entries for the last
     * file parsed.
     */
    private static final ThreadLocal<List<Entry>> ENTRIES = ThreadLocal.withInitial(LinkedList::new);
    
    private final List<String> annotations = new ArrayList<>();

    /**
     * Set the annotations to filter out.
     * @param annotations a comma-separated list of annotations
     */
    @SuppressWarnings("unused") // Used by Checkstyle
    public void setAnnotation(final String annotations)
    {
        this.annotations.addAll(Arrays.asList(annotations.split(",")));
    }
    
    /**
     * Checks for a suppression of a check with the given source name and
     * location in the last file processed.
     * @param event audit event.
     * @return whether the check with the given name is suppressed at the given
     *         source location
     */
    public static boolean isSuppressed(AuditEvent event){
        final List<Entry> entries = ENTRIES.get();
        final int line = event.getLine();
        final int column = event.getColumn();
        boolean suppressed = false;
        for (Entry entry : entries) {
            final boolean afterStart = isSuppressedAfterEventStart(line, column, entry);
            final boolean beforeEnd = isSuppressedBeforeEventEnd(line, column, entry);
            if (afterStart && beforeEnd)
            {
                suppressed = true;
                break;
            }
        }
        return suppressed;
    }

    /**
     * Checks whether suppression entry position is after the audit event occurrence position
     * in the source file.
     * @param line the line number in the source file where the event occurred.
     * @param column the column number in the source file where the event occurred.
     * @param entry suppression entry.
     * @return true if suppression entry position is after the audit event occurrence position
     *         in the source file.
     */
    private static boolean isSuppressedAfterEventStart(int line, int column, Entry entry) {
        return entry.getFirstLine() < line
                || entry.getFirstLine() == line
                && (column == 0 || entry.getFirstColumn() <= column);
    }

    /**
     * Checks whether suppression entry position is before the audit event occurrence position
     * in the source file.
     * @param line the line number in the source file where the event occurred.
     * @param column the column number in the source file where the event occurred.
     * @param entry suppression entry.
     * @return true if suppression entry position is before the audit event occurrence position
     *         in the source file.
     */
    private static boolean isSuppressedBeforeEventEnd(int line, int column, Entry entry) {
        return entry.getLastLine() > line
                || entry.getLastLine() == line && entry
                .getLastColumn() >= column;
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {TokenTypes.ANNOTATION};
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        ENTRIES.get().clear();
    }

    @Override
    public void visitToken(DetailAST ast) {
        // check whether annotation is SuppressWarnings
        // expected children: AT ( IDENT | DOT ) LPAREN <values> RPAREN
        final String identifier = removePackage(
            getIdentifier(getFirstChild(ast))
        );
        
        if (annotations.contains(identifier)) {
            final List<Entry> entries = ENTRIES.get();

            final DetailAST targetAST = getAnnotationTarget(ast);
            if (targetAST != null)
            {
                final int firstLine = targetAST.getLineNo();
                final int firstColumn = targetAST.getColumnNo();
                final DetailAST nextAST = targetAST.getNextSibling();
                final int lastLine;
                final int lastColumn;
                if (nextAST == null)
                {
                    lastLine = Integer.MAX_VALUE;
                    lastColumn = Integer.MAX_VALUE;
                }
                else
                {
                    lastLine = nextAST.getLineNo();
                    lastColumn = nextAST.getColumnNo() - 1;
                }
                entries.add(
                    new Entry(firstLine, firstColumn, lastLine, lastColumn)
                );
            }
        }
    }

    private static String removePackage(final String identifier)
    {
        if (identifier.contains("."))
        {
            String[] tokens = identifier.split(".");
            return tokens[tokens.length - 1];
        }
        return identifier;
    }

    /**
     * Get target of annotation.
     * @param ast the AST node to get the child of
     * @return get target of annotation
     */
    private static DetailAST getAnnotationTarget(DetailAST ast) {
        final DetailAST targetAST;
        final DetailAST parentAST = ast.getParent();
        switch (parentAST.getType()) {
            case TokenTypes.MODIFIERS:
            case TokenTypes.ANNOTATIONS:
                targetAST = getAcceptableParent(parentAST);
                break;
            default:
                // unexpected container type
                throw new IllegalArgumentException("Unexpected container AST: " + parentAST);
        }
        return targetAST;
    }

    /**
     * Returns parent of given ast if parent has one of the following types:
     * ANNOTATION_DEF, PACKAGE_DEF, CLASS_DEF, ENUM_DEF, ENUM_CONSTANT_DEF, CTOR_DEF,
     * METHOD_DEF, PARAMETER_DEF, VARIABLE_DEF, ANNOTATION_FIELD_DEF, TYPE, LITERAL_NEW,
     * LITERAL_THROWS, TYPE_ARGUMENT, IMPLEMENTS_CLAUSE, DOT.
     * @param child an ast
     * @return returns ast - parent of given
     */
    private static DetailAST getAcceptableParent(DetailAST child) {
        final DetailAST result;
        final DetailAST parent = child.getParent();
        switch (parent.getType()) {
            case TokenTypes.ANNOTATION_DEF:
            case TokenTypes.PACKAGE_DEF:
            case TokenTypes.CLASS_DEF:
            case TokenTypes.INTERFACE_DEF:
            case TokenTypes.ENUM_DEF:
            case TokenTypes.ENUM_CONSTANT_DEF:
            case TokenTypes.CTOR_DEF:
            case TokenTypes.METHOD_DEF:
            case TokenTypes.PARAMETER_DEF:
            case TokenTypes.VARIABLE_DEF:
            case TokenTypes.ANNOTATION_FIELD_DEF:
            case TokenTypes.TYPE:
            case TokenTypes.LITERAL_NEW:
            case TokenTypes.LITERAL_THROWS:
            case TokenTypes.TYPE_ARGUMENT:
            case TokenTypes.IMPLEMENTS_CLAUSE:
            case TokenTypes.DOT:
                result = parent;
                break;
            default:
                // it's possible case, but shouldn't be processed here
                result = null;
        }
        return result;
    }

    /**
     * Returns the n'th child of an AST node.
     * @param ast the AST node to get the child of
     * @return the n'th child of the given AST node, or {@code null} if none
     */
    private static DetailAST getFirstChild(DetailAST ast) {
        DetailAST child = ast.getFirstChild();
        return child == null ? null : child.getNextSibling();
    }

    /**
     * Returns the Java identifier represented by an AST.
     * @param ast an AST node for an IDENT or DOT
     * @return the Java identifier represented by the given AST subtree
     * @throws IllegalArgumentException if the AST is invalid
     */
    private static String getIdentifier(DetailAST ast) {
        if (ast == null) {
            throw new IllegalArgumentException("Identifier AST expected, but get null.");
        }
        final String identifier;
        if (ast.getType() == TokenTypes.IDENT) {
            identifier = ast.getText();
        }
        else {
            identifier = getIdentifier(ast.getFirstChild()) + "."
                    + getIdentifier(ast.getLastChild());
        }
        return identifier;
    }

    /** Records a particular suppression for a region of a file. */
    private static class Entry {
        
        /** The suppression region for the check - first line. */
        private final int firstLine;
        /** The suppression region for the check - first column. */
        private final int firstColumn;
        /** The suppression region for the check - last line. */
        private final int lastLine;
        /** The suppression region for the check - last column. */
        private final int lastColumn;

        /**
         * Constructs a new suppression region entry.
         * @param firstLine the first line of the suppression region
         * @param firstColumn the first column of the suppression region
         * @param lastLine the last line of the suppression region
         * @param lastColumn the last column of the suppression region
         */
        Entry(int firstLine, int firstColumn, int lastLine, int lastColumn) {
            this.firstLine = firstLine;
            this.firstColumn = firstColumn;
            this.lastLine = lastLine;
            this.lastColumn = lastColumn;
        }

        /**
         * Gets the first line of the suppression region.
         * @return the first line of the suppression region
         */
        int getFirstLine() {
            return firstLine;
        }

        /**
         * Gets the first column of the suppression region.
         * @return the first column of the suppression region
         */
        int getFirstColumn() {
            return firstColumn;
        }

        /**
         * Gets the last line of the suppression region.
         * @return the last line of the suppression region
         */
        int getLastLine() {
            return lastLine;
        }

        /**
         * Gets the last column of the suppression region.
         * @return the last column of the suppression region
         */
        int getLastColumn() {
            return lastColumn;
        }
    }
}