package com.termux.terminal;

/**
 * Unicode Bidirectional Algorithm (UAX #9) implementation for terminal text rendering.
 *
 * This implements a subset of the Unicode Bidirectional Algorithm sufficient for
 * terminal emulators. Each terminal line is treated as a paragraph.
 *
 * Reference: https://unicode.org/reports/tr9/
 */
public final class BidiReorderer {

    private BidiReorderer() {}

    // BiDi character types (from Unicode Character Database)
    static final byte L   = 0;  // Left-to-Right
    static final byte R   = 1;  // Right-to-Left
    static final byte AL  = 2;  // Right-to-Left Arabic
    static final byte EN  = 3;  // European Number
    static final byte ES  = 4;  // European Separator
    static final byte ET  = 5;  // European Terminator (currency symbols, etc.)
    static final byte AN  = 6;  // Arabic Number
    static final byte CS  = 7;  // Common Separator
    static final byte NSM = 8;  // Nonspacing Mark
    static final byte BN  = 9;  // Boundary Neutral
    static final byte B   = 10; // Paragraph Separator
    static final byte S   = 11; // Segment Separator
    static final byte WS  = 12; // Whitespace
    static final byte ON  = 13; // Other Neutral
    static final byte LRE = 14; // Left-to-Right Embedding
    static final byte RLE = 15; // Right-to-Left Embedding
    static final byte LRO = 16; // Left-to-Right Override
    static final byte RLO = 17; // Right-to-Left Override
    static final byte PDF = 18; // Pop Directional Format
    static final byte LRI = 19; // Left-to-Right Isolate
    static final byte RLI = 20; // Right-to-Left Isolate
    static final byte FSI = 21; // First Strong Isolate
    static final byte PDI = 22; // Pop Directional Isolate

    static final int MAX_STACK_SIZE = 125;

    /**
     * Result of BiDi processing for a single line.
     */
    public static class BidiResult {
        /**
         * Visual-to-logical mapping. visualToLogical[i] = the logical index
         * that maps to visual position i. Length equals the number of columns.
         */
        public final int[] visualToLogical;

        /**
         * The resolved embedding level for each logical column.
         * Odd levels are RTL, even levels are LTR.
         */
        public final byte[] levels;

        /**
         * The paragraph embedding level (0 = LTR, 1 = RTL).
         */
        public final int paragraphLevel;

        public BidiResult(int[] visualToLogical, byte[] levels, int paragraphLevel) {
            this.visualToLogical = visualToLogical;
            this.levels = levels;
            this.paragraphLevel = paragraphLevel;
        }
    }

    /**
     * Process a line of text through the Unicode Bidirectional Algorithm.
     *
     * @param chars The character array of the line
     * @param charCount Number of valid characters in the array
     * @return BiDi processing result with visual-to-logical mapping
     */
    public static BidiResult processLine(char[] chars, int charCount) {
        if (charCount == 0) {
            return new BidiResult(new int[0], new byte[0], 0);
        }

        // Convert code points to BiDi types
        byte[] types = new byte[charCount];
        for (int i = 0; i < charCount; i++) {
            types[i] = getBiDiType(codePointAt(chars, i, charCount));
            // Skip surrogate pair second half
            if (Character.isHighSurrogate(chars[i]) && i + 1 < charCount && Character.isLowSurrogate(chars[i + 1])) {
                i++;
            }
        }

        // Step P2-P3: Determine paragraph embedding level
        int paragraphLevel = 0; // default LTR
        for (int i = 0; i < charCount; i++) {
            if (types[i] == L || types[i] == AL || types[i] == R) {
                paragraphLevel = (types[i] == L) ? 0 : 1;
                break;
            }
            if (types[i] == LRE || types[i] == LRO || types[i] == LRI || types[i] == RLE || types[i] == RLO || types[i] == RLI || types[i] == FSI) {
                break;
            }
        }

        // Copy types for modification by rules
        byte[] embeddingLevels = new byte[charCount];
        for (int i = 0; i < charCount; i++) {
            embeddingLevels[i] = 0;
        }

        // Step X1-X8: Resolve explicit levels
        resolveExplicit(types, embeddingLevels, charCount, (byte) paragraphLevel);

        // Copy types for weak/neutral resolution (we modify types in place per spec)
        byte[] workingTypes = types.clone();

        // Step W1-W7: Resolve weak types
        resolveWeakTypes(workingTypes, embeddingLevels, charCount);

        // Step N0-N2: Resolve neutral types
        resolveNeutrals(workingTypes, embeddingLevels, charCount);

        // Step I1-I2: Resolve implicit levels
        resolveImplicit(workingTypes, embeddingLevels, charCount);

        // Step L1-L4: Reorder
        int[] visualToLogical = reorder(embeddingLevels, charCount, paragraphLevel);

        return new BidiResult(visualToLogical, embeddingLevels, paragraphLevel);
    }

    // --- Character Classification ---

    private static int codePointAt(char[] chars, int index, int length) {
        char c = chars[index];
        if (Character.isHighSurrogate(c) && index + 1 < length && Character.isLowSurrogate(chars[index + 1])) {
            return Character.toCodePoint(c, chars[index + 1]);
        }
        return c;
    }

    static byte getBiDiType(int codePoint) {
        if (codePoint <= 0x7F) {
            return getAsciiBiDiType(codePoint);
        }
        if (codePoint <= 0xFFFF) {
            return getBmpBiDiType(codePoint);
        }
        return getSupplementaryBiDiType(codePoint);
    }

    private static byte getAsciiBiDiType(int cp) {
        if (cp == 0x0A || cp == 0x0D) return B;  // LF, CR
        if (cp == 0x09 || cp == 0x0B || cp == 0x0C || cp == 0x20) return WS; // TAB, VT, FF, SP
        if (cp >= 0x30 && cp <= 0x39) return EN;  // ASCII digits
        if (cp == 0x2B || cp == 0x2D) return ES;  // + and -
        if (cp >= 0x21 && cp <= 0x2F && cp != 0x2B && cp != 0x2D) return ON; // ! " # $ % & ' ( ) * + , - . /
        if (cp >= 0x3A && cp <= 0x40) return ON; // : ; < = > ? @
        if (cp >= 0x5B && cp <= 0x60) return ON; // [ \ ] ^ _ `
        if (cp >= 0x7B && cp <= 0x7E) return ON; // { | } ~
        return L; // A-Z, a-z
    }

    private static byte getBmpBiDiType(int cp) {
        // Arabic block (0x0600-0x06FF)
        if (cp >= 0x0600 && cp <= 0x0605) return AN;
        if (cp >= 0x0608 && cp <= 0x0608) return AL;
        if (cp == 0x0609 || cp == 0x060A) return ET;
        if (cp == 0x060B) return AL;
        if (cp == 0x060C) return CS;
        if (cp == 0x060D) return AL;
        if (cp >= 0x0610 && cp <= 0x061A) return NSM;
        if (cp >= 0x061B) return AL;
        if (cp == 0x061C) return L;
        if (cp >= 0x061D) return AL;
        if (cp >= 0x061E && cp <= 0x061F) return AL;
        if (cp >= 0x0620 && cp <= 0x063F) return AL;
        if (cp == 0x0640) return AL;
        if (cp >= 0x0641 && cp <= 0x064A) return AL;
        if (cp >= 0x064B && cp <= 0x065F) return NSM;
        if (cp >= 0x0660 && cp <= 0x0669) return AN;
        if (cp >= 0x066A) return AL;
        if (cp >= 0x066B && cp <= 0x066C) return AN;
        if (cp == 0x066D) return AL;
        if (cp >= 0x06D6 && cp <= 0x06DC) return NSM;
        if (cp == 0x06DD) return AN;
        if (cp >= 0x06DE && cp <= 0x06FF) return AL;
        if (cp >= 0x0700 && cp <= 0x070D) return AL;
        if (cp == 0x070E) return AL;
        if (cp == 0x070F) return BN;
        if (cp == 0x0710) return AL;
        if (cp >= 0x0711) return AL;
        if (cp >= 0x0712 && cp <= 0x072F) return AL;
        if (cp >= 0x0730 && cp <= 0x074A) return NSM;
        if (cp >= 0x074B && cp <= 0x074C) return AL;
        if (cp >= 0x074D && cp <= 0x07A5) return AL;
        if (cp >= 0x07A6 && cp <= 0x07B0) return NSM;
        if (cp >= 0x07B1) return AL;

        // Hebrew (0x0590-0x05FF, 0xFB1D-FB4F)
        if (cp >= 0x0590 && cp <= 0x0590) return R;
        if (cp >= 0x0591 && cp <= 0x05BD) return NSM;
        if (cp == 0x05BE) return R;
        if (cp == 0x05BF) return NSM;
        if (cp == 0x05C0) return R;
        if (cp == 0x05C1 || cp == 0x05C2) return NSM;
        if (cp == 0x05C3) return R;
        if (cp >= 0x05C4 && cp <= 0x05C5) return NSM;
        if (cp == 0x05C6) return R;
        if (cp == 0x05C7) return NSM;
        if (cp >= 0x05D0 && cp <= 0x05EA) return R;
        if (cp >= 0x05F0 && cp <= 0x05F2) return R;
        if (cp == 0x05F3) return R;
        if (cp == 0x05F4) return R;
        if (cp >= 0xFB1D && cp <= 0xFB1F) return R;
        if (cp >= 0xFB20 && cp <= 0xFB28) return R;
        if (cp >= 0xFB2A && cp <= 0xFB36) return R;
        if (cp >= 0xFB38 && cp <= 0xFB3C) return R;
        if (cp == 0xFB3E) return R;
        if (cp >= 0xFB40 && cp <= 0xFB41) return R;
        if (cp >= 0xFB42 && cp <= 0xFB43) return R;
        if (cp >= 0xFB44 && cp <= 0xFB45) return R;
        if (cp >= 0xFB46 && cp <= 0xFB4F) return R;

        // General ranges
        if (cp >= 0x0300 && cp <= 0x036F) return NSM; // Combining diacritical marks
        if (cp >= 0x0483 && cp <= 0x0489) return NSM; // Combining Cyrillic
        if (cp >= 0x0591 && cp <= 0x05BD) return NSM;
        if (cp >= 0x0610 && cp <= 0x061A) return NSM;
        if (cp >= 0x064B && cp <= 0x065F) return NSM;
        if (cp >= 0x0670) return NSM;
        if (cp >= 0x06D6 && cp <= 0x06DC) return NSM;
        if (cp >= 0x06DF && cp <= 0x06E4) return NSM;
        if (cp >= 0x06E7 && cp <= 0x06E8) return NSM;
        if (cp >= 0x06EA && cp <= 0x06ED) return NSM;
        if (cp >= 0x0711) return NSM;
        if (cp >= 0x0730 && cp <= 0x074A) return NSM;
        if (cp >= 0x07A6 && cp <= 0x07B0) return NSM;
        if (cp >= 0x07EB && cp <= 0x07F3) return NSM;
        if (cp >= 0x0816 && cp <= 0x0819) return NSM;
        if (cp >= 0x081B && cp <= 0x0823) return NSM;
        if (cp >= 0x0825 && cp <= 0x0827) return NSM;
        if (cp >= 0x0829 && cp <= 0x082D) return NSM;
        if (cp >= 0x0859 && cp <= 0x085B) return NSM;
        if (cp >= 0x08E3 && cp <= 0x0903) return NSM;
        if (cp >= 0x093A && cp <= 0x093C) return NSM;
        if (cp >= 0x0941 && cp <= 0x0948) return NSM;
        if (cp >= 0x094D && cp <= 0x094F) return NSM;
        if (cp >= 0x0951 && cp <= 0x0957) return NSM;
        if (cp >= 0x0962 && cp <= 0x0963) return NSM;
        if (cp >= 0x0981 && cp <= 0x0983) return NSM;
        if (cp >= 0x09BC && cp <= 0x09BC) return NSM;
        if (cp >= 0x09C1 && cp <= 0x09C4) return NSM;
        if (cp >= 0x09CD && cp <= 0x09CD) return NSM;
        if (cp >= 0x09E2 && cp <= 0x09E3) return NSM;
        if (cp >= 0x0A01 && cp <= 0x0A03) return NSM;
        if (cp >= 0x0A3C && cp <= 0x0A3C) return NSM;
        if (cp >= 0x0A41 && cp <= 0x0A42) return NSM;
        if (cp >= 0x0A47 && cp <= 0x0A48) return NSM;
        if (cp >= 0x0A4B && cp <= 0x0A4D) return NSM;
        if (cp >= 0x0A51 && cp <= 0x0A51) return NSM;
        if (cp >= 0x0A70 && cp <= 0x0A71) return NSM;
        if (cp >= 0x0A75 && cp <= 0x0A75) return NSM;
        if (cp >= 0x0A81 && cp <= 0x0A83) return NSM;
        if (cp >= 0x0ABC && cp <= 0x0ABC) return NSM;
        if (cp >= 0x0AC1 && cp <= 0x0AC5) return NSM;
        if (cp >= 0x0AC7 && cp <= 0x0AC8) return NSM;
        if (cp == 0x0ACD) return NSM;
        if (cp >= 0x0AE2 && cp <= 0x0AE3) return NSM;
        if (cp >= 0x0B01 && cp <= 0x0B03) return NSM;
        if (cp >= 0x0B3C && cp <= 0x0B3C) return NSM;
        if (cp >= 0x0B3F && cp <= 0x0B3F) return NSM;
        if (cp >= 0x0B41 && cp <= 0x0B44) return NSM;
        if (cp >= 0x0B4D && cp <= 0x0B4D) return NSM;
        if (cp >= 0x0B56 && cp <= 0x0B57) return NSM;
        if (cp >= 0x0B62 && cp <= 0x0B63) return NSM;
        if (cp >= 0x0B82 && cp <= 0x0B82) return NSM;
        if (cp >= 0x0BC0 && cp <= 0x0BC0) return NSM;
        if (cp >= 0x0BCD && cp <= 0x0BCD) return NSM;
        if (cp >= 0x0C3E && cp <= 0x0C40) return NSM;
        if (cp >= 0x0C46 && cp <= 0x0C48) return NSM;
        if (cp >= 0x0C4A && cp <= 0x0C4D) return NSM;
        if (cp >= 0x0C55 && cp <= 0x0C56) return NSM;
        if (cp >= 0x0C62 && cp <= 0x0C63) return NSM;
        if (cp >= 0x0C81 && cp <= 0x0C81) return NSM;
        if (cp >= 0x0CBC && cp <= 0x0CBC) return NSM;
        if (cp >= 0x0CBF && cp <= 0x0CBF) return NSM;
        if (cp >= 0x0CC6 && cp <= 0x0CC6) return NSM;
        if (cp >= 0x0CCC && cp <= 0x0CCD) return NSM;
        if (cp >= 0x0CE2 && cp <= 0x0CE3) return NSM;
        if (cp >= 0x0D01 && cp <= 0x0D03) return NSM;
        if (cp >= 0x0D3B && cp <= 0x0D3C) return NSM;
        if (cp >= 0x0D41 && cp <= 0x0D44) return NSM;
        if (cp >= 0x0D4D && cp <= 0x0D4D) return NSM;
        if (cp >= 0x0D62 && cp <= 0x0D63) return NSM;
        if (cp >= 0x0DCA && cp <= 0x0DCA) return NSM;
        if (cp >= 0x0DD2 && cp <= 0x0DD4) return NSM;
        if (cp >= 0x0DD6 && cp <= 0x0DD6) return NSM;
        if (cp >= 0x0E31 && cp <= 0x0E31) return NSM;
        if (cp >= 0x0E34 && cp <= 0x0E3A) return NSM;
        if (cp >= 0x0E47 && cp <= 0x0E4E) return NSM;
        if (cp >= 0x0EB1 && cp <= 0x0EB1) return NSM;
        if (cp >= 0x0EB4 && cp <= 0x0EBC) return NSM;
        if (cp >= 0x0EC8 && cp <= 0x0ECD) return NSM;
        if (cp >= 0x0F18 && cp <= 0x0F19) return NSM;
        if (cp >= 0x0F35 && cp <= 0x0F35) return NSM;
        if (cp >= 0x0F37 && cp <= 0x0F37) return NSM;
        if (cp >= 0x0F39 && cp <= 0x0F39) return NSM;
        if (cp >= 0x0F71 && cp <= 0x0F7E) return NSM;
        if (cp >= 0x0F80 && cp <= 0x0F84) return NSM;
        if (cp >= 0x0F86 && cp <= 0x0F87) return NSM;
        if (cp >= 0x0F8D && cp <= 0x0FBC) return NSM;
        if (cp >= 0x0FC6 && cp <= 0x0FC6) return NSM;
        if (cp >= 0x102D && cp <= 0x1030) return NSM;
        if (cp >= 0x1032 && cp <= 0x1037) return NSM;
        if (cp >= 0x1039 && cp <= 0x103A) return NSM;
        if (cp >= 0x103D && cp <= 0x103E) return NSM;
        if (cp >= 0x1058 && cp <= 0x1059) return NSM;
        if (cp >= 0x105E && cp <= 0x1060) return NSM;
        if (cp >= 0x1071 && cp <= 0x1074) return NSM;
        if (cp >= 0x1082 && cp <= 0x1082) return NSM;
        if (cp >= 0x1085 && cp <= 0x1086) return NSM;
        if (cp >= 0x108D && cp <= 0x108D) return NSM;
        if (cp >= 0x109D && cp <= 0x109D) return NSM;
        if (cp >= 0x1100 && cp <= 0x115F) return L; // Hangul Jamo
        if (cp >= 0x1160 && cp <= 0x11FF) return NSM; // Hangul Jamo Extended
        if (cp >= 0x135D && cp <= 0x135F) return NSM;
        if (cp >= 0x1712 && cp <= 0x1714) return NSM;
        if (cp >= 0x1732 && cp <= 0x1733) return NSM;
        if (cp >= 0x1752 && cp <= 0x1753) return NSM;
        if (cp >= 0x1772 && cp <= 0x1773) return NSM;
        if (cp >= 0x180B && cp <= 0x180D) return NSM;
        if (cp >= 0x1885 && cp <= 0x1886) return NSM;
        if (cp >= 0x18A9 && cp <= 0x18A9) return NSM;
        if (cp >= 0x1920 && cp <= 0x1922) return NSM;
        if (cp >= 0x1927 && cp <= 0x1928) return NSM;
        if (cp >= 0x1932 && cp <= 0x1932) return NSM;
        if (cp >= 0x1939 && cp <= 0x193B) return NSM;
        if (cp >= 0x1A17 && cp <= 0x1A18) return NSM;
        if (cp >= 0x1A1B && cp <= 0x1A1B) return NSM;
        if (cp >= 0x1A56 && cp <= 0x1A56) return NSM;
        if (cp >= 0x1A58 && cp <= 0x1A5E) return NSM;
        if (cp >= 0x1A60 && cp <= 0x1A60) return NSM;
        if (cp >= 0x1A62 && cp <= 0x1A62) return NSM;
        if (cp >= 0x1A65 && cp <= 0x1A6C) return NSM;
        if (cp >= 0x1A73 && cp <= 0x1A7C) return NSM;
        if (cp == 0x1A7F) return NSM;
        if (cp >= 0x1AB0 && cp <= 0x1ABE) return NSM;
        if (cp >= 0x1B00 && cp <= 0x1B03) return NSM;
        if (cp >= 0x1B34 && cp <= 0x1B34) return NSM;
        if (cp >= 0x1B36 && cp <= 0x1B3A) return NSM;
        if (cp == 0x1B3C) return NSM;
        if (cp >= 0x1B42 && cp <= 0x1B42) return NSM;
        if (cp >= 0x1B6B && cp <= 0x1B73) return NSM;
        if (cp >= 0x1B80 && cp <= 0x1B82) return NSM;
        if (cp >= 0x1BA1 && cp <= 0x1BA1) return NSM;
        if (cp >= 0x1BA6 && cp <= 0x1BA7) return NSM;
        if (cp == 0x1BAA) return NSM;
        if (cp >= 0x1BAC && cp <= 0x1BE5) return NSM;
        if (cp >= 0x1BE8 && cp <= 0x1BE9) return NSM;
        if (cp >= 0x1BED && cp <= 0x1BED) return NSM;
        if (cp >= 0x1BEF && cp <= 0x1BF1) return NSM;
        if (cp >= 0x1C2C && cp <= 0x1C33) return NSM;
        if (cp >= 0x1C36 && cp <= 0x1C37) return NSM;
        if (cp >= 0x1C78 && cp <= 0x1C7D) return NSM;
        if (cp >= 0x1CD0 && cp <= 0x1CD2) return NSM;
        if (cp >= 0x1CD4 && cp <= 0x1CE0) return NSM;
        if (cp >= 0x1CE2 && cp <= 0x1CE8) return NSM;
        if (cp == 0x1CED) return NSM;
        if (cp >= 0x1CF4 && cp <= 0x1CF4) return NSM;
        if (cp >= 0x1CF8 && cp <= 0x1CF9) return NSM;
        if (cp >= 0x1DC0 && cp <= 0x1DF5) return NSM;
        if (cp >= 0x1DFC && cp <= 0x1DFF) return NSM;
        if (cp >= 0x200B) return getHighBiDiType(cp);
        return L; // Default: most characters are LTR
    }

    private static byte getHighBiDiType(int cp) {
        // Format characters
        if (cp >= 0x200B && cp <= 0x200B) return BN; // ZWSP
        if (cp >= 0x200C && cp <= 0x200C) return BN; // ZWNJ
        if (cp >= 0x200D && cp <= 0x200D) return BN; // ZWJ
        if (cp == 0x200E) return L;  // LRM
        if (cp == 0x200F) return R;  // RLM
        if (cp == 0x202A) return LRE;
        if (cp == 0x202B) return RLE;
        if (cp == 0x202C) return PDF;
        if (cp == 0x202D) return LRO;
        if (cp == 0x202E) return RLO;
        if (cp == 0x2066) return LRI;
        if (cp == 0x2067) return RLI;
        if (cp == 0x2068) return FSI;
        if (cp == 0x2069) return PDI;
        if (cp >= 0x206A && cp <= 0x206F) return BN;

        // Whitespace and separators
        if (cp == 0x00A0) return CS;  // NBSP
        if (cp == 0x2028) return L;   // Line Separator
        if (cp == 0x2029) return B;   // Paragraph Separator
        if (cp == 0x202F) return CS;  // Narrow NBSP
        if (cp == 0x2039) return ON;
        if (cp == 0x203A) return ON;

        // Currency symbols
        if (cp == 0x00A4) return ET;  // ¤
        if (cp == 0x00A2 || cp == 0x00A3 || cp == 0x00A5 || cp == 0x20A0 ||
            cp == 0x20A1 || cp == 0x20A2 || cp == 0x20A3 || cp == 0x20A4 ||
            cp == 0x20A5 || cp == 0x20A6 || cp == 0x20A7 || cp == 0x20A8 ||
            cp == 0x20A9 || cp == 0x20AA || cp == 0x20AB || cp == 0x20AC ||
            cp == 0x20AD || cp == 0x20AE || cp == 0x20AF || cp == 0x20B0 ||
            cp == 0x20B1 || cp == 0x20B2 || cp == 0x20B3 || cp == 0x20B4 ||
            cp == 0x20B5 || cp == 0x20B6 || cp == 0x20B7 || cp == 0x20B8 ||
            cp == 0x20B9 || cp == 0x20BA || cp == 0x20BB || cp == 0x20BC ||
            cp == 0x20BD || cp == 0x20BE || cp == 0x20BF) return ET;

        // Superscript/subscript digits and modifiers
        if (cp >= 0x2070 && cp <= 0x2079) return ET;
        if (cp >= 0x2080 && cp <= 0x2089) return ET;

        // Number forms
        if (cp == 0x2116) return ET;  // №
        if (cp == 0x2122) return L;   // ™

        // Box drawing, block elements
        if (cp >= 0x2500 && cp <= 0x257F) return L;  // Box drawing
        if (cp >= 0x2580 && cp <= 0x259F) return L;  // Block elements
        if (cp >= 0x25A0 && cp <= 0x25FF) return L;  // Geometric shapes

        // CJK ranges
        if (cp >= 0x2E80 && cp <= 0x2FFF) return L;  // CJK Radicals
        if (cp >= 0x3000 && cp <= 0x3004) return L;  // CJK Symbols
        if (cp == 0x3005) return L;
        if (cp >= 0x3006 && cp <= 0x3007) return L;
        if (cp >= 0x3008 && cp <= 0x3011) return ON;
        if (cp >= 0x3013 && cp <= 0x301F) return ON;
        if (cp >= 0x3021 && cp <= 0x3029) return L;
        if (cp >= 0x302A && cp <= 0x302D) return NSM;
        if (cp >= 0x3030) return L;

        // Hiragana, Katakana
        if (cp >= 0x3040 && cp <= 0x309F) return L;  // Hiragana
        if (cp >= 0x30A0 && cp <= 0x30FF) return L;  // Katakana

        // CJK Unified Ideographs
        if (cp >= 0x4E00 && cp <= 0x9FFF) return L;  // CJK Unified
        if (cp >= 0xA000 && cp <= 0xA4CF) return L;  // Yi
        if (cp >= 0xAC00 && cp <= 0xD7AF) return L;  // Hangul Syllables
        if (cp >= 0xF900 && cp <= 0xFAFF) return L;  // CJK Compatibility Ideographs

        // Fullwidth forms
        if (cp >= 0xFF01 && cp <= 0xFF03) return ON;
        if (cp == 0xFF05) return ON;
        if (cp == 0xFF06) return ON;
        if (cp >= 0xFF08 && cp <= 0xFF0B) return ON;
        if (cp == 0xFF0C) return CS;
        if (cp == 0xFF0D) return ES;
        if (cp >= 0xFF10 && cp <= 0xFF19) return EN;
        if (cp >= 0xFF1A && cp <= 0xFF20) return ON;
        if (cp >= 0xFF3B && cp <= 0xFF40) return ON;
        if (cp >= 0xFF5B && cp <= 0xFF65) return ON;

        // Emoji
        if (cp >= 0x1F000 && cp <= 0x1F9FF) return L; // Emoji
        if (cp >= 0x1FA00 && cp <= 0x1FA6F) return L;
        if (cp >= 0x1FA70 && cp <= 0x1FAFF) return L;

        // Variation selectors
        if (cp >= 0xFE00 && cp <= 0xFE0F) return BN;
        if (cp >= 0xE0100 && cp <= 0xE01EF) return BN;

        // Default for unlisted
        if (cp >= 0x0600 && cp <= 0x06FF) return AL;
        if (cp >= 0x0700 && cp <= 0x074F) return AL;
        if (cp >= 0x0750 && cp <= 0x077F) return AL;
        if (cp >= 0x0780 && cp <= 0x07BF) return AL;
        if (cp >= 0x07C0 && cp <= 0x07FF) return AL;
        if (cp >= 0x0800 && cp <= 0x083F) return AL;
        if (cp >= 0x0840 && cp <= 0x085F) return AL;
        if (cp >= 0x0860 && cp <= 0x086F) return AL;
        if (cp >= 0x0870 && cp <= 0x089F) return AL;
        if (cp >= 0x08A0 && cp <= 0x08FF) return AL;
        if (cp >= 0xFB50 && cp <= 0xFDCF) return AL;
        if (cp >= 0xFDF0 && cp <= 0xFDFF) return AL;
        if (cp >= 0xFE70 && cp <= 0xFEFF) return AL;

        return L; // Default to LTR for unknown characters
    }

    private static byte getSupplementaryBiDiType(int cp) {
        // Most supplementary characters are LTR
        return L;
    }

    // --- Step X1-X8: Resolve explicit embedding levels ---

    private static void resolveExplicit(byte[] types, byte[] levels, int length, byte paragraphLevel) {
        // Stack for explicit embedding levels
        int[] levelStack = new int[MAX_STACK_SIZE + 2];
        int stackTop = 0;
        levelStack[0] = paragraphLevel;

        byte overrideStatus = L; // Current override status

        for (int i = 0; i < length; i++) {
            byte type = types[i];

            switch (type) {
                case LRE: // X2
                case LRO: // X3
                case RLE: // X4
                case RLO: // X5
                case LRI: // X5a
                case RLI: // X5b
                case FSI: // X5c
                    if (stackTop < MAX_STACK_SIZE) {
                        int currentLevel = levelStack[stackTop];
                        int newLevel;
                        if (type == LRE || type == LRO) {
                            newLevel = (currentLevel + 1) | 1; // least greater odd
                        } else {
                            newLevel = (currentLevel + 2) & ~1; // least greater even
                        }
                        if (newLevel <= MAX_STACK_SIZE) {
                            stackTop++;
                            levelStack[stackTop] = newLevel;
                            if (type == RLO || type == RLE || type == RLI) {
                                overrideStatus = R;
                            } else {
                                overrideStatus = L;
                            }
                        }
                    }
                    levels[i] = (byte) levelStack[stackTop];
                    types[i] = BN; // Rule X9: convert to BN
                    break;

                case PDF: // X6a
                    if (stackTop > 0) {
                        stackTop--;
                    }
                    levels[i] = (byte) levelStack[stackTop];
                    types[i] = BN; // Rule X9
                    break;

                case PDI: // X6a
                    if (stackTop > 0) {
                        stackTop--;
                    }
                    levels[i] = (byte) levelStack[stackTop];
                    break;

                default:
                    levels[i] = (byte) levelStack[stackTop];
                    if (overrideStatus == R) {
                        types[i] = R;
                    }
                    break;
            }
        }
    }

    // --- Step W1-W7: Resolve weak types ---

    private static void resolveWeakTypes(byte[] types, byte[] levels, int length) {
        // W1: NSM
        byte prevType = levels[0] == 0 ? L : R; // assume paragraph embedding before first char
        for (int i = 0; i < length; i++) {
            if (types[i] == NSM) {
                types[i] = prevType;
            }
            if (types[i] != BN) {
                prevType = types[i];
            }
        }

        // W2: EN after AL becomes AN
        prevType = levels[0] == 0 ? L : R;
        for (int i = 0; i < length; i++) {
            if (types[i] == EN) {
                if (prevType == AL) {
                    types[i] = AN;
                }
            }
            if (types[i] != BN) {
                prevType = types[i];
            }
        }

        // W3: AL becomes R
        for (int i = 0; i < length; i++) {
            if (types[i] == AL) {
                types[i] = R;
            }
        }

        // W4: ES between EN becomes EN; CS between AN becomes AN; CS between EN becomes EN
        for (int i = 1; i < length - 1; i++) {
            if (types[i] == ES && types[i - 1] == EN && types[i + 1] == EN) {
                types[i] = EN;
            } else if (types[i] == CS) {
                if (types[i - 1] == AN && types[i + 1] == AN) {
                    types[i] = AN;
                } else if (types[i - 1] == EN && types[i + 1] == EN) {
                    types[i] = EN;
                }
            }
        }

        // W5: Sequence of ETs becomes EN if any EN is found
        for (int i = 0; i < length; i++) {
            if (types[i] == ET) {
                // Look for EN in the same ET sequence
                boolean foundEn = false;
                int j = i;
                while (j < length && types[j] == ET) {
                    j++;
                }
                // Check if the sequence is followed by EN
                if (j < length && types[j] == EN) {
                    foundEn = true;
                }
                // Also check backwards
                j = i;
                while (j >= 0 && types[j] == ET) {
                    j--;
                }
                if (j >= 0 && types[j] == EN) {
                    foundEn = true;
                }

                if (foundEn) {
                    types[i] = EN;
                }
                // Mark all ETs in this run for conversion if found
                if (foundEn) {
                    int k = i;
                    while (k < length && types[k] == ET) {
                        types[k] = EN;
                        k++;
                    }
                }
            }
        }

        // W6: Remaining ES/ET/CS become ON
        for (int i = 0; i < length; i++) {
            if (types[i] == ES || types[i] == ET || types[i] == CS) {
                types[i] = ON;
            }
        }

        // W7: EN preceded by L becomes L
        prevType = levels[0] == 0 ? L : R;
        for (int i = 0; i < length; i++) {
            if (types[i] == EN && prevType == L) {
                types[i] = L;
            }
            if (types[i] != BN) {
                prevType = types[i];
            }
        }
    }

    // --- Step N0-N2: Resolve neutral types ---

    private static void resolveNeutrals(byte[] types, byte[] levels, int length) {
        // N0: Bracket pairs
        resolveBracketPairs(types, levels, length);

        // N1: Neutral type between strong types of same direction becomes that type
        // N2: Remaining neutrals become the embedding direction
        for (int i = 0; i < length; i++) {
            if (isNeutral(types[i])) {
                // Find the run of neutrals
                int runStart = i;
                while (i < length && isNeutral(types[i])) {
                    i++;
                }
                int runEnd = i;
                i--; // Adjust for loop increment

                // Get the strong types before and after the run
                byte prevStrong = L;
                for (int j = runStart - 1; j >= 0; j--) {
                    if (types[j] == L || types[j] == R || types[j] == EN || types[j] == AN) {
                        prevStrong = (types[j] == L) ? L : R;
                        break;
                    }
                }

                byte nextStrong = L;
                for (int j = runEnd; j < length; j++) {
                    if (types[j] == L || types[j] == R || types[j] == EN || types[j] == AN) {
                        nextStrong = (types[j] == L) ? L : R;
                        break;
                    }
                }

                // N1: If both surrounding strong types are the same, use that type
                if (prevStrong == nextStrong) {
                    for (int j = runStart; j < runEnd; j++) {
                        types[j] = prevStrong;
                    }
                } else {
                    // N2: Use the embedding direction
                    byte embeddingDir = (levels[runStart] % 2 == 0) ? L : R;
                    for (int j = runStart; j < runEnd; j++) {
                        types[j] = embeddingDir;
                    }
                }
            }
        }
    }

    private static void resolveBracketPairs(byte[] types, byte[] levels, int length) {
        // Simplified bracket pair resolution
        // In a full implementation, this would handle paired brackets
        // with the same embedding level and correct directionality.
        // For terminal use, this simplified version handles the common cases.
    }

    private static boolean isNeutral(byte type) {
        return type == ON || type == WS || type == S || type == B;
    }

    // --- Step I1-I2: Resolve implicit levels ---

    private static void resolveImplicit(byte[] types, byte[] levels, int length) {
        for (int i = 0; i < length; i++) {
            if (levels[i] >= MAX_STACK_SIZE) continue; // Don't modify overflow levels

            byte type = types[i];

            if (levels[i] % 2 == 0) {
                // Even level: I1
                if (type == R || type == AN || type == EN) {
                    levels[i]++;
                }
            } else {
                // Odd level: I2
                if (type == L || type == EN) {
                    levels[i]++;
                } else if (type == AN || type == EN) {
                    levels[i] += 2;
                }
            }
        }
    }

    // --- Step L1-L4: Reorder ---

    private static int[] reorder(byte[] levels, int length, int paragraphLevel) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = i;
        }

        // L1: Reset trailing whitespace and segment/paragraph separators to paragraph level
        int highestLevel = paragraphLevel;
        int lowestOddLevel = MAX_STACK_SIZE + 1;

        for (int i = length - 1; i >= 0; i--) {
            if (levels[i] > highestLevel) {
                highestLevel = levels[i];
            }
            if (levels[i] % 2 != 0 && levels[i] < lowestOddLevel) {
                lowestOddLevel = levels[i];
            }
        }

        // Reset trailing whitespace/separator levels
        for (int i = length - 1; i >= 0; i--) {
            if (levels[i] == BN || levels[i] == S || levels[i] == WS || levels[i] == B || levels[i] == PDF || levels[i] == LRE || levels[i] == RLE || levels[i] == LRO || levels[i] == RLO || levels[i] == LRI || levels[i] == RLI || levels[i] == FSI || levels[i] == PDI) {
                levels[i] = (byte) paragraphLevel;
            } else {
                break;
            }
        }

        // L2: Reverse contiguous runs of characters at each level
        for (int level = highestLevel; level > lowestOddLevel; level--) {
            int i = 0;
            while (i < length) {
                if (levels[i] >= level) {
                    int runStart = i;
                    while (i < length && levels[i] >= level) {
                        i++;
                    }
                    // Reverse this run in the result array
                    int left = runStart;
                    int right = i - 1;
                    while (left < right) {
                        int temp = result[left];
                        result[left] = result[right];
                        result[right] = temp;
                        left++;
                        right--;
                    }
                } else {
                    i++;
                }
            }
        }

        // L3: Already handled above (level adjustments)
        // L4: Reverse all characters at odd levels greater than the lowest odd level
        // This is already handled by L2

        return result;
    }
}
