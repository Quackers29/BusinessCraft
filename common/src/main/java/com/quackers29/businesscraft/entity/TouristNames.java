package com.quackers29.businesscraft.entity;

import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Personal names given to tourists when they spawn. The pool is mixed on purpose
 * (traditionally masculine, feminine, and neutral English names). About one name
 * in four also takes a surname. The choice is stored on the tourist; this class
 * only picks.
 */
public final class TouristNames {
    /** Nearby tourists inside this radius avoid sharing a name. */
    public static final double NEARBY_BLOCKS = 32.0;

    private static final List<String> FIRST = List.of(
            "Ada", "Agnes", "Alice", "Amelia", "Anna", "Audrey", "Beatrice", "Bess", "Bridget", "Cora",
            "Daisy", "Dorothy", "Edith", "Eleanor", "Eliza", "Ella", "Emma", "Esther", "Ethel", "Eva",
            "Flora", "Florence", "Frances", "Georgia", "Grace", "Harriet", "Hazel", "Helen", "Ida", "Irene",
            "Ivy", "Jane", "Joan", "Josephine", "Judith", "Julia", "June", "Katherine", "Laura", "Lillian",
            "Lucy", "Lydia", "Mabel", "Margaret", "Martha", "Mary", "Matilda", "Mildred", "Nancy", "Nell",
            "Nora", "Olive", "Pearl", "Rose", "Ruth", "Sylvia", "Vera", "Violet", "Willa", "Winifred",
            "Albert", "Alfred", "Arthur", "Bernard", "Calvin", "Carl", "Charles", "Clarence", "Clifford", "Clyde",
            "Donald", "Edgar", "Edmund", "Edward", "Edwin", "Elmer", "Ernest", "Eugene", "Felix", "Frank",
            "Frederick", "George", "Gerald", "Harold", "Harvey", "Henry", "Herbert", "Howard", "Hugh", "Irving",
            "Jack", "James", "John", "Joseph", "Lawrence", "Leo", "Leonard", "Lewis", "Louis", "Martin",
            "Norman", "Oscar", "Otto", "Patrick", "Paul", "Peter", "Ralph", "Raymond", "Richard", "Robert",
            "Roger", "Russell", "Samuel", "Stanley", "Theodore", "Thomas", "Victor", "Walter", "Warren", "William",
            "Alex", "Avery", "Blake", "Cameron", "Casey", "Charlie", "Drew", "Ellis", "Emery", "Finley",
            "Frankie", "Greer", "Harper", "Hayden", "Hollis", "Indigo", "Jamie", "Jordan", "Jules", "Kendall",
            "Lane", "Logan", "Marlow", "Morgan", "Noel", "Parker", "Quinn", "Reese", "Riley", "Rowan",
            "Sage", "Sam", "Shannon", "Shawn", "Skyler", "Taylor", "Terry", "Winter", "Ash", "River");

    private static final List<String> LAST = List.of(
            "Adler", "Baker", "Bell", "Bennett", "Brooks", "Carter", "Clark", "Cole", "Cooper", "Cruz",
            "Dawson", "Evans", "Fisher", "Foster", "Graham", "Gray", "Green", "Hall", "Harris", "Hayes",
            "Hill", "Hughes", "Hunt", "Jenkins", "Keller", "Knight", "Marsh", "Miller", "Moore", "Nash",
            "Owens", "Perry", "Powell", "Price", "Reed", "Ross", "Shaw", "Stone", "Turner", "Walker",
            "Ward", "Webb", "Wells", "West", "Young", "Brennan", "Dalton", "Ellison");

    private TouristNames() {
    }

    public static List<String> firstNames() {
        return FIRST;
    }

    public static List<String> surnames() {
        return LAST;
    }

    public static boolean isPlaceholder(String name) {
        if (name == null) {
            return true;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() || trimmed.equalsIgnoreCase("Tourist");
    }

    /**
     * @param nextInt {@code nextInt(bound)} from the tourist's random
     * @param avoidFull full names already used nearby
     * @param avoidFirst first names already used nearby; skipped for the first attempts only
     */
    public static String pick(IntUnaryOperator nextInt, Set<String> avoidFull, Set<String> avoidFirst) {
        Set<String> full = avoidFull == null ? Set.of() : avoidFull;
        Set<String> firsts = avoidFirst == null ? Set.of() : avoidFirst;
        for (int attempt = 0; attempt < 32; attempt++) {
            String name = roll(nextInt);
            if (full.contains(name)) {
                continue;
            }
            if (attempt < 24 && firsts.contains(firstOf(name))) {
                continue;
            }
            return name;
        }
        return roll(nextInt);
    }

    public static String firstOf(String name) {
        if (name == null) {
            return "";
        }
        int space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }

    private static String roll(IntUnaryOperator nextInt) {
        String first = FIRST.get(nextInt.applyAsInt(FIRST.size()));
        if (nextInt.applyAsInt(4) == 0) {
            return first + " " + LAST.get(nextInt.applyAsInt(LAST.size()));
        }
        return first;
    }
}
