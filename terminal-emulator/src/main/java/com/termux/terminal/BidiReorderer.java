package com.termux.terminal;

/**
 * Unicode Bidirectional Algorithm (UAX #9) implementation for terminal text rendering.
 *
 * Operates on terminal columns (not Java chars). Each column has one code point.
 * The algorithm determines the visual ordering of columns for display.
 *
 * Reference: https://unicode.org/reports/tr9/
 */
public final class BidiReorderer {

    private BidiReorderer() {}

    static final byte L   = 0;
    static final byte R   = 1;
    static final byte AL  = 2;
    static final byte EN  = 3;
    static final byte ES  = 4;
    static final byte ET  = 5;
    static final byte AN  = 6;
    static final byte CS  = 7;
    static final byte NSM = 8;
    static final byte BN  = 9;
    static final byte B   = 10;
    static final byte S   = 11;
    static final byte WS  = 12;
    static final byte ON  = 13;
    static final byte LRE = 14;
    static final byte RLE = 15;
    static final byte LRO = 16;
    static final byte RLO = 17;
    static final byte PDF = 18;
    static final byte LRI = 19;
    static final byte RLI = 20;
    static final byte FSI = 21;
    static final byte PDI = 22;

    static final int MAX_STACK_SIZE = 125;

    public static class BidiResult {
        public final int[] visualToLogical;
        public final byte[] levels;
        public final int paragraphLevel;

        public BidiResult(int[] visualToLogical, byte[] levels, int paragraphLevel) {
            this.visualToLogical = visualToLogical;
            this.levels = levels;
            this.paragraphLevel = paragraphLevel;
        }
    }

    /**
     * Process a terminal line through the BiDi algorithm.
     *
     * @param codePoints One code point per terminal column
     * @param columns Number of columns
     * @param widths Width of each column (1 or 2, from WcWidth)
     * @return BiDi result with column-level visual-to-logical mapping
     */
    public static BidiResult processLine(int[] codePoints, int columns, int[] widths) {
        if (columns == 0) {
            return new BidiResult(new int[0], new byte[0], 0);
        }

        byte[] origTypes = new byte[columns];
        for (int i = 0; i < columns; i++) {
            origTypes[i] = (codePoints[i] > 0) ? getBiDiType(codePoints[i]) : WS;
        }

        // P2-P3: Determine paragraph level from first strong character
        int paragraphLevel = 0;
        for (int i = 0; i < columns; i++) {
            if (origTypes[i] == L) { paragraphLevel = 0; break; }
            if (origTypes[i] == R || origTypes[i] == AL) { paragraphLevel = 1; break; }
            if (origTypes[i] == LRE || origTypes[i] == LRO || origTypes[i] == LRI ||
                origTypes[i] == RLE || origTypes[i] == RLO || origTypes[i] == RLI || origTypes[i] == FSI) {
                break;
            }
            if (origTypes[i] == PDI) break;
        }

        byte[] levels = new byte[columns];
        byte[] workingTypes = origTypes.clone();

        // X1-X8: Resolve explicit levels
        resolveExplicit(workingTypes, levels, columns, (byte) paragraphLevel);

        // W1-W7: Resolve weak types
        resolveWeakTypes(workingTypes, levels, columns);

        // N0-N2: Resolve neutral types
        resolveNeutrals(workingTypes, levels, columns);

        // I1-I2: Resolve implicit levels
        resolveImplicit(workingTypes, levels, columns);

        // L1-L4: Reorder
        int[] visualToLogical = reorder(levels, origTypes, columns, paragraphLevel);

        return new BidiResult(visualToLogical, levels, paragraphLevel);
    }

    static byte getBiDiType(int codePoint) {
        if (codePoint <= 0x7F) return getAsciiBiDiType(codePoint);
        if (codePoint <= 0xFFFF) return getBmpBiDiType(codePoint);
        return getSupplementaryBiDiType(codePoint);
    }

    private static byte getAsciiBiDiType(int cp) {
        if (cp == 0x0A || cp == 0x0D) return B;
        if (cp == 0x09 || cp == 0x0B || cp == 0x0C || cp == 0x20) return WS;
        if (cp >= 0x30 && cp <= 0x39) return EN;
        if (cp == 0x2B || cp == 0x2D) return ES;
        if (cp == 0x2C || cp == 0x2E || cp == 0x3A || cp == 0x3B) return CS;
        return L;
    }

    private static byte getBmpBiDiType(int cp) {
        // Arabic block
        if (cp >= 0x0600 && cp <= 0x0605) return AN;
        if (cp == 0x0608) return AL;
        if (cp == 0x0609 || cp == 0x060A) return ET;
        if (cp == 0x060B) return AL;
        if (cp == 0x060C) return CS;
        if (cp == 0x060D) return AL;
        if (cp >= 0x0610 && cp <= 0x061A) return NSM;
        if (cp == 0x061B) return AL;
        if (cp == 0x061C) return L;
        if (cp == 0x061D) return AL;
        if (cp == 0x061E || cp == 0x061F) return AL;
        if (cp >= 0x0620 && cp <= 0x063F) return AL;
        if (cp == 0x0640) return AL;
        if (cp >= 0x0641 && cp <= 0x064A) return AL;
        if (cp >= 0x064B && cp <= 0x065F) return NSM;
        if (cp >= 0x0660 && cp <= 0x0669) return AN;
        if (cp == 0x066A) return AL;
        if (cp == 0x066B || cp == 0x066C) return AN;
        if (cp == 0x066D) return AL;
        if (cp >= 0x06D6 && cp <= 0x06DC) return NSM;
        if (cp == 0x06DD) return AN;
        if (cp >= 0x06DE && cp <= 0x06FF) return AL;
        if (cp >= 0x0700 && cp <= 0x070D) return AL;
        if (cp == 0x070E) return AL;
        if (cp == 0x070F) return BN;
        if (cp == 0x0710) return AL;
        if (cp == 0x0711) return AL;
        if (cp >= 0x0712 && cp <= 0x072F) return AL;
        if (cp >= 0x0730 && cp <= 0x074A) return NSM;
        if (cp == 0x074B || cp == 0x074C) return AL;
        if (cp >= 0x074D && cp <= 0x07A5) return AL;
        if (cp >= 0x07A6 && cp <= 0x07B0) return NSM;
        if (cp >= 0x07B1) return AL;

        // Hebrew
        if (cp == 0x0590) return R;
        if (cp >= 0x0591 && cp <= 0x05BD) return NSM;
        if (cp == 0x05BE) return R;
        if (cp == 0x05BF) return NSM;
        if (cp == 0x05C0) return R;
        if (cp == 0x05C1 || cp == 0x05C2) return NSM;
        if (cp == 0x05C3) return R;
        if (cp == 0x05C4 || cp == 0x05C5) return NSM;
        if (cp == 0x05C6) return R;
        if (cp == 0x05C7) return NSM;
        if (cp >= 0x05D0 && cp <= 0x05EA) return R;
        if (cp >= 0x05F0 && cp <= 0x05F4) return R;
        if (cp >= 0xFB1D && cp <= 0xFB4F) return R;

        // General combining marks
        if (cp >= 0x0300 && cp <= 0x036F) return NSM;
        if (cp >= 0x0483 && cp <= 0x0489) return NSM;
        if (cp >= 0x0670) {
            if (cp <= 0x0670) return NSM;
        }
        if (cp >= 0x06DF && cp <= 0x06E4) return NSM;
        if (cp >= 0x06E7 && cp <= 0x06E8) return NSM;
        if (cp >= 0x06EA && cp <= 0x06ED) return NSM;

        // Syriac combining marks (must come before AL catch-all)
        if (cp >= 0x0730 && cp <= 0x074A) return NSM;

        // Thaana combining marks (must come before AL catch-all)
        if (cp >= 0x07A6 && cp <= 0x07B0) return NSM;

        // NKo, Samaritan, Mandaic combining marks
        if (cp >= 0x07EB && cp <= 0x07F3) return NSM;
        if (cp >= 0x0816 && cp <= 0x0819) return NSM;
        if (cp >= 0x081B && cp <= 0x0823) return NSM;
        if (cp >= 0x0825 && cp <= 0x0827) return NSM;
        if (cp >= 0x0829 && cp <= 0x082D) return NSM;
        if (cp >= 0x0859 && cp <= 0x085B) return NSM;

        // Brahmic family combining marks
        if (cp >= 0x08E3 && cp <= 0x0903) return NSM;
        if (cp >= 0x093A && cp <= 0x093C) return NSM;
        if (cp >= 0x0941 && cp <= 0x0948) return NSM;
        if (cp >= 0x094D && cp <= 0x094F) return NSM;
        if (cp >= 0x0951 && cp <= 0x0957) return NSM;
        if (cp >= 0x0962 && cp <= 0x0963) return NSM;
        if (cp >= 0x0981 && cp <= 0x0983) return NSM;
        if (cp == 0x09BC) return NSM;
        if (cp >= 0x09C1 && cp <= 0x09C4) return NSM;
        if (cp == 0x09CD) return NSM;
        if (cp >= 0x09E2 && cp <= 0x09E3) return NSM;
        if (cp >= 0x0A01 && cp <= 0x0A03) return NSM;
        if (cp == 0x0A3C) return NSM;
        if (cp >= 0x0A41 && cp <= 0x0A42) return NSM;
        if (cp >= 0x0A47 && cp <= 0x0A48) return NSM;
        if (cp >= 0x0A4B && cp <= 0x0A4D) return NSM;
        if (cp == 0x0A51) return NSM;
        if (cp >= 0x0A70 && cp <= 0x0A71) return NSM;
        if (cp == 0x0A75) return NSM;
        if (cp >= 0x0A81 && cp <= 0x0A83) return NSM;
        if (cp == 0x0ABC) return NSM;
        if (cp >= 0x0AC1 && cp <= 0x0AC5) return NSM;
        if (cp >= 0x0AC7 && cp <= 0x0AC8) return NSM;
        if (cp == 0x0ACD) return NSM;
        if (cp >= 0x0AE2 && cp <= 0x0AE3) return NSM;
        if (cp >= 0x0B01 && cp <= 0x0B03) return NSM;
        if (cp == 0x0B3C) return NSM;
        if (cp == 0x0B3F) return NSM;
        if (cp >= 0x0B41 && cp <= 0x0B44) return NSM;
        if (cp == 0x0B4D) return NSM;
        if (cp == 0x0B56 || cp == 0x0B57) return NSM;
        if (cp >= 0x0B62 && cp <= 0x0B63) return NSM;
        if (cp == 0x0B82) return NSM;
        if (cp == 0x0BC0) return NSM;
        if (cp == 0x0BCD) return NSM;
        if (cp >= 0x0C3E && cp <= 0x0C40) return NSM;
        if (cp >= 0x0C46 && cp <= 0x0C48) return NSM;
        if (cp >= 0x0C4A && cp <= 0x0C4D) return NSM;
        if (cp == 0x0C55 || cp == 0x0C56) return NSM;
        if (cp >= 0x0C62 && cp <= 0x0C63) return NSM;
        if (cp == 0x0C81) return NSM;
        if (cp == 0x0CBC) return NSM;
        if (cp == 0x0CBF) return NSM;
        if (cp == 0x0CC6) return NSM;
        if (cp == 0x0CCC || cp == 0x0CCD) return NSM;
        if (cp >= 0x0CE2 && cp <= 0x0CE3) return NSM;
        if (cp >= 0x0D01 && cp <= 0x0D03) return NSM;
        if (cp == 0x0D3B || cp == 0x0D3C) return NSM;
        if (cp >= 0x0D41 && cp <= 0x0D44) return NSM;
        if (cp == 0x0D4D) return NSM;
        if (cp >= 0x0D62 && cp <= 0x0D63) return NSM;
        if (cp == 0x0DCA) return NSM;
        if (cp >= 0x0DD2 && cp <= 0x0DD4) return NSM;
        if (cp == 0x0DD6) return NSM;
        if (cp == 0x0E31) return NSM;
        if (cp >= 0x0E34 && cp <= 0x0E3A) return NSM;
        if (cp >= 0x0E47 && cp <= 0x0E4E) return NSM;
        if (cp == 0x0EB1) return NSM;
        if (cp >= 0x0EB4 && cp <= 0x0EBC) return NSM;
        if (cp >= 0x0EC8 && cp <= 0x0ECD) return NSM;
        if (cp == 0x0F18 || cp == 0x0F19) return NSM;
        if (cp == 0x0F35 || cp == 0x0F37 || cp == 0x0F39) return NSM;
        if (cp >= 0x0F71 && cp <= 0x0F7E) return NSM;
        if (cp >= 0x0F80 && cp <= 0x0F84) return NSM;
        if (cp == 0x0F86 || cp == 0x0F87) return NSM;
        if (cp >= 0x0F8D && cp <= 0x0FBC) return NSM;
        if (cp == 0x0FC6) return NSM;
        if (cp >= 0x102D && cp <= 0x1030) return NSM;
        if (cp >= 0x1032 && cp <= 0x1037) return NSM;
        if (cp == 0x1039 || cp == 0x103A) return NSM;
        if (cp == 0x103D || cp == 0x103E) return NSM;
        if (cp == 0x1058 || cp == 0x1059) return NSM;
        if (cp >= 0x105E && cp <= 0x1060) return NSM;
        if (cp >= 0x1071 && cp <= 0x1074) return NSM;
        if (cp == 0x1082) return NSM;
        if (cp == 0x1085 || cp == 0x1086) return NSM;
        if (cp == 0x108D) return NSM;
        if (cp == 0x109D) return NSM;
        if (cp >= 0x1100 && cp <= 0x115F) return L;
        if (cp >= 0x1160 && cp <= 0x11FF) return NSM;
        if (cp >= 0x135D && cp <= 0x135F) return NSM;
        if (cp >= 0x1712 && cp <= 0x1714) return NSM;
        if (cp >= 0x1732 && cp <= 0x1733) return NSM;
        if (cp >= 0x1752 && cp <= 0x1753) return NSM;
        if (cp >= 0x1772 && cp <= 0x1773) return NSM;
        if (cp >= 0x180B && cp <= 0x180D) return NSM;
        if (cp == 0x1885 || cp == 0x1886) return NSM;
        if (cp == 0x18A9) return NSM;
        if (cp >= 0x1920 && cp <= 0x1922) return NSM;
        if (cp == 0x1927 || cp == 0x1928) return NSM;
        if (cp == 0x1932) return NSM;
        if (cp >= 0x1939 && cp <= 0x193B) return NSM;
        if (cp == 0x1A17 || cp == 0x1A18) return NSM;
        if (cp == 0x1A1B) return NSM;
        if (cp == 0x1A56) return NSM;
        if (cp >= 0x1A58 && cp <= 0x1A5E) return NSM;
        if (cp == 0x1A60) return NSM;
        if (cp == 0x1A62) return NSM;
        if (cp >= 0x1A65 && cp <= 0x1A6C) return NSM;
        if (cp >= 0x1A73 && cp <= 0x1A7C) return NSM;
        if (cp == 0x1A7F) return NSM;
        if (cp >= 0x1AB0 && cp <= 0x1ABE) return NSM;
        if (cp >= 0x1B00 && cp <= 0x1B03) return NSM;
        if (cp == 0x1B34) return NSM;
        if (cp >= 0x1B36 && cp <= 0x1B3A) return NSM;
        if (cp == 0x1B3C) return NSM;
        if (cp == 0x1B42) return NSM;
        if (cp >= 0x1B6B && cp <= 0x1B73) return NSM;
        if (cp >= 0x1B80 && cp <= 0x1B82) return NSM;
        if (cp == 0x1BA1) return NSM;
        if (cp == 0x1BA6 || cp == 0x1BA7) return NSM;
        if (cp == 0x1BAA) return NSM;
        if (cp >= 0x1BAC && cp <= 0x1BE5) return NSM;
        if (cp == 0x1BE8 || cp == 0x1BE9) return NSM;
        if (cp == 0x1BED) return NSM;
        if (cp >= 0x1BEF && cp <= 0x1BF1) return NSM;
        if (cp >= 0x1C2C && cp <= 0x1C33) return NSM;
        if (cp == 0x1C36 || cp == 0x1C37) return NSM;
        if (cp >= 0x1C78 && cp <= 0x1C7D) return NSM;
        if (cp >= 0x1CD0 && cp <= 0x1CD2) return NSM;
        if (cp >= 0x1CD4 && cp <= 0x1CE0) return NSM;
        if (cp >= 0x1CE2 && cp <= 0x1CE8) return NSM;
        if (cp == 0x1CED) return NSM;
        if (cp == 0x1CF4) return NSM;
        if (cp == 0x1CF8 || cp == 0x1CF9) return NSM;
        if (cp >= 0x1DC0 && cp <= 0x1DF5) return NSM;
        if (cp >= 0x1DFC && cp <= 0x1DFF) return NSM;
        if (cp >= 0x200B && cp <= 0x200D) return BN;
        if (cp == 0x200E) return L;
        if (cp == 0x200F) return R;
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
        if (cp == 0x00A0) return CS;
        if (cp == 0x2028) return L;
        if (cp == 0x2029) return B;
        if (cp == 0x202F) return CS;
        if (cp >= 0x20A0 && cp <= 0x20CF) return ET;
        if (cp == 0x00A4) return ET;
        if (cp == 0x00A2 || cp == 0x00A3 || cp == 0x00A5) return ET;
        if (cp == 0x2116) return ET;
        if (cp == 0xFE0F) return BN;
        if (cp >= 0xFE00 && cp <= 0xFE0F) return BN;

        // CJK (all LTR)
        if (cp >= 0x2E80 && cp <= 0x9FFF) return L;
        if (cp >= 0xA000 && cp <= 0xA4CF) return L;
        if (cp >= 0xAC00 && cp <= 0xD7AF) return L;
        if (cp >= 0xF900 && cp <= 0xFAFF) return L;
        if (cp >= 0xFE30 && cp <= 0xFE6F) return L;
        if (cp >= 0xFF00 && cp <= 0xFFEF) return L;
        if (cp >= 0x1F000 && cp <= 0x1FFFF) return L;

        // Unlisted Arabic-script ranges
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

        return L;
    }

    private static byte getSupplementaryBiDiType(int cp) {
        if (cp >= 0x1F600 && cp <= 0x1F64F) return L;
        if (cp >= 0x1F680 && cp <= 0x1F6FF) return L;
        return L;
    }

    // --- X1-X8: Resolve explicit embedding levels ---

    private static void resolveExplicit(byte[] types, byte[] levels, int length, byte paragraphLevel) {
        int[] levelStack = new int[MAX_STACK_SIZE + 2];
        byte[] overrideStack = new byte[MAX_STACK_SIZE + 2];
        int stackTop = 0;
        levelStack[0] = paragraphLevel;
        overrideStack[0] = L;

        for (int i = 0; i < length; i++) {
            byte type = types[i];

            switch (type) {
                case LRE:
                case LRO:
                case RLE:
                case RLO:
                case LRI:
                case RLI:
                case FSI:
                    if (stackTop < MAX_STACK_SIZE) {
                        int currentLevel = levelStack[stackTop];
                        int newLevel;
                        if (type == LRE || type == LRO) {
                            newLevel = (currentLevel + 1) | 1;
                        } else {
                            newLevel = (currentLevel + 2) & ~1;
                        }
                        if (newLevel <= MAX_STACK_SIZE) {
                            stackTop++;
                            levelStack[stackTop] = newLevel;
                            if (type == RLO) {
                                overrideStack[stackTop] = R;
                            } else if (type == LRO) {
                                overrideStack[stackTop] = L;
                            } else {
                                overrideStack[stackTop] = L; // embedding/isolate: no override
                            }
                        }
                    }
                    levels[i] = (byte) levelStack[stackTop];
                    types[i] = BN;
                    break;

                case PDF:
                    if (stackTop > 0) stackTop--;
                    levels[i] = (byte) levelStack[stackTop];
                    types[i] = BN;
                    break;

                case PDI:
                    if (stackTop > 0) stackTop--;
                    levels[i] = (byte) levelStack[stackTop];
                    types[i] = BN;
                    break;

                default:
                    levels[i] = (byte) levelStack[stackTop];
                    byte override = overrideStack[stackTop];
                    if (override == R) {
                        types[i] = R;
                    } else if (override == L && type != L && type != R && type != AL && type != EN && type != AN) {
                        types[i] = L;
                    }
                    break;
            }
        }
    }

    // --- W1-W7: Resolve weak types ---

    private static void resolveWeakTypes(byte[] types, byte[] levels, int length) {
        byte prevType = (levels[0] & 1) == 0 ? L : R;
        for (int i = 0; i < length; i++) {
            if (types[i] == NSM) types[i] = prevType;
            if (types[i] != BN) prevType = types[i];
        }

        prevType = (levels[0] & 1) == 0 ? L : R;
        for (int i = 0; i < length; i++) {
            if (types[i] == EN && prevType == AL) types[i] = AN;
            if (types[i] != BN) prevType = types[i];
        }

        for (int i = 0; i < length; i++) {
            if (types[i] == AL) types[i] = R;
        }

        for (int i = 1; i < length - 1; i++) {
            if (types[i] == ES && types[i - 1] == EN && types[i + 1] == EN) {
                types[i] = EN;
            } else if (types[i] == CS) {
                if (types[i - 1] == AN && types[i + 1] == AN) types[i] = AN;
                else if (types[i - 1] == EN && types[i + 1] == EN) types[i] = EN;
            }
        }

        for (int i = 0; i < length; i++) {
            if (types[i] == ET) {
                boolean foundEn = false;
                int j = i;
                while (j < length && types[j] == ET) j++;
                if (j < length && types[j] == EN) foundEn = true;
                if (!foundEn) {
                    j = i - 1;
                    while (j >= 0 && types[j] == ET) j--;
                    if (j >= 0 && types[j] == EN) foundEn = true;
                }
                if (foundEn) {
                    int k = i;
                    while (k < length && types[k] == ET) { types[k] = EN; k++; }
                }
            }
        }

        for (int i = 0; i < length; i++) {
            if (types[i] == ES || types[i] == ET || types[i] == CS) types[i] = ON;
        }

        prevType = (levels[0] & 1) == 0 ? L : R;
        for (int i = 0; i < length; i++) {
            if (types[i] == EN && prevType == L) types[i] = L;
            if (types[i] != BN) prevType = types[i];
        }
    }

    // --- N0-N2: Resolve neutral types ---

    private static void resolveNeutrals(byte[] types, byte[] levels, int length) {
        for (int i = 0; i < length; i++) {
            if (isNeutral(types[i])) {
                int runStart = i;
                while (i < length && isNeutral(types[i])) i++;
                int runEnd = i;
                i--;

                byte prevStrong = L;
                for (int j = runStart - 1; j >= 0; j--) {
                    if (types[j] == L) { prevStrong = L; break; }
                    if (types[j] == R || types[j] == EN || types[j] == AN) { prevStrong = R; break; }
                }

                byte nextStrong = L;
                for (int j = runEnd; j < length; j++) {
                    if (types[j] == L) { nextStrong = L; break; }
                    if (types[j] == R || types[j] == EN || types[j] == AN) { nextStrong = R; break; }
                }

                byte resolvedType;
                if (prevStrong == nextStrong) {
                    resolvedType = prevStrong;
                } else {
                    resolvedType = (levels[runStart] & 1) == 0 ? L : R;
                }
                for (int j = runStart; j < runEnd; j++) {
                    types[j] = resolvedType;
                }
            }
        }
    }

    private static boolean isNeutral(byte type) {
        return type == ON || type == WS || type == S || type == B;
    }

    // --- I1-I2: Resolve implicit levels ---

    private static void resolveImplicit(byte[] types, byte[] levels, int length) {
        for (int i = 0; i < length; i++) {
            if (levels[i] >= MAX_STACK_SIZE) continue;
            byte type = types[i];
            if ((levels[i] & 1) == 0) {
                if (type == R || type == AN || type == EN) levels[i]++;
            } else {
                if (type == L || type == EN) {
                    levels[i]++;
                } else if (type == AN) {
                    levels[i] += 2;
                }
            }
        }
    }

    // --- L1-L4: Reorder ---

    private static int[] reorder(byte[] levels, byte[] origTypes, int length, int paragraphLevel) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) result[i] = i;

        int highestLevel = paragraphLevel;
        int lowestOddLevel = MAX_STACK_SIZE + 1;
        for (int i = 0; i < length; i++) {
            if (levels[i] > highestLevel) highestLevel = levels[i];
            if ((levels[i] & 1) != 0 && levels[i] < lowestOddLevel) lowestOddLevel = levels[i];
        }

        // L1: Reset trailing WS/S/B to paragraph level (using original types, not levels)
        for (int i = length - 1; i >= 0; i--) {
            byte t = origTypes[i];
            if (t == BN || t == S || t == WS || t == B) {
                levels[i] = (byte) paragraphLevel;
            } else {
                break;
            }
        }

        // L2: Reverse contiguous runs at each level
        for (int level = highestLevel; level > lowestOddLevel; level--) {
            int i = 0;
            while (i < length) {
                if (levels[i] >= level) {
                    int runStart = i;
                    while (i < length && levels[i] >= level) i++;
                    int left = runStart, right = i - 1;
                    while (left < right) {
                        int tmp = result[left];
                        result[left] = result[right];
                        result[right] = tmp;
                        left++;
                        right--;
                    }
                } else {
                    i++;
                }
            }
        }

        return result;
    }
}
