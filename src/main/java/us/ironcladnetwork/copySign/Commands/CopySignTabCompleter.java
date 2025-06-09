package us.ironcladnetwork.copySign.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import us.ironcladnetwork.copySign.Util.SignLibraryManager;
import us.ironcladnetwork.copySign.Util.SavedSignData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tab completer for the /copysign command.
 * Now supports subcommands: "on", "off", "clear", "save", "library", "delete", "load".
 * For subcommands "delete" and "load", the second argument auto-completes saved sign names.
 */
public class CopySignTabCompleter implements TabCompleter {
    private static final List<String> OPTIONS = Arrays.asList("on", "off", "clear", "save", "library", "delete", "load");
    private final SignLibraryManager signLibraryManager;

    /**
     * Constructor to inject the SignLibraryManager.
     *
     * @param signLibraryManager the SignLibraryManager instance.
     */
    public CopySignTabCompleter(SignLibraryManager signLibraryManager) {
        this.signLibraryManager = signLibraryManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // First argument auto-completion.
        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ENGLISH);
            List<String> completions = new ArrayList<>();
            for (String option : OPTIONS) {
                if (option.startsWith(current))
                    completions.add(option);
            }
            return completions;
        }
        // Auto-complete sign names for "delete" and "load" only.
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ENGLISH);
            if (subCommand.equals("delete") || subCommand.equals("load")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Map<String, SavedSignData> savedSigns = signLibraryManager.getAllSigns(player);
                    List<String> names = new ArrayList<>(savedSigns.keySet());
                    String current = args[1].toLowerCase(Locale.ENGLISH);
                    List<String> completions = new ArrayList<>();
                    for (String name : names) {
                        if (name.toLowerCase(Locale.ENGLISH).startsWith(current))
                            completions.add(name);
                    }
                    return completions;
                }
            }
        }
        return Collections.emptyList();
    }
}
