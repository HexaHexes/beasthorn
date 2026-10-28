package org.bitwisemadness.pseudocore.util;

/**
 * The word matching behind the Race line: whether a variant's own text already
 * names the species it belongs to (#69).
 *
 * <p>Deliberately imports no foreign type at all, mirroring
 * {@link DragonTints} and {@link DragonStages}: the whole decision is string
 * work, so it lives here as pure Java and the junit suite can pin it. The
 * caller is {@link VariantHelper#alreadyNamesMob}, which keeps its signature and
 * delegates.</p>
 *
 * <p>Before this class the check used a bare {@code String.contains} on the
 * whole text, so a word could match anywhere inside another word rather than
 * only at an end. "lime" matched at index 1 inside "slime", and a Lime Slime's
 * name line read "Lime" instead of "Lime Slime" (#69). Splitting each side
 * into words and matching them whole is the entire fix.</p>
 *
 * <p>The two directions are deliberately not the same test, because the two
 * questions are not the same. A variant word may relate to a species word at
 * either end - "dire" begins "direwolf", and "wolf" ends it - so that direction
 * is symmetric. A species word may retire a variant only by appearing whole in
 * it, which is what stops "slime" from retiring "slime king": the species
 * begins that variant but does not name it.</p>
 */
public final class SpeciesWords {

    /**
     * How many letters each half of a compound word must have before a suffix
     * match counts.
     *
     * <p>Four. This guards the remainder as well as the matched part, which a
     * length floor on the matched part alone could not do: "lime" is four
     * letters and ends "slime", so any guard that only measured "lime" would
     * let it through and a Lime Slime would still lose its species (#69).
     * "direwolf" splits dire + wolf, both four or more, so it still retires.</p>
     */
    private static final int MIN_COMPOUND_HALF = 4;

    private SpeciesWords() {
    }

    /**
     * Whether the variant's own words already name the species it belongs to.
     *
     * <p>Whichever side carries the shared word does the retiring, each under
     * its own rule; see the class javadoc for why they differ.</p>
     *
     * @param variantText the variant's display text
     * @param raceText    the species text
     */
    public static boolean alreadyNamesMob(String variantText, String raceText) {
        if (variantText == null || raceText == null) return false;
        // The variant side is symmetric; the species side is whole-word only.
        return anyWordRelates(variantText, raceText)
                || anyWholeWordIn(raceText, variantText);
    }

    /**
     * Whether any word of {@code words} relates to any word of {@code text} at
     * a <i>meaningful</i> end, as "dire" and "direwolf" or "wolf" and
     * "direwolf" do.
     *
     * <p>The prefix half is unguarded: a shortened species name begins the
     * species word, and nothing else can be confused with it. The suffix half
     * is guarded by {@link #isCompound}, because a suffix may be a
     * coincidental ending rather than a buried species - "lime" is a suffix of
     * "slime" - and only the compound check keeps that from retiring the race
     * (#69). "wolf" halves "direwolf", so it still retires.</p>
     *
     * <p>The check runs on whitespace-separated words rather than on the raw
     * text, which is what lets "lime" compare against "slime" as a whole word
     * and fail instead of matching at index 1 as a substring.</p>
     */
    private static boolean anyWordRelates(String words, String text) {
        for (String word : splitWords(words)) {
            for (String otherWord : splitWords(text)) {
                // A prefix is unambiguous: a shortened species name begins the
                // species word and nothing else.
                if (otherWord.startsWith(word) && !otherWord.equals(word)) {
                    return true;
                }
                // A suffix is only safe inside a compound, where both halves
                // are words in their own right. A bare length floor does not
                // work here - "lime" is four letters and ends "slime" (#69) -
                // so the guard is MIN_COMPOUND_HALF on BOTH sides instead.
                if (otherWord.endsWith(word) && isCompound(otherWord, word)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether {@code whole} is a compound word that {@code part} forms one half
     * of - "direwolf" as dire + wolf - rather than a word that merely happens
     * to end in {@code part}, as "slime" ends in "lime".
     *
     * <p>Both sides must be at least {@link #MIN_COMPOUND_HALF} letters, which
     * is what separates the two shapes. "wolf" is four letters and so halves
     * "direwolf"; "lime" is also four, but the other half of "slime" would be
     * "s", and a one-letter stem is not a word. The guard has to sit on the
     * remainder, not on the matched part, for exactly that reason.</p>
     */
    private static boolean isCompound(String whole, String part) {
        return whole.length() - part.length() >= MIN_COMPOUND_HALF
                && part.length() >= MIN_COMPOUND_HALF;
    }

    /**
     * Whether any word of {@code words} is a whole word of {@code text}.
     *
     * <p>The stricter direction, and the asymmetry is deliberate. A species
     * word may retire a variant only by appearing whole in it, never by
     * beginning or ending one: "slime" begins "slimeking" and "king" ends it,
     * yet neither word names that variant, so left to the rule above they would
     * retire the race from a variant that never named itself. This is also why
     * "lime" no longer retires "slime" - as whole words the two simply
     * differ.</p>
     */
    private static boolean anyWholeWordIn(String words, String text) {
        for (String word : splitWords(words)) {
            for (String otherWord : splitWords(text)) {
                if (word.equals(otherWord)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Splits text into words on whitespace, lowercased for comparison.
     *
     * <p>Split on {@code \s+} rather than a single space, so a variant with
     * double spaces or a stray tab still compares as its words and not as
     * fragments with empty words between them.</p>
     */
    private static String[] splitWords(String text) {
        String normalised = text.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalised.isEmpty()) return new String[0];
        return normalised.split("\\s+");
    }
}
