import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

String GOLD    = "\u001B[1;93m";
String GREEN   = "\u001B[92m";
String CYAN    = "\u001B[96m";
String MAGENTA = "\u001B[95m";
String BLUE    = "\u001B[94m";
String RED     = "\u001B[91m";
String YELLOW  = "\u001B[93m";
String DIM     = "\u001B[2m";
String BOLD    = "\u001B[1m";
String RESET   = "\u001B[0m";

void main() throws Exception {
    printBanner();

    var scanner = new Scanner(System.in);
    var names = readNames(scanner);

    if (names.isEmpty()) {
        IO.println(RED + "  Nobody to pick from 😢" + RESET);
        return;
    }

    IO.println();
    IO.println(GREEN + "🎟️   " + names.size() + " brave souls enter the draw..." + RESET);
    Thread.sleep(900);

    var pool = new ArrayList<>(names);
    var winners = new ArrayList<String>();
    int round = 0;

    while (!pool.isEmpty()) {
        round++;
        liftTerminal();
        if (round > 1) {
            IO.println();
            IO.println(MAGENTA + "🔄  Spinning again with " + pool.size() + " remaining..." + RESET);
            Thread.sleep(700);
        }

        consultTheCosmos();
        drumroll();
        var winner = slotMachine(pool);
        IO.println();
        Thread.sleep(350);

        IO.println();
        var answer = ask(scanner, CYAN + "  👋  Is " + BOLD + winner + RESET + CYAN + " in the room? (y/N, Enter) " + RESET);
        if (answer == null) break;

        if (answer.startsWith("y")) {
            winners.add(winner);
            pool.remove(winner);
            IO.println();
            revealWinner(winner);
            celebrate(winner);
            break;
        } else {
            IO.println();
            IO.println(YELLOW + "  " + randomExcuse(winner) + RESET);
            pool.remove(winner);
            Thread.sleep(1400);
        }
    }

    if (pool.isEmpty() && winners.isEmpty()) {
        IO.println();
        IO.println(RED + BOLD + "  💀  We've run out of candidates! Everyone's at the coffee bar ☕" + RESET);
    }

    farewell(winners);
}

void printBanner() {
    IO.println(GOLD + """

            ╔════════════════════════════════════════════╗
            ║      🎰  JavaZone 2026 Lucky Draw  🎰       ║
            ╚════════════════════════════════════════════╝
            """ + RESET);
}

ArrayList<String> readNames(Scanner scanner) {
    IO.println(CYAN + "📋  Paste names below (one per line), end with a blank line:" + RESET);
    IO.println(DIM  + "   (tabs/commas treated as columns — first column wins)" + RESET);
    IO.println();

    var names = new ArrayList<String>();
    while (scanner.hasNextLine()) {
        var row = scanner.nextLine();
        if (row.isBlank()) break;
        var name = row.split("[\t,]")[0].trim();
        if (!name.isEmpty()) names.add(name);
    }
    return names;
}

String ask(Scanner scanner, String prompt) {
    var console = System.console();
    if (console != null) {
        var line = console.readLine("%s", prompt);
        return line == null ? null : line.trim().toLowerCase(Locale.ROOT);
    }

    System.out.print(prompt);
    System.out.flush();
    if (scanner.hasNextLine()) return scanner.nextLine().trim().toLowerCase(Locale.ROOT);

    IO.println();
    IO.println(RED + "  No input available for confirmation prompt. Run from a terminal, or include answers after the blank line." + RESET);
    return null;
}

void liftTerminal() {
    System.out.print("\n".repeat(5));
    System.out.flush();
}

void consultTheCosmos() throws InterruptedException {
    String[] sayings = {
        "🔮  Consulting the cosmic Math.random()...",
        "🧙  Casting Random.nextInt() at the audience...",
        "🌌  Asking the JVM for guidance...",
        "🎲  Rolling a 20-sided die in the heap..."
    };
    var saying = sayings[ThreadLocalRandom.current().nextInt(sayings.length)];
    IO.println(BLUE + saying + RESET);

    String[] spinner = {"⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"};
    for (int i = 0; i < 22; i++) {
        System.out.print("\r   " + MAGENTA + BOLD + spinner[i % spinner.length] + RESET + "   ");
        System.out.flush();
        Thread.sleep(70);
    }
    System.out.print("\r          \r");
}

void drumroll() throws InterruptedException {
    IO.println(MAGENTA + "🥁  Drumroll please..." + RESET);
    for (int i = 3; i >= 1; i--) {
        System.out.print("\r   " + BOLD + GOLD + i + "..." + RESET + "   ");
        System.out.flush();
        Thread.sleep(150);
    }
    System.out.print("\r              \r");
}

String slotMachine(List<String> pool) throws InterruptedException {
    var rng = ThreadLocalRandom.current();
    var winner = pool.get(rng.nextInt(pool.size()));
    int cycles = 28;
    double delay = 35;
    String[] icons = {"🎰","🎲","🎯","✨","🎁","🎪"};
    String[] colors = {CYAN, MAGENTA, BLUE, GREEN, YELLOW};
    for (int i = 0; i < cycles; i++) {
        var pick = (i == cycles - 1) ? winner : pool.get(rng.nextInt(pool.size()));
        var icon = icons[i % icons.length];
        var color = colors[i % colors.length];
        System.out.print("\r" + color + icon + "  " + BOLD + pad(pick, 32) + RESET);
        System.out.flush();
        Thread.sleep((long) delay);
        delay *= 1.13;
    }
    return winner;
}

void revealWinner(String winner) throws InterruptedException {
    IO.println(YELLOW  + "             *  ✨  *" + RESET);          Thread.sleep(120);
    IO.println(MAGENTA + "          ✨  💫  ✨  💫  ✨" + RESET);   Thread.sleep(120);
    IO.println(GOLD    + "       ⭐  🎆   🏆   🎆  ⭐" + RESET);    Thread.sleep(120);
    IO.println(MAGENTA + "          ✨  💫  ✨  💫  ✨" + RESET);   Thread.sleep(120);
    IO.println(YELLOW  + "             *  ✨  *" + RESET);          Thread.sleep(300);

    var border    = "═".repeat(Math.max(winner.length() + 6, 24));
    int totalPad  = border.length() - winner.length();
    int leftPad   = totalPad / 2;
    int rightPad  = totalPad - leftPad;
    var nameLine  = " ".repeat(leftPad) + winner + " ".repeat(rightPad);

    IO.println(GOLD + BOLD + """
                ╔%s╗
                ║%s║
                ╚%s╝""".formatted(border, nameLine, border) + RESET);
    Thread.sleep(350);
    IO.println(MAGENTA + "    🎊  🥳  🎊  🥳  🎊  🥳  🎊  🥳  🎊" + RESET);
}

void celebrate(String winner) {
    IO.println();
    IO.println(GREEN + BOLD + "  🏆  Congratulations, " + winner + "! 🏆" + RESET);
}

void farewell(List<String> winners) {
    IO.println();
    if (winners.isEmpty()) {
        IO.println(DIM + "  No winners drawn this time. Thanks for playing! 🐕" + RESET);
        return;
    }
    IO.println(GOLD + BOLD + "  ╭───────────  Today's Winners  ───────────╮" + RESET);
    for (int i = 0; i < winners.size(); i++) {
        var medal = switch (i) {
            case 0 -> "🥇";
            case 1 -> "🥈";
            case 2 -> "🥉";
            default -> "🏅";
        };
        IO.println(GREEN + "     " + medal + "  " + BOLD + winners.get(i) + RESET);
    }
    IO.println(GOLD + BOLD + "  ╰──────────────────────────────────────────╯" + RESET);
    IO.println();
    IO.println(DIM + "  May the new tricks be ever in your favor — @bazlur_rahman 🐕" + RESET);
}

String randomExcuse(String name) {
    String[] excuses = {
        "  ☕  " + name + " is probably at the coffee bar...",
        "  💥  " + name + " threw NoSuchPersonException!",
        "  🔒  " + name + " is stuck in a deadlock.",
        "  ♻️   " + name + " is being garbage collected.",
        "  🧵  " + name + " is on a virtual thread to nowhere.",
        "  🕳️   " + name + " is currently null... and not present.",
        "  🐛  " + name + " is debugging in production.",
        "  🐌  " + name + " is waiting for Maven to download the internet.",
        "  💾  " + name + " was last seen in the JVM heap.",
        "  🌀  Has " + name + " been pattern-matched into another dimension?",
        "  🥧  " + name + " is enjoying pastéis de nata.",
        "  ⏳  " + name + " is awaiting a CompletableFuture that never completes."
    };
    return excuses[ThreadLocalRandom.current().nextInt(excuses.length)];
}

String pad(String s, int width) {
    if (s.length() >= width) return s.substring(0, width);
    return s + " ".repeat(width - s.length());
}
