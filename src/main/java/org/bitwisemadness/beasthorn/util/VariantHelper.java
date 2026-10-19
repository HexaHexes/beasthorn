package org.bitwisemadness.beasthorn.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Turns a stored mob's {@code Variant} tag into a name for the Race line.
 *
 * <p>Every species in scope stores its variant under the same key but not the
 * same type, and no two species agree on how that value becomes words. Ice and
 * Fire keeps dragons, sea serpents and hippogryphs as registered names that its
 * own lang file translates; it keeps trolls as a registered name it never
 * translates at all; and it keeps death worms as a bare int. Twenty more
 * species — across Tensura, Mobs of Mythology and Mysticism — store a bare int
 * ordinal resolved through an enum. This class is the one place that knows all
 * of it.</p>
 *
 * <p>Deliberately imports <b>no</b> foreign type at all. The Ice and Fire
 * branches are pure NBT-and-lang lookups, and the int-ordinal family resolves
 * through {@link VariantBridge}, which gates every call on ModList. That is
 * what lets this class load identically whatever the pack contains.</p>
 */
public final class VariantHelper {

    /** The one NBT key every species in scope uses, though the types differ. */
    private static final String VARIANT_KEY = "Variant";

    /**
     * The variant constant that names no particular variant.
     *
     * <p>Seven species use it, and on each one it means "the plain version".
     * Left alone it would reach the tooltip as {@code Default Skeleton},
     * {@code Default Cattledeer} or {@code Default Phantaspore} — text that
     * tells a reader nothing they did not already know from the race. Dropping
     * it lets those mobs read plainly as {@code Skeleton}, {@code Cattledeer}
     * and {@code Phantaspore}.</p>
     */
    private static final String DEFAULT_VARIANT = "DEFAULT";

    /**
     * Mobs of Mythology names no drake skins at all — its enum is bare
     * {@code DRAKE_1..DRAKE_7}, its textures are {@code drake_1.png} through
     * {@code drake_7.png}, and its lang file has no keys for them. Seven drakes
     * that differ only by palette therefore deserve better than numbers, so
     * these names are ours, taken from the textures themselves.
     *
     * <p>Indexed by the <b>stored id, which is zero-based</b>: {@code DRAKE_1}
     * is id 0 and {@code DRAKE_7} is id 6. Read off the enum's constructor
     * rather than assumed from the constant names, since guessing 1-based would
     * have misnamed every single drake.</p>
     */
    private static final String[] DRAKE_NAMES = {
            "Fiery",         // id 0 -> DRAKE_1
            "Plum",          // id 1 -> DRAKE_2
            "Crimson",       // id 2 -> DRAKE_3
            "Violet",        // id 3 -> DRAKE_4
            "Slate Green",   // id 4 -> DRAKE_5
            "Teal",          // id 5 -> DRAKE_6
            "Black",         // id 6 -> DRAKE_7
    };

    /**
     * How long a word has to be before it counts as naming the species.
     *
     * <p>Four. Short enough that {@code wolf} still retires the species from a
     * direwolf's variant, long enough that {@code age} does not retire it from
     * a villager by turning up inside the word.</p>
     */
    private static final int MIN_NAMING_WORD = 4;

    private VariantHelper() {
    }

    /**
     * A resolved variant, plus whether its text already names the species.
     *
     * @param text         the display name, never null
     * @param replacesRace true when the name already contains the species word,
     *                     so it stands in for the race instead of preceding it
     */
    public record Variant(String text, boolean replacesRace) {
    }

    /**
     * The variant of the stored mob, or null when it has none we can read.
     *
     * <p>Type-checked rather than blindly read: a species whose storage changes
     * shape between mod versions falls through to "no variant" instead of
     * printing whatever an int happens to look like as text.</p>
     */
    public static Variant variantOf(ResourceLocation typeId, CompoundTag data) {
        if (typeId == null || data == null || !data.contains(VARIANT_KEY)) return null;
        return switch (typeId.toString()) {
            // Dragon colours carry a lang key, and IaF's wording is not a
            // capitalisation of the stored name - "green" reads "Emerald".
            case "iceandfire:fire_dragon", "iceandfire:ice_dragon", "iceandfire:lightning_dragon" ->
                    localized(data, "dragon.");
            case "iceandfire:sea_serpent" -> localized(data, "sea_serpent.");
            // Hippogryph's key renders a whole phrase ("Black Hippogryph"), so
            // it replaces the race rather than preceding it.
            case "iceandfire:hippogryph" -> hippogryph(data);
            // Trolls have no lang key anywhere in IaF - only their armour and
            // weapon item names reveal that the three types are these words.
            case "iceandfire:troll" -> plainName(data);
            case "iceandfire:deathworm" -> deathWorm(data);

            // The int-ordinal family: twenty species across three mods that
            // all store a bare int under this one key and resolve it through
            // their own byId table. One shared shape, so one shared path.
            case "tensura:direwolf" -> intVariant(data, VariantBridge::direwolf);
            case "tensura:slime" -> intVariant(data, VariantBridge::slime);
            case "tensura:basilisk" -> intVariant(data, VariantBridge::basilisk);
            case "tensura:bone_golem" -> intVariant(data, VariantBridge::boneGolem);
            case "tensura:cattledeer" -> intVariant(data, VariantBridge::cattledeer);
            case "tensura:horned_rabbit" -> intVariant(data, VariantBridge::hornedRabbit);
            case "tensura:hound_dog" -> intVariant(data, VariantBridge::houndDog);
            case "tensura:hover_lizard" -> intVariant(data, VariantBridge::hoverLizard);
            case "tensura:landfish" -> intVariant(data, VariantBridge::landfish);
            case "tensura:leech_lizard" -> intVariant(data, VariantBridge::leechLizard);
            case "tensura:lizardman" -> intVariant(data, VariantBridge::lizardman);
            case "tensura:one_eyed_owl" -> intVariant(data, VariantBridge::oneEyedOwl);
            case "tensura:orc" -> intVariant(data, VariantBridge::orc);
            case "tensura:dragon_peacock" -> intVariant(data, VariantBridge::dragonPeacock);
            case "tensura:phantaspore" -> intVariant(data, VariantBridge::phantaspore);
            case "tensura:skeleton" -> intVariant(data, VariantBridge::skeleton);
            case "tensura:zombie" -> intVariant(data, VariantBridge::zombie);
            // The drake is the one species that needs names of our own, so it
            // owns a case rather than sharing the family path outright.
            case "mobs_of_mythology:drake" -> drake(data);
            case "mobs_of_mythology:sporeling" -> intVariant(data, VariantBridge::sporeling);
            case "mysticism:okami" -> intVariant(data, VariantBridge::okami);

            default -> plainName(data);
        };
    }

    /**
     * Whether the variant's own words already name the species it belongs to.
     *
     * <p>The redundancy check behind dropping the race. A direwolf's
     * {@code Mystic Wind Wolf} already reads as a wolf, so repeating the
     * species would give "Mystic Wind Wolf Direwolf"; its {@code Red Fang}
     * does not, so "Red Fang Direwolf" keeps both. A variant that <i>is</i>
     * the species name - {@code Direwolf} on a direwolf - retires it too.
     * </p>
     *
     * <p>Compared word by word in both directions, so whichever side carries
     * the shared word does the retiring, and only for words of
     * {@link #MIN_NAMING_WORD} letters or more. The threshold is the whole
     * reason this is safe: it is what lets {@code wolf} match inside
     * {@code direwolf} while {@code age} stays out of {@code villager}, which
     * would otherwise match by accident and strip a race from a mob that never
     * named itself.</p>
     */
    public static boolean alreadyNamesMob(String variantText, String raceText) {
        if (variantText == null || raceText == null) return false;
        return anyWordAppearsIn(variantText, raceText)
                || anyWordAppearsIn(raceText, variantText);
    }

    /** Whether any of {@code words} occurs inside {@code text}, as a fragment. */
    private static boolean anyWordAppearsIn(String words, String text) {
        String haystack = text.toLowerCase(Locale.ROOT);
        for (String word : words.split("\\s+")) {
            if (word.length() < MIN_NAMING_WORD) continue;
            if (haystack.contains(word.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /**
     * A species whose variant is a registered name with a short lang namespace.
     *
     * <p>Falls back to a title-cased name so an unlocalised variant degrades to
     * readable text rather than leaking a translation key into a tooltip.</p>
     */
    private static Variant localized(CompoundTag data, String keyPrefix) {
        String raw = stringVariant(data);
        String title = titleCase(raw);
        if (title == null) return null;
        return new Variant(Component.translatableWithFallback(keyPrefix + raw, title).getString(), false);
    }

    /**
     * Hippogryph, whose lang value is a finished name rather than a modifier.
     *
     * <p>Prefixing it with the race would read "Black Hippogryph Hippogryph",
     * so this is the one species that substitutes instead of prepends. The
     * fallback appends the species word for the same reason - without IaF's
     * translation the line still has to name the mob.</p>
     */
    private static Variant hippogryph(CompoundTag data) {
        String raw = stringVariant(data);
        String title = titleCase(raw);
        if (title == null) return null;
        String text = Component.translatableWithFallback(
                "entity.iceandfire.hippogryph." + raw, title + " Hippogryph").getString();
        return new Variant(text, true);
    }

    /** A variant stored as a name we simply read and title-case. */
    private static Variant plainName(CompoundTag data) {
        String title = titleCase(stringVariant(data));
        return title == null ? null : new Variant(title, false);
    }

    /**
     * The death worm's int variant, mapped exactly as IaF's renderer does.
     *
     * <p>The three chitin colours are tan, red and white, but the order is
     * <b>not</b> the order the renderer declares its texture constants in
     * ({@code RED, WHITE, YELLOW}). {@code DeathWormEntityRenderer#getTexture}
     * tests 2 for white, 1 for red, and falls through to yellow for anything
     * else, which makes 0 the tan variant. Taken from that branch rather than
     * from declaration order, which would have named every worm wrongly.</p>
     */
    private static Variant deathWorm(CompoundTag data) {
        if (!data.contains(VARIANT_KEY, Tag.TAG_INT)) return null;
        return new Variant(switch (data.getInt(VARIANT_KEY)) {
            case 1 -> "Red";
            case 2 -> "White";
            default -> "Tan";
        }, false);
    }

    /**
     * A variant stored as a bare int ordinal and resolved through its owning
     * mod's enum — the shape shared by twenty species across three mods.
     *
     * <p>The resolver is gated behind {@link VariantBridge}, so a mod that is
     * no longer installed yields no variant instead of a crash. That matters
     * because a horn is portable: one captured under Mobs of Mythology and
     * hovered after its removal still carries that mob's NBT.</p>
     *
     * <p>The {@link #DEFAULT_VARIANT} constant is dropped here rather than
     * per-species, which is what lets seven different mobs read plainly as
     * {@code Skeleton}, {@code Cattledeer} and {@code Phantaspore} instead of
     * {@code Default Skeleton} and friends.</p>
     */
    private static Variant intVariant(CompoundTag data, IntFunction<Optional<String>> resolver) {
        if (data == null || !data.contains(VARIANT_KEY, Tag.TAG_INT)) return null;
        String raw = resolver.apply(data.getInt(VARIANT_KEY)).orElse(null);
        if (raw == null || DEFAULT_VARIANT.equals(raw)) return null;
        String title = titleCase(raw);
        return title == null ? null : new Variant(title, false);
    }

    /**
     * The drake, whose seven skins MoM never names.
     *
     * <p>Our own table covers the ids the mod ships today; anything outside it
     * falls through to the enum's own constant, so a future build adding an
     * eighth drake degrades to {@code Drake 8} rather than showing nothing or
     * throwing.</p>
     */
    private static Variant drake(CompoundTag data) {
        if (data == null || !data.contains(VARIANT_KEY, Tag.TAG_INT)) return null;
        int id = data.getInt(VARIANT_KEY);
        if (id >= 0 && id < DRAKE_NAMES.length) return new Variant(DRAKE_NAMES[id], false);
        return intVariant(data, VariantBridge::drake);
    }

    /** The stored variant as a raw string, or null if it is not one. */
    private static String stringVariant(CompoundTag data) {
        if (!data.contains(VARIANT_KEY, Tag.TAG_STRING)) return null;
        return data.getString(VARIANT_KEY);
    }

    /**
     * Reads a stored variant as words: {@code dark_brown} becomes
     * {@code Dark Brown}.
     *
     * <p>Returns null for anything that would render as noise, which is what
     * keeps the generic fallback honest when some unrelated mod stores an
     * opaque value under the same key — a path, a bare number, or a blank all
     * decline rather than reaching the tooltip. A namespace is stripped first
     * so {@code someid:value} reads as {@code Value}.</p>
     */
    private static String titleCase(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        int colon = text.indexOf(':');
        if (colon >= 0) text = text.substring(colon + 1).trim();
        if (text.isEmpty() || text.length() > 48 || text.indexOf('/') >= 0) return null;

        boolean hasLetter = false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                hasLetter = true;
                break;
            }
        }
        if (!hasLetter) return null;

        StringBuilder out = new StringBuilder();
        for (String part : text.split("[_\\-\\s]+")) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(capitalise(part));
        }
        return out.isEmpty() ? null : out.toString();
    }

    /**
     * Upper-cases a token's first letter and leaves the rest alone unless the
     * token is shouty, so {@code red} becomes {@code Red}, {@code RED} becomes
     * {@code Red}, and a camel-cased name keeps its own casing intact.
     */
    private static String capitalise(String token) {
        String first = token.substring(0, 1).toUpperCase(Locale.ROOT);
        String rest = token.substring(1);
        if (allUpper(token)) rest = rest.toLowerCase(Locale.ROOT);
        return first + rest;
    }

    private static boolean allUpper(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isLetter(c) && !Character.isUpperCase(c)) return false;
        }
        return true;
    }
}
