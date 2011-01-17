/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CIF.java
 * Input/output tool: CIF input
 * Original C CIF Parser (front end) by Robert W. Hon, Schlumberger Palo Alto Research
 * and its interface to C Electric (back end) by Robert Winstanley, University of Calgary.
 * Translated into Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.api.minarea;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PushbackInputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class reads files in CIF files.
 */
public class CIF {

    /** max value that can add extra digit */
    private static final int BIGSIGNED = ((0X7FFFFFFF - 9) / 10);
    //	specific syntax errors
    private static final int NOERROR = 100;
    private static final int NUMTOOBIG = 101;
    private static final int NOUNSIGNED = 102;
    private static final int NOSIGNED = 103;
    private static final int NOSEMI = 104;
    private static final int NOPATH = 105;
    private static final int BADTRANS = 106;
    private static final int BADUSER = 107;
    private static final int BADCOMMAND = 108;
    private static final int INTERNAL = 109;
    private static final int BADDEF = 110;
    private static final int NOLAYER = 111;
    private static final int BADCOMMENT = 112;
    private static final int BADAXIS = 113;
    private static final int NESTDEF = 114;
    private static final int NESTDD = 115;
    private static final int NODEFSTART = 116;
    private static final int NESTEND = 117;
    private static final int NOSPACE = 118;
    private static final int NONAME = 119;

    // error codes for reporting errors
    private static final int FATALINTERNAL = 0;
    private static final int FATALSYNTAX = 1;
    private static final int FATALSEMANTIC = 2;
    private static final int FATALOUTPUT = 3;
    private static final int ADVISORY = 4;
//	private static final int OTHER         = 5;			/* OTHER must be last */

    /** flag for error encountered */
    private boolean errorFound;
    /** what it was */
    private int errorType;
    /** definition in progress flag */
    private boolean isInCellDefinition;
    /** end command flag */
    private boolean endIsSeen;
    /** number of chars in buffer */
    private int charactersRead;
    /** flag to reset buffer */
    private boolean resetInputBuffer;
    /** number of "fatal" errors */
    private int numFatalErrors;
    /** lookahead character */
    private int nextInputCharacter;
    /** the line being read */
    private StringBuilder inputBuffer;
    
    private CIFActions c1;

    /**
     * Creates a new instance of CIF.
     */
    CIF(CIFActions c1) {
        this.c1 = c1;
    }

    /**
     * Method to import a library from disk.
     * @param lib the library to fill
     * @param currentCells this map will be filled with currentCells in Libraries found in library file
     * @return the created library (null on error).
     */
    protected boolean importALibrary() {
        setProgressNote("Reading CIF file");

        if (initFind()) {
            return false;
        }

        // parse the CIF and create a listing
        if (interpret()) {
            return false;
        }

        // clean up
        c1.doneInterpreter();

        return true;
    }

    private boolean interpret() {
        initParser();
        c1.initInterpreter();
        inFromFile();
        parseFile();		// read in the CIF
        doneParser();

        if (numFatalErrors > 0) {
            return true;
        }

        return false;
    }

    private boolean initFind() {
        return false;
    }

    private void initParser() {
        errorFound = false;
        errorType = NOERROR;
        isInCellDefinition = false;
        endIsSeen = false;
        initInput();
        initErrors();
    }

    private void doneParser() {
        if (!endIsSeen) {
            errorReport("missing End command", FATALSYNTAX);
        }
    }

    private int parseFile() {
        int comCount = 1;
        for (;;) {
            boolean end = parseStatement();
            if (end) {
                break;
            }
            comCount++;
        }
        return comCount;
    }

    private void initInput() {
        charactersRead = 0;
        resetInputBuffer = true;
    }

    private void initErrors() {
        numFatalErrors = 0;
    }

    private void inFromFile() {
        try {
            nextInputCharacter = lineReader.read();
            updateProgressDialog(1);
        } catch (IOException e) {
            nextInputCharacter = -1;
        }
    }

    private char getNextCharacter() {
        if (resetInputBuffer) {
            resetInputBuffer = false;
            inputBuffer = new StringBuilder();
            charactersRead = 0;
        }

        int c = nextInputCharacter;
        if (c >= 0) {
            if (c != '\n') {
                charactersRead++;
                inputBuffer.append((char) c);
            } else {
                resetInputBuffer = true;
            }
            try {
                nextInputCharacter = lineReader.read();
                updateProgressDialog(1);
            } catch (IOException e) {
                nextInputCharacter = -1;
            }
        }
        return (char) c;
    }

    private char peekNextCharacter() {
        return (char) nextInputCharacter;
    }

    private boolean atEndOfFile() {
        return nextInputCharacter < 0;
    }

    private int flushInput(char breakchar) {
        int c;
        while ((c = peekNextCharacter()) >= 0 && c != breakchar) {
            getNextCharacter();
        }
        return c;
    }

    private void skipBlanks() {
        for (;;) {
            if (atEndOfFile()) {
                break;
            }
            int c = peekNextCharacter();
            if (isAsciiDigit((char) c) || Character.isUpperCase((char) c)) {
                break;
            }
            if (c == '(' || c == ')' || c == ';' || c == '-') {
                break;
            }
            getNextCharacter();
        }
    }

    private boolean parseStatement() {
        if (atEndOfFile()) {
            return true;
        }

        skipBlanks();		// flush initial junk

        int curChar = getNextCharacter();
        int command = 0;
        int xRotate = 0, yRotate = 0, length = 0, width = 0, diameter = 0, symbolNumber = 0, multiplier = 0, divisor = 0, userCommand = 0;
        Point center = null, namePoint = null;
        String lName = null, nameText = null, userText = null;
        switch (curChar) {
            case 'P':
                getPath();
                if (errorFound) {
                    return reportError();
                }
                c1.makePolygon();
                break;

            case 'B':
                xRotate = 1;
                yRotate = 0;
                length = getNumber();
                if (errorFound) {
                    return reportError();
                }
                width = getNumber();
                if (errorFound) {
                    return reportError();
                }
                center = getPoint();
                if (errorFound) {
                    return reportError();
                }
                skipSeparators();
                if (((curChar = peekNextCharacter()) >= '0' && curChar <= '9') || curChar == '-') {
                    xRotate = getSignedInteger();
                    if (errorFound) {
                        return reportError();
                    }
                    yRotate = getSignedInteger();
                    if (errorFound) {
                        return reportError();
                    }
                }
                c1.makeBox(length, width, center, xRotate, yRotate);
                break;

            case 'R':
                diameter = getNumber();
                if (errorFound) {
                    return reportError();
                }
                center = getPoint();
                if (errorFound) {
                    return reportError();
                }
                c1.makeFlash(diameter, center);
                break;

            case 'W':
                width = getNumber();
                if (errorFound) {
                    return reportError();
                }
                getPath();
                if (errorFound) {
                    return reportError();
                }
                c1.makeWire(width);
                break;

            case 'L':
                skipBlanks();
                StringBuilder layerName = new StringBuilder();
                for (;;) //				for (int i = 0; i<4; i++)
                {
                    int chr = peekNextCharacter();
                    if (!Character.isUpperCase((char) chr) && !isAsciiDigit((char) chr)) {
                        break;
                    }
                    layerName.append(getNextCharacter());
                }
                if (layerName.length() == 0) {
                    errorFound = true;
                    errorType = NOLAYER;
                    return reportError();
                }
                lName = layerName.toString();
                c1.makeLayer(lName);
                break;

            case 'D':
                skipBlanks();
                switch (getNextCharacter()) {
                    case 'S':
                        symbolNumber = getNumber();
                        if (errorFound) {
                            return reportError();
                        }
                        skipSeparators();
                        multiplier = divisor = 1;
                        if (isAsciiDigit(peekNextCharacter())) {
                            multiplier = getNumber();
                            if (errorFound) {
                                return reportError();
                            }
                            divisor = getNumber();
                            if (errorFound) {
                                return reportError();
                            }
                        }
                        if (isInCellDefinition) {
                            errorFound = true;
                            errorType = NESTDEF;
                            return reportError();
                        }
                        isInCellDefinition = true;
                        c1.makeStartDefinition(symbolNumber, multiplier, divisor);
                        break;
                    case 'F':
                        if (!isInCellDefinition) {
                            errorFound = true;
                            errorType = NODEFSTART;
                            return reportError();
                        }
                        isInCellDefinition = false;
                        c1.makeEndDefinition();
                        break;
                    case 'D':
                        symbolNumber = getNumber();
                        if (errorFound) {
                            return reportError();
                        }
                        if (isInCellDefinition) {
                            errorFound = true;
                            errorType = NESTDD;
                            return reportError();
                        }
                        c1.makeDeleteDefinition(symbolNumber);
                        break;
                    default:
                        errorFound = true;
                        errorType = BADDEF;
                        return reportError();
                }
                break;

            case 'C':
                symbolNumber = getNumber();
                if (errorFound) {
                    return reportError();
                }
                skipBlanks();
                c1.initTransform();
                for (;;) {
                    int val = peekNextCharacter();
                    if (val == ';') {
                        break;
                    }
                    switch (peekNextCharacter()) {
                        case 'T':
                            getNextCharacter();
                            int xt = getSignedInteger();
                            if (errorFound) {
                                return reportError();
                            }
                            int yt = getSignedInteger();
                            if (errorFound) {
                                return reportError();
                            }
                            c1.appendTranslate(xt, yt);
                            break;

                        case 'M':
                            getNextCharacter();
                            skipBlanks();
                            switch (getNextCharacter()) {
                                case 'X':
                                    c1.appendMirrorX();
                                    break;
                                case 'Y':
                                    c1.appendMirrorY();
                                    break;
                                default:
                                    errorFound = true;
                                    errorType = BADAXIS;
                                    return reportError();
                            }
                            break;

                        case 'R':
                            getNextCharacter();
                            int xRot = getSignedInteger();
                            if (errorFound) {
                                return reportError();
                            }
                            int yRot = getSignedInteger();
                            if (errorFound) {
                                return reportError();
                            }
                            c1.appendRotate(xRot, yRot);
                            break;

                        default:
                            errorFound = true;
                            errorType = BADTRANS;
                            return reportError();
                    }
                    skipBlanks();		// between transformation commands
                }	// end of while (1) loop
                c1.makeCall(symbolNumber, lineReader.getLineNumber());
                break;

            case '(': {
                int level = 1;
                StringBuffer comment = new StringBuffer();
                while (level != 0) {
                    curChar = getNextCharacter();
                    switch (curChar) {
                        case '(':
                            level++;
                            comment.append('(');
                            break;
                        case ')':
                            level--;
                            if (level != 0) {
                                comment.append(')');
                            }
                            break;
                        case -1:
                            errorFound = true;
                            errorType = BADCOMMENT;
                            return reportError();
                        default:
                            comment.append(curChar);
                    }
                }
            }
            // comment
            break;

            case 'E':
                skipBlanks();
                if (isInCellDefinition) {
                    errorFound = true;
                    errorType = NESTEND;
                    return reportError();
                }
                if (!atEndOfFile()) {
                    errorReport("more text follows end command", ADVISORY);
                }
                endIsSeen = true;
                c1.processEnd();
                return true;

            case ';':
                return false;

            default:
                if (isAsciiDigit((char) curChar)) {
                    userCommand = curChar - '0';
                    if (userCommand == 9) {
                        curChar = peekNextCharacter();
                        if (curChar == ' ' || curChar == '\t' || curChar == '1' || curChar == '2' || curChar == '3') {
                            switch (getNextCharacter()) {
                                case ' ':
                                case '\t':
                                    skipSpaces();
                                    nameText = parseName();
                                    if (errorFound) {
                                        return reportError();
                                    }
                                    c1.makeSymbolName(nameText);
                                    break;
                                case '1':
                                case '2':
                                case '3':
                                    if (!skipSpaces()) {
                                        errorFound = true;
                                        errorType = NOSPACE;
                                        return reportError();
                                    }
                                    nameText = parseName();
                                    if (errorFound) {
                                        return reportError();
                                    }
                                    switch (curChar) {
                                        case '1':
                                            c1.makeInstanceName(nameText);
                                            break;
                                        case '2': {
                                            namePoint = getPoint();
                                            if (errorFound) {
                                                return reportError();
                                            }
                                            skipBlanks();
                                            StringBuffer layName = new StringBuffer();
                                            for (int i = 0; i < 4; i++) {
                                                int chr = peekNextCharacter();
                                                if (!Character.isUpperCase((char) chr) && !isAsciiDigit((char) chr)) {
                                                    break;
                                                }
                                                layName.append(getNextCharacter());
                                            }
                                            lName = layName.toString();
                                            c1.makeGeomName(nameText, namePoint, lName);
                                            break;
                                        }
                                        case '3':
                                            namePoint = getPoint();
                                            if (errorFound) {
                                                return reportError();
                                            }
                                            c1.makeLabel(nameText, namePoint);
                                            break;
                                    }
                                    break;
                            }
                        } else {
                            userText = getUserText();
                            if (atEndOfFile()) {
                                errorFound = true;
                                errorType = BADUSER;
                                return reportError();
                            }
                            c1.makeUserComment(userCommand, userText);
                        }
                    } else if (userCommand == 4) {
                        curChar = peekNextCharacter();
                        if (curChar == 'I' || curChar == 'X' || curChar == 'N') {
                            switch (getNextCharacter()) {
                                case 'I':
                                case 'X':
                                case 'N':
                                    if (!skipSpaces()) {
                                        errorFound = true;
                                        errorType = NOSPACE;
                                        return reportError();
                                    }
                                    nameText = parseName();
                                    if (errorFound) {
                                        return reportError();
                                    }
                                    switch (curChar) {
                                        case 'I':
                                            c1.makeInstanceName(nameText);
                                            break;
                                        case 'X': {
                                            getSignedInteger(); // skip Index
                                            namePoint = getPoint();
                                            if (errorFound) {
                                                return reportError();
                                            }
                                            int w = getSignedInteger();
                                            getUserText();
                                            c1.makeGeomName(nameText, namePoint, null);
                                            break;
                                        }
                                        case 'N':
                                            namePoint = getPoint();
                                            if (errorFound) {
                                                return reportError();
                                            }
                                            c1.makeLabel(nameText, namePoint);
                                            break;
                                    }
                                    break;
                            }
                        } else {
                            userText = getUserText();
                            if (atEndOfFile()) {
                                errorFound = true;
                                errorType = BADUSER;
                                return reportError();
                            }
                            c1.makeUserComment(userCommand, userText);
                        }
                    } else {
                        userText = getUserText();
                        if (atEndOfFile()) {
                            errorFound = true;
                            errorType = BADUSER;
                            return reportError();
                        }
                        c1.makeUserComment(userCommand, userText);
                    }
                } else {
                    errorFound = true;
                    errorType = BADCOMMAND;
                    return reportError();
                }
        }

        // by now we have a syntactically valid command although it might be missing a semicolon
        if (!skipSemicolon()) {
            errorFound = true;
            errorType = NOSEMI;
            return reportError();
        }

        return false;
    }

    private String getUserText() {
        StringBuilder user = new StringBuilder();
        for (;;) {
            if (atEndOfFile()) {
                break;
            }
            if (peekNextCharacter() == ';') {
                break;
            }
            user.append(getNextCharacter());
        }
        return user.toString();
    }

    private String parseName() {
        StringBuilder nText = new StringBuilder();
        boolean noChar = true;
        for (;;) {
            if (atEndOfFile()) {
                break;
            }
            int c = peekNextCharacter();
            if (c == ';' || c == ' ' || c == '\t' || c == '{' || c == '}') {
                break;
            }
            noChar = false;
            getNextCharacter();
            nText.append((char) c);
        }
        if (noChar) {
            logIt(NONAME);
        }
        return nText.toString();
    }

    private int getNumber() {
        boolean somedigit = false;
        int ans = 0;
        skipSpaces();

        while (ans < BIGSIGNED && isAsciiDigit(peekNextCharacter())) {
            ans *= 10;
            ans += getNextCharacter() - '0';
            somedigit = true;
        }

        if (!somedigit) {
            logIt(NOUNSIGNED);
            return 0;
        }
        if (isAsciiDigit(peekNextCharacter())) {
            logIt(NUMTOOBIG);
            return 0XFFFFFFFF;
        }
        return ans;
    }

    private boolean skipSemicolon() {
        boolean ans = false;
        skipBlanks();
        if (peekNextCharacter() == ';') {
            getNextCharacter();
            ans = true;
            skipBlanks();
        }
        return ans;
    }

    private boolean skipSpaces() {
        boolean ans = false;
        for (;;) {
            int c = peekNextCharacter();
            if (c != ' ' && c != '\t') {
                break;
            }
            getNextCharacter();
            ans = true;
        }
        return ans;
    }

    private int getSignedInteger() {
        boolean sign = false;
        int ans = 0;
        skipSeparators();

        if (peekNextCharacter() == '-') {
            sign = true;
            getNextCharacter();
        }

        boolean someDigit = false;
        while (ans < BIGSIGNED && isAsciiDigit(peekNextCharacter())) {
            ans *= 10;
            ans += getNextCharacter() - '0';
            someDigit = true;
        }

        if (!someDigit) {
            logIt(NOSIGNED);
            return 0;
        }
        if (isAsciiDigit(peekNextCharacter())) {
            logIt(NUMTOOBIG);
            return sign ? -0X7FFFFFFF : 0X7FFFFFFF;
        }
        return sign ? -ans : ans;
    }

    private void logIt(int thing) {
        errorFound = true;
        errorType = thing;
    }

    private Point getPoint() {
        int x = getSignedInteger();
        int y = getSignedInteger();
        return new Point(x, y);
    }

    private void getPath() {
        c1.initPath();
        skipSeparators();
        boolean hasPoints = false;
        for (;;) {
            int c = peekNextCharacter();
            if (!isAsciiDigit((char) c) && c != '-') {
                break;
            }
            Point temp = getPoint();
            if (errorFound) {
                break;
            }
            hasPoints = true;
            c1.appendPoint(temp);
            skipSeparators();
        }
        if (!hasPoints) {
            logIt(NOPATH);
        }
    }

    private void skipSeparators() {
        for (;;) {
            int c = peekNextCharacter();
            switch (c) {
                case '(':
                case ')':
                case ';':
                case '-':
                case -1:
                    return;
                default:
                    if (isAsciiDigit((char) c)) {
                        return;
                    }
                    getNextCharacter();
            }
        }
    }

    private boolean reportError() {
        switch (errorType) {
            case NUMTOOBIG:
                errorReport("number too large", FATALSYNTAX);
                break;
            case NOUNSIGNED:
                errorReport("unsigned integer expected", FATALSYNTAX);
                break;
            case NOSIGNED:
                errorReport("signed integer expected", FATALSYNTAX);
                break;
            case NOSEMI:
                errorReport("missing ';' inserted", FATALSYNTAX);
                break;
            case NOPATH:
                errorReport("no points in path", FATALSYNTAX);
                break;
            case BADTRANS:
                errorReport("no such transformation command", FATALSYNTAX);
                break;
            case BADUSER:
                errorReport("end of file inside user command", FATALSYNTAX);
                break;
            case BADCOMMAND:
                errorReport("unknown command encountered", FATALSYNTAX);
                break;
            case INTERNAL:
                errorReport("parser can't find i routine", FATALINTERNAL);
                break;
            case BADDEF:
                errorReport("no such define command", FATALSYNTAX);
                break;
            case NOLAYER:
                errorReport("layer name expected", FATALSYNTAX);
                break;
            case BADCOMMENT:
                errorReport("end of file inside a comment", FATALSYNTAX);
                break;
            case BADAXIS:
                errorReport("no such axis in mirror command", FATALSYNTAX);
                break;
            case NESTDEF:
                errorReport("symbol definitions can't nest", FATALSYNTAX);
                break;
            case NODEFSTART:
                errorReport("DF without DS", FATALSYNTAX);
                break;
            case NESTDD:
                errorReport("DD can't appear inside symbol definition", FATALSYNTAX);
                break;
            case NOSPACE:
                errorReport("missing space in name command", FATALSYNTAX);
                break;
            case NONAME:
                errorReport("no name in name command", FATALSYNTAX);
                break;
            case NESTEND:
                errorReport("End command inside symbol definition", FATALSYNTAX);
                break;
            case NOERROR:
                errorReport("error signaled but not reported", FATALINTERNAL);
                break;
            default:
                errorReport("uncaught error", FATALSYNTAX);
        }
        if (errorType != INTERNAL && errorType != NOSEMI && flushInput(';') < 0) {
            errorReport("unexpected end of input file", FATALSYNTAX);
        } else {
            skipBlanks();
        }
        errorFound = false;
        errorType = NOERROR;
        return false/*SYNTAXERROR*/;
    }

    private void errorReport(String mess, int kind) {
        if (charactersRead > 0) {
            System.out.println("line " + (lineReader.getLineNumber() + (resetInputBuffer ? 0 : 1)) + ": " + inputBuffer.toString());
        }
        if (kind == FATALINTERNAL || kind == FATALINTERNAL
                || kind == FATALSEMANTIC || kind == FATALOUTPUT) {
            numFatalErrors++;
        }
        switch (kind) {
            case FATALINTERNAL:
                System.out.println("Fatal internal error: " + mess);
                break;
            case FATALSYNTAX:
                System.out.println("Syntax error: " + mess);
                break;
            case FATALSEMANTIC:
                System.out.println("Error: " + mess);
                break;
            case FATALOUTPUT:
                System.out.println("Output error: " + mess);
                break;
            case ADVISORY:
                System.out.println("Warning: " + mess);
                break;
            default:
                System.out.println(mess);
                break;
        }
    }

    // From class com.sun.electric.tool.io.input.Input
    protected static final int READ_BUFFER_SIZE = 65536;
    /** Name of the file being input. */
    protected String filePath;
    /** The raw input stream. */
    protected InputStream inputStream;
    /** The line number reader (text only). */
    protected LineNumberReader lineReader;
    /** The input stream. */
    protected PushbackInputStream pushbackInputStream;
    /** The input stream. */
    protected DataInputStream dataInputStream;
    /** The length of the file. */
    protected long fileLength;
    /** the number of bytes of data read so far */
    protected long byteCount;

    protected boolean openTextInput(URL fileURL) {
        if (openBinaryInput(fileURL)) {
            return true;
        }
        InputStreamReader is = new InputStreamReader(inputStream);
        lineReader = new LineNumberReader(is);
        return false;
    }

    protected boolean openBinaryInput(URL fileURL) {
        filePath = fileURL.getFile();

        try {
            URLConnection urlCon = fileURL.openConnection();
            String contentLength = urlCon.getHeaderField("content-length");
            fileLength = -1;
            try {
                fileLength = Long.parseLong(contentLength);
            } catch (Exception e) {
            }
            inputStream = urlCon.getInputStream();
        } catch (IOException e) {
            System.out.println("Could not find file: " + filePath);
            return true;
        }
        byteCount = 0;

        BufferedInputStream bufStrm = new BufferedInputStream(inputStream, READ_BUFFER_SIZE);
        pushbackInputStream = new PushbackInputStream(bufStrm);
        dataInputStream = new DataInputStream(pushbackInputStream);
        return false;
    }

    protected void closeInput() {
        try {
            dataInputStream = null;
            if (lineReader != null) {
                lineReader.close();
                lineReader = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
        }
    }

    protected static void setProgressNote(String msg) {
    }

    protected void updateProgressDialog(int bytesRead) {
    }
    
    private static boolean isAsciiDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }
    
    static interface CIFActions {
        void initInterpreter();
        
        void makeWire(int width/*, path*/);
        void makeStartDefinition(int symbol, int mtl, int div);
        void makeEndDefinition();
        void makeDeleteDefinition(int n);
        
        void initTransform();
        void appendTranslate(int xt, int yt);
        void appendMirrorX();
        void appendMirrorY();
        void appendRotate(int xRot, int yRot);
        
        void initPath();
        void appendPoint(Point p);
        
        void makeCall(int symbol, int lineNumber/*, transform*/);
        void makeLayer(String lName);
        void makeFlash(int diameter, Point center);
        void makePolygon(/*path*/);
        void makeBox(int length, int width, Point center, int xr, int yr);
        void makeUserComment(int command, String text);
        void makeSymbolName(String name);
        void makeInstanceName(String name);
        void makeGeomName(String name, Point pt, String lay);
        void makeLabel(String name, Point pt);
        void processEnd();
        
        void doneInterpreter();
    }
}
