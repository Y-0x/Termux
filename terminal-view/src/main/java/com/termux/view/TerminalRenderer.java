package com.termux.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;

import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalRow;
import com.termux.terminal.TextStyle;
import com.termux.terminal.WcWidth;

/**
 * Renderer of a {@link TerminalEmulator} into a {@link Canvas}.
 * <p/>
 * Saves font metrics, so needs to be recreated each time the typeface or font size changes.
 */
public final class TerminalRenderer {

    final int mTextSize;
    final Typeface mTypeface;
    private final Paint mTextPaint = new Paint();

    /** The width of a single mono spaced character obtained by {@link Paint#measureText(String)} on a single 'X'. */
    final float mFontWidth;
    /** The {@link Paint#getFontSpacing()}. See http://www.fampennings.nl/maarten/android/08numgrid/font.png */
    final int mFontLineSpacing;
    /** The {@link Paint#ascent()}. See http://www.fampennings.nl/maarten/android/08numgrid/font.png */
    private final int mFontAscent;
    /** The {@link #mFontLineSpacing} + {@link #mFontAscent}. */
    public final int mFontLineSpacingAndAscent;

    private final float[] asciiMeasures = new float[127];

    public TerminalRenderer(int textSize, Typeface typeface) {
        mTextSize = textSize;
        mTypeface = typeface;

        mTextPaint.setTypeface(typeface);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(textSize);

        mFontLineSpacing = (int) Math.ceil(mTextPaint.getFontSpacing());
        mFontAscent = (int) Math.ceil(mTextPaint.ascent());
        mFontLineSpacingAndAscent = mFontLineSpacing + mFontAscent;
        mFontWidth = mTextPaint.measureText("X");

        StringBuilder sb = new StringBuilder(" ");
        for (int i = 0; i < asciiMeasures.length; i++) {
            sb.setCharAt(0, (char) i);
            asciiMeasures[i] = mTextPaint.measureText(sb, 0, 1);
        }
    }

    /** Render the terminal to a canvas with at a specified row scroll, and an optional rectangular selection. */
    public final void render(TerminalEmulator mEmulator, Canvas canvas, int topRow,
                             int selectionY1, int selectionY2, int selectionX1, int selectionX2) {
        final boolean reverseVideo = mEmulator.isReverseVideo();
        final int endRow = topRow + mEmulator.mRows;
        final int columns = mEmulator.mColumns;
        final int cursorCol = mEmulator.getCursorCol();
        final int cursorRow = mEmulator.getCursorRow();
        final boolean cursorVisible = mEmulator.shouldCursorBeVisible();
        final TerminalBuffer screen = mEmulator.getScreen();
        final int[] palette = mEmulator.mColors.mCurrentColors;
        final int cursorShape = mEmulator.getCursorStyle();

        if (reverseVideo)
            canvas.drawColor(palette[TextStyle.COLOR_INDEX_FOREGROUND], PorterDuff.Mode.SRC);

        float heightOffset = mFontLineSpacingAndAscent;
        for (int row = topRow; row < endRow; row++) {
            heightOffset += mFontLineSpacing;

            final int cursorX = (row == cursorRow && cursorVisible) ? cursorCol : -1;
            int selx1 = -1, selx2 = -1;
            if (row >= selectionY1 && row <= selectionY2) {
                if (row == selectionY1) selx1 = selectionX1;
                selx2 = (row == selectionY2) ? selectionX2 : mEmulator.mColumns;
            }

            TerminalRow lineObject = screen.allocateFullLineIfNecessary(screen.externalToInternalRow(row));
            final char[] line = lineObject.mText;
            final int charsUsedInLine = lineObject.getSpaceUsed();

            // Get BiDi visual-to-logical mapping
            final int[] visualToLogical = lineObject.getVisualToLogicalMapping();

            if (visualToLogical.length == 0 || isIdentityMapping(visualToLogical)) {
                // No BiDi reordering needed — use fast path
                renderLineLtr(canvas, line, lineObject, palette, heightOffset, columns,
                    cursorX, cursorShape, selx1, selx2, charsUsedInLine, reverseVideo, mEmulator);
            } else {
                // BiDi reordering needed — render in visual order
                renderLineBiDi(canvas, line, lineObject, visualToLogical, palette, heightOffset,
                    columns, cursorX, cursorShape, selx1, selx2, charsUsedInLine, reverseVideo, mEmulator);
            }
        }
    }

    private boolean isIdentityMapping(int[] mapping) {
        for (int i = 0; i < mapping.length; i++) {
            if (mapping[i] != i) return false;
        }
        return true;
    }

    /** Fast path: render a line with no BiDi reordering (pure LTR). */
    private void renderLineLtr(Canvas canvas, char[] line, TerminalRow lineObject, int[] palette,
                               float heightOffset, int columns, int cursorX, int cursorShape,
                               int selx1, int selx2, int charsUsedInLine, boolean reverseVideo,
                               TerminalEmulator emulator) {
        long lastRunStyle = 0;
        boolean lastRunInsideCursor = false;
        boolean lastRunInsideSelection = false;
        int lastRunStartColumn = -1;
        int lastRunStartIndex = 0;
        boolean lastRunFontWidthMismatch = false;
        int currentCharIndex = 0;
        float measuredWidthForRun = 0.f;

        for (int column = 0; column < columns; ) {
            final char charAtIndex = line[currentCharIndex];
            final boolean charIsHighsurrogate = Character.isHighSurrogate(charAtIndex);
            final int charsForCodePoint = charIsHighsurrogate ? 2 : 1;
            final int codePoint = charIsHighsurrogate ? Character.toCodePoint(charAtIndex, line[currentCharIndex + 1]) : charAtIndex;
            final int codePointWcWidth = WcWidth.width(codePoint);
            final boolean insideCursor = (cursorX == column || (codePointWcWidth == 2 && cursorX == column + 1));
            final boolean insideSelection = column >= selx1 && column <= selx2;
            final long style = lineObject.getStyle(column);

            final float measuredCodePointWidth = (codePoint < asciiMeasures.length) ? asciiMeasures[codePoint] : mTextPaint.measureText(line,
                currentCharIndex, charsForCodePoint);
            final boolean fontWidthMismatch = Math.abs(measuredCodePointWidth / mFontWidth - codePointWcWidth) > 0.01;

            if (style != lastRunStyle || insideCursor != lastRunInsideCursor || insideSelection != lastRunInsideSelection || fontWidthMismatch || lastRunFontWidthMismatch) {
                if (column == 0) {
                    // Skip first column as there is nothing to draw, just record the current style.
                } else {
                    final int columnWidthSinceLastRun = column - lastRunStartColumn;
                    final int charsSinceLastRun = currentCharIndex - lastRunStartIndex;
                    int cursorColor = lastRunInsideCursor ? emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR] : 0;
                    boolean invertCursorTextColor = false;
                    if (lastRunInsideCursor && cursorShape == TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK) {
                        invertCursorTextColor = true;
                    }
                    drawTextRun(canvas, line, palette, heightOffset, lastRunStartColumn, columnWidthSinceLastRun,
                        lastRunStartIndex, charsSinceLastRun, measuredWidthForRun,
                        cursorColor, cursorShape, lastRunStyle, reverseVideo || invertCursorTextColor || lastRunInsideSelection);
                }
                measuredWidthForRun = 0.f;
                lastRunStyle = style;
                lastRunInsideCursor = insideCursor;
                lastRunInsideSelection = insideSelection;
                lastRunStartColumn = column;
                lastRunStartIndex = currentCharIndex;
                lastRunFontWidthMismatch = fontWidthMismatch;
            }
            measuredWidthForRun += measuredCodePointWidth;
            column += codePointWcWidth;
            currentCharIndex += charsForCodePoint;
            while (currentCharIndex < charsUsedInLine && WcWidth.width(line, currentCharIndex) <= 0) {
                currentCharIndex += Character.isHighSurrogate(line[currentCharIndex]) ? 2 : 1;
            }
        }

        final int columnWidthSinceLastRun = columns - lastRunStartColumn;
        final int charsSinceLastRun = currentCharIndex - lastRunStartIndex;
        int cursorColor = lastRunInsideCursor ? emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR] : 0;
        boolean invertCursorTextColor = false;
        if (lastRunInsideCursor && cursorShape == TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK) {
            invertCursorTextColor = true;
        }
        drawTextRun(canvas, line, palette, heightOffset, lastRunStartColumn, columnWidthSinceLastRun, lastRunStartIndex, charsSinceLastRun,
            measuredWidthForRun, cursorColor, cursorShape, lastRunStyle, reverseVideo || invertCursorTextColor || lastRunInsideSelection);
    }

    /** BiDi path: render a line with visual reordering. Draws runs as strings for proper shaping. */
    private void renderLineBiDi(Canvas canvas, char[] line, TerminalRow lineObject, int[] visualToLogical,
                                 int[] palette, float heightOffset, int columns, int cursorX, int cursorShape,
                                 int selx1, int selx2, int charsUsedInLine, boolean reverseVideo,
                                 TerminalEmulator emulator) {
        int[] logicalToCharIndex = buildLogicalToCharIndex(line, columns, charsUsedInLine);

        int visualCol = 0;
        while (visualCol < columns) {
            int runStartVisual = visualCol;
            int runStartLogical = visualToLogical[visualCol];
            if (runStartLogical < 0 || runStartLogical >= columns) { visualCol++; continue; }
            long runStyle = lineObject.getStyle(runStartLogical);

            int runDirection = 0;
            if (visualCol + 1 < columns) {
                int nextLog = visualToLogical[visualCol + 1];
                if (nextLog == runStartLogical + 1) runDirection = 1;
                else if (nextLog == runStartLogical - 1) runDirection = -1;
            }

            visualCol++;
            while (visualCol < columns) {
                int log = visualToLogical[visualCol];
                int prevLog = visualToLogical[visualCol - 1];
                if (log < 0 || log >= columns) break;
                long style = lineObject.getStyle(log);
                if (style != runStyle) break;
                boolean continues;
                if (runDirection == 1) continues = (log == prevLog + 1);
                else if (runDirection == -1) continues = (log == prevLog - 1);
                else {
                    if (log == prevLog + 1) { runDirection = 1; continues = true; }
                    else if (log == prevLog - 1) { runDirection = -1; continues = true; }
                    else continues = false;
                }
                if (!continues) break;
                visualCol++;
            }

            int runEndVisual = visualCol;
            int runEndLogical = visualToLogical[runEndVisual - 1];

            int logMin = Math.min(runStartLogical, runEndLogical);
            int logMax = Math.max(runStartLogical, runEndLogical);
            int charStart = (logMin >= 0 && logMin <= columns) ? logicalToCharIndex[logMin] : 0;
            int charEnd = (logMax + 1 >= 0 && logMax + 1 <= columns) ? logicalToCharIndex[logMax + 1] : charsUsedInLine;
            while (charEnd < charsUsedInLine && WcWidth.width(line, charEnd) <= 0) {
                charEnd += Character.isHighSurrogate(line[charEnd]) ? 2 : 1;
            }

            int numChars = charEnd - charStart;
            if (numChars <= 0 || charStart >= charsUsedInLine) continue;

            boolean isLtr = (runDirection >= 0);
            int visualWidth = runEndVisual - runStartVisual;
            float left = runStartVisual * mFontWidth;
            float right = runEndVisual * mFontWidth;
            float measuredWidth = mTextPaint.measureText(line, charStart, charEnd);

            float mes = measuredWidth / mFontWidth;
            boolean savedMatrix = false;
            if (Math.abs(mes - visualWidth) > 0.01) {
                canvas.save();
                canvas.scale(visualWidth / mes, 1.f);
                left *= mes / visualWidth;
                right *= mes / visualWidth;
                savedMatrix = true;
            }

            int foreColor = TextStyle.decodeForeColor(runStyle);
            int backColor = TextStyle.decodeBackColor(runStyle);
            int effect = TextStyle.decodeEffect(runStyle);
            boolean bold = (effect & (TextStyle.CHARACTER_ATTRIBUTE_BOLD | TextStyle.CHARACTER_ATTRIBUTE_BLINK)) != 0;
            boolean underline = (effect & TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE) != 0;
            boolean italic = (effect & TextStyle.CHARACTER_ATTRIBUTE_ITALIC) != 0;
            boolean strikeThrough = (effect & TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH) != 0;
            boolean dim = (effect & TextStyle.CHARACTER_ATTRIBUTE_DIM) != 0;

            if ((foreColor & 0xff000000) != 0xff000000) {
                if (bold && foreColor >= 0 && foreColor < 8) foreColor += 8;
                foreColor = palette[foreColor];
            }
            if ((backColor & 0xff000000) != 0xff000000) {
                backColor = palette[backColor];
            }

            boolean reverseVideoHere = reverseVideo ^ ((effect & TextStyle.CHARACTER_ATTRIBUTE_INVERSE) != 0);
            if (reverseVideoHere) {
                int tmp = foreColor; foreColor = backColor; backColor = tmp;
            }

            if (backColor != palette[TextStyle.COLOR_INDEX_BACKGROUND]) {
                mTextPaint.setColor(backColor);
                canvas.drawRect(left, heightOffset - mFontLineSpacingAndAscent + mFontAscent, right, heightOffset, mTextPaint);
            }

            boolean hasCursor = false;
            for (int v = runStartVisual; v < runEndVisual; v++) {
                int lc = visualToLogical[v];
                if (lc >= 0 && lc < columns && (cursorX == lc || (cursorX == lc + 1 && WcWidth.width(line, logicalToCharIndex[lc]) == 2))) {
                    hasCursor = true;
                    break;
                }
            }
            if (hasCursor) {
                mTextPaint.setColor(palette[TextStyle.COLOR_INDEX_CURSOR]);
                float cursorHeight = mFontLineSpacingAndAscent - mFontAscent;
                if (cursorShape == TerminalEmulator.TERMINAL_CURSOR_STYLE_UNDERLINE) cursorHeight /= 4.f;
                else if (cursorShape == TerminalEmulator.TERMINAL_CURSOR_STYLE_BAR) right -= ((right - left) * 3) / 4.f;
                canvas.drawRect(left, heightOffset - cursorHeight, right, heightOffset, mTextPaint);
            }

            if ((effect & TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE) == 0) {
                if (dim) {
                    int red = (0xFF & (foreColor >> 16)) * 2 / 3;
                    int green = (0xFF & (foreColor >> 8)) * 2 / 3;
                    int blue = (0xFF & foreColor) * 2 / 3;
                    foreColor = 0xFF000000 + (red << 16) + (green << 8) + blue;
                }

                mTextPaint.setFakeBoldText(bold);
                mTextPaint.setUnderlineText(underline);
                mTextPaint.setTextSkewX(italic ? -0.35f : 0.f);
                mTextPaint.setStrikeThruText(strikeThrough);
                mTextPaint.setColor(foreColor);

                float drawX = isLtr ? left : right;
                canvas.drawTextRun(line, charStart, numChars, charStart, numChars,
                    drawX, heightOffset - mFontLineSpacingAndAscent, !isLtr, mTextPaint);
            }

            if (savedMatrix) canvas.restore();
        }
    }

    private int[] buildLogicalToCharIndex(char[] line, int columns, int charsUsedInLine) {
        int[] logicalToCharIndex = new int[columns + 1];
        int charIndex = 0;
        int col = 0;
        while (col < columns && charIndex < charsUsedInLine) {
            logicalToCharIndex[col] = charIndex;
            char c = line[charIndex];
            boolean isHigh = Character.isHighSurrogate(c);
            int charsForCodePoint = isHigh ? 2 : 1;
            int codePoint = isHigh ? Character.toCodePoint(c, line[charIndex + 1]) : c;
            int w = WcWidth.width(codePoint);
            charIndex += charsForCodePoint;
            while (charIndex < charsUsedInLine && WcWidth.width(line, charIndex) <= 0) {
                charIndex += Character.isHighSurrogate(line[charIndex]) ? 2 : 1;
            }
            col++;
            if (w == 2 && col < columns) {
                logicalToCharIndex[col] = logicalToCharIndex[col - 1];
                col++;
            }
        }
        while (col <= columns) {
            logicalToCharIndex[col] = charsUsedInLine;
            col++;
        }
        return logicalToCharIndex;
    }

    private void drawTextRun(Canvas canvas, char[] text, int[] palette, float y, int startColumn, int runWidthColumns,
                             int startCharIndex, int runWidthChars, float mes, int cursor, int cursorStyle,
                             long textStyle, boolean reverseVideo) {
        int foreColor = TextStyle.decodeForeColor(textStyle);
        final int effect = TextStyle.decodeEffect(textStyle);
        int backColor = TextStyle.decodeBackColor(textStyle);
        final boolean bold = (effect & (TextStyle.CHARACTER_ATTRIBUTE_BOLD | TextStyle.CHARACTER_ATTRIBUTE_BLINK)) != 0;
        final boolean underline = (effect & TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE) != 0;
        final boolean italic = (effect & TextStyle.CHARACTER_ATTRIBUTE_ITALIC) != 0;
        final boolean strikeThrough = (effect & TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH) != 0;
        final boolean dim = (effect & TextStyle.CHARACTER_ATTRIBUTE_DIM) != 0;

        if ((foreColor & 0xff000000) != 0xff000000) {
            // Let bold have bright colors if applicable (one of the first 8):
            if (bold && foreColor >= 0 && foreColor < 8) foreColor += 8;
            foreColor = palette[foreColor];
        }

        if ((backColor & 0xff000000) != 0xff000000) {
            backColor = palette[backColor];
        }

        // Reverse video here if _one and only one_ of the reverse flags are set:
        final boolean reverseVideoHere = reverseVideo ^ (effect & (TextStyle.CHARACTER_ATTRIBUTE_INVERSE)) != 0;
        if (reverseVideoHere) {
            int tmp = foreColor;
            foreColor = backColor;
            backColor = tmp;
        }

        float left = startColumn * mFontWidth;
        float right = left + runWidthColumns * mFontWidth;

        mes = mes / mFontWidth;
        boolean savedMatrix = false;
        if (Math.abs(mes - runWidthColumns) > 0.01) {
            canvas.save();
            canvas.scale(runWidthColumns / mes, 1.f);
            left *= mes / runWidthColumns;
            right *= mes / runWidthColumns;
            savedMatrix = true;
        }

        if (backColor != palette[TextStyle.COLOR_INDEX_BACKGROUND]) {
            // Only draw non-default background.
            mTextPaint.setColor(backColor);
            canvas.drawRect(left, y - mFontLineSpacingAndAscent + mFontAscent, right, y, mTextPaint);
        }

        if (cursor != 0) {
            mTextPaint.setColor(cursor);
            float cursorHeight = mFontLineSpacingAndAscent - mFontAscent;
            if (cursorStyle == TerminalEmulator.TERMINAL_CURSOR_STYLE_UNDERLINE) cursorHeight /= 4.;
            else if (cursorStyle == TerminalEmulator.TERMINAL_CURSOR_STYLE_BAR) right -= ((right - left) * 3) / 4.;
            canvas.drawRect(left, y - cursorHeight, right, y, mTextPaint);
        }

        if ((effect & TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE) == 0) {
            if (dim) {
                int red = (0xFF & (foreColor >> 16));
                int green = (0xFF & (foreColor >> 8));
                int blue = (0xFF & foreColor);
                // Dim color handling used by libvte which in turn took it from xterm
                // (https://bug735245.bugzilla-attachments.gnome.org/attachment.cgi?id=284267):
                red = red * 2 / 3;
                green = green * 2 / 3;
                blue = blue * 2 / 3;
                foreColor = 0xFF000000 + (red << 16) + (green << 8) + blue;
            }

            mTextPaint.setFakeBoldText(bold);
            mTextPaint.setUnderlineText(underline);
            mTextPaint.setTextSkewX(italic ? -0.35f : 0.f);
            mTextPaint.setStrikeThruText(strikeThrough);
            mTextPaint.setColor(foreColor);

            // The text alignment is the default Paint.Align.LEFT.
            canvas.drawTextRun(text, startCharIndex, runWidthChars, startCharIndex, runWidthChars, left, y - mFontLineSpacingAndAscent, false, mTextPaint);
        }

        if (savedMatrix) canvas.restore();
    }

    public float getFontWidth() {
        return mFontWidth;
    }

    public int getFontLineSpacing() {
        return mFontLineSpacing;
    }
}
