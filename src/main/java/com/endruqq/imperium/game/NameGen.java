package com.endruqq.imperium.game;

import net.minecraft.util.RandomSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class NameGen {
    private static final String[] NATION_PRE = {"Wol", "Kar", "Mir", "Vel", "Ost", "Brun", "Sol", "Drak", "Lech", "Nor", "Aval", "Zar",
            "Elan", "Mer", "Tar", "Kast", "Bor", "Ruth", "Sar", "Val", "Gal", "Hel", "Iber", "Mor", "Pol", "Ser", "Tyr", "Ver"};
    private static final String[] NATION_SUF = {"andia", "onia", "avia", "aria", "gard", "mark", "ania", "heim", "ovia", "esia",
            "istan", "oria", "enia", "ycja", "ulia", "ow", "land"};
    private static final String[] NATION_FORM = {"Królestwo %s", "Księstwo %s", "Cesarstwo %s", "Republika %s", "%s", "Wolne Miasta %s",
            "Rzeczpospolita %s", "Kalifat %s", "Ordo %s", "Imperium %s"};

    private static final String[] PROV_PRE = {"Bia", "Czarn", "Wysok", "Zło", "Kam", "Lis", "Mor", "Dęb", "Sokol", "Wil", "Grun",
            "Rad", "Bel", "Kor", "Nor", "Ost", "Zar", "Brz", "Jas", "Krak", "Gniez", "Pozn", "Lub", "Sand", "Tor", "Płoc", "Wroc", "Głog",
            "Miel", "Sier", "Rudn", "Przem", "Stary", "Nowy", "Zielon", "Złot", "Srebr", "Mgl", "Lodow", "Słon", "Wiatr"};
    private static final String[] PROV_SUF = {"owo", "ice", "grad", "ów", "sk", "ań", "in", "no", "ek", "ia", "heim", "burg", "wice",
            "ny Bród", "a Góra", "e Pole", "y Las", "a Dolina", "e Jezioro", "a Przełęcz"};

    private static final String[] MALE = {"Jan", "Piotr", "Bartek", "Wojciech", "Mieszko", "Bolesław", "Grzegorz", "Stefan",
            "Kazimierz", "Zbigniew", "Marek", "Tomasz", "Jakub", "Mikołaj", "Wacław", "Zdzisław", "Leszek", "Bogdan", "Andrzej", "Jarek",
            "Henryk", "Władek", "Feliks", "Ignacy", "Janusz", "Edmund", "Czesław"};
    private static final String[] FEMALE = {"Anna", "Kasia", "Zofia", "Jadwiga", "Halina", "Maria", "Agnieszka", "Barbara",
            "Dobrawa", "Grażyna", "Jagoda", "Wanda", "Bożena", "Ewa", "Helena", "Krystyna", "Ludmiła", "Małgorzata", "Stanisława", "Teresa"};
    private static final String[] SURNAME = {"Kowalsk", "Nowak", "Wiśniewsk", "Wójcik", "Kamińsk", "Lewandowsk", "Zielińsk",
            "Szymańsk", "Woźniak", "Dąbrowsk", "Kozłowsk", "Mazur", "Krawczyk", "Piątek", "Grabowsk", "Pawlak", "Michalsk", "Król",
            "Wieczorek", "Jabłońsk", "Wróbel", "Nowick", "Majewsk", "Olszewsk", "Stępień", "Malinowsk", "Jaworsk", "Adamczyk", "Dudek",
            "Zając", "Sowa", "Kmieć", "Gołąb", "Bąk", "Kruk", "Lis", "Wilk", "Baran"};

    private NameGen() {}

    public static String nation(RandomSource r, Collection<String> taken) {
        for (int i = 0; i < 50; i++) {
            String core = NATION_PRE[r.nextInt(NATION_PRE.length)] + NATION_SUF[r.nextInt(NATION_SUF.length)];
            String n = r.nextInt(3) == 0 ? String.format(NATION_FORM[r.nextInt(NATION_FORM.length)], core) : core;
            if (!taken.contains(n) && !containsCore(taken, core)) return n;
        }
        return "Nation " + r.nextInt(1000);
    }

    private static boolean containsCore(Collection<String> taken, String core) {
        for (String t : taken) if (t.contains(core)) return true;
        return false;
    }

    public static String province(RandomSource r, Set<String> taken) {
        for (int i = 0; i < 50; i++) {
            String n = PROV_PRE[r.nextInt(PROV_PRE.length)] + PROV_SUF[r.nextInt(PROV_SUF.length)];
            if (taken.add(n)) return n;
        }
        return "Prowincja " + r.nextInt(1000);
    }

    public static Set<String> newSet() {
        return new HashSet<>();
    }

    public static String person(RandomSource r, boolean female) {
        String first = female ? FEMALE[r.nextInt(FEMALE.length)] : MALE[r.nextInt(MALE.length)];
        String s = SURNAME[r.nextInt(SURNAME.length)];
        if (s.endsWith("sk") || s.endsWith("ck")) s = s + (female ? "a" : "i");
        return first + " " + s;
    }
}
