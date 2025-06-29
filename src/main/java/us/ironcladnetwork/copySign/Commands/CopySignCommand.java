package us.ironcladnetwork.copySign.Commands;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.ironcladnetwork.copySign.Lang.Lang;
import us.ironcladnetwork.copySign.Util.CopySignToggleManager;
import us.ironcladnetwork.copySign.Util.SignLibraryManager;
import us.ironcladnetwork.copySign.Util.SavedSignData;
import us.ironcladnetwork.copySign.Util.SignLibraryGUI;
import us.ironcladnetwork.copySign.Util.NBTValidationUtil;
import us.ironcladnetwork.copySign.Util.Permissions;
import us.ironcladnetwork.copySign.Util.SignValidationUtil;
import us.ironcladnetwork.copySign.Util.SignLoreBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Command executor for /copysign with sub-commands "on", "off", "clear",
 * and extended subcommands "save", "library", "delete", "load".
 */
public class CopySignCommand implements CommandExecutor, TabCompleter {
    // Removed the static enabledMap and use the toggleManager instead.
    private final CopySignToggleManager toggleManager;
    // New field for managing a player's sign library.
    private final SignLibraryManager signLibraryManager;

    /**
     * Constructor to inject the toggle manager and the sign library manager.
     *
     * @param toggleManager      the CopySignToggleManager instance.
     * @param signLibraryManager the SignLibraryManager instance.
     */
    public CopySignCommand(CopySignToggleManager toggleManager, SignLibraryManager signLibraryManager) {
        this.toggleManager = toggleManager;
        this.signLibraryManager = signLibraryManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow players to execute this command.
        if (!(sender instanceof Player)) {
            sender.sendMessage(Lang.COMMAND_PLAYER_ONLY.getWithPrefix());
            return true;
        }

        Player player = (Player) sender;

        // Check basic permission for all commands
        if (!Permissions.canUse(player)) {
            player.sendMessage(Lang.NO_PERMISSION_USE.getWithPrefix());
            return true;
        }

        // No arguments: send usage
        if (args.length == 0) {
            player.sendMessage(Lang.COPYSIGN_USAGE.getWithPrefix());
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ENGLISH);
        switch(subCommand) {
            case "on":
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.on", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Set state to enabled and persist it.
                toggleManager.setCopySignEnabled(player, true);
                player.sendMessage(Lang.COPYSIGN_ENABLED.getWithPrefix());
                break;
            case "off":
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.off", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Set state to disabled and persist it.
                toggleManager.setCopySignEnabled(player, false);
                player.sendMessage(Lang.COPYSIGN_DISABLED.getWithPrefix());
                break;
            case "clear":
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.clear", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Check if the clear command feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfig().getBoolean("features.clear-command", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cClear command is currently disabled.");
                    return true;
                }
                // Check cooldown
                if (!us.ironcladnetwork.copySign.CopySign.getCooldownManager().canUseCommand(player, "clear")) {
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().sendCooldownMessage(player, "clear");
                    return true;
                }
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem == null || heldItem.getType() == Material.AIR) {
                    player.sendMessage(Lang.CLEAR_NO_ITEM.getWithPrefix());
                } else {
                    // Clear NBT data from the held item.
                    NBTItem nbtItem = new NBTItem(heldItem);
                    nbtItem.removeKey("copiedSignFront");
                    nbtItem.removeKey("copiedSignBack");
                    nbtItem.removeKey("copiedSignFrontColor");
                    nbtItem.removeKey("copiedSignBackColor");
                    nbtItem.removeKey("signGlowing");
                    nbtItem.removeKey("signType");
                    ItemStack updatedItem = nbtItem.getItem();
                    // Clear lore as well.
                    ItemMeta meta = updatedItem.getItemMeta();
                    if (meta != null) {
                        meta.setLore(null);
                        updatedItem.setItemMeta(meta);
                    }
                    // Update player's held item.
                    player.getInventory().setItemInMainHand(updatedItem);
                    player.sendMessage(Lang.CLEAR_SUCCESS.getWithPrefix());
                    // Record command usage
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().recordCommandUse(player, "clear");
                }
                break;
            case "save": {
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.save", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Check library permission for library commands
                if (!Permissions.canSaveToLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfig().getBoolean("features.sign-library", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cSign library is currently disabled.");
                    return true;
                }
                // Check cooldown
                if (!us.ironcladnetwork.copySign.CopySign.getCooldownManager().canUseCommand(player, "save")) {
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().sendCooldownMessage(player, "save");
                    return true;
                }
                // Usage: /copysign save [name]
                if (args.length < 2) {
                    player.sendMessage(Lang.COPYSIGN_USAGE.getWithPrefix());
                    return true;
                }
                String saveName = args[1];
                
                // Validate save name input
                if (!SignValidationUtil.isValidSignName(saveName)) {
                    player.sendMessage(Lang.PREFIX.get() + "§cInvalid sign name. Use only letters, numbers, hyphens, and underscores (max 32 characters).");
                    return true;
                }
                
                // Check if a sign with this name already exists for the player.
                if (signLibraryManager.getSign(player, saveName) != null) {
                    player.sendMessage(Lang.SIGN_ALREADY_EXISTS.getWithPrefix());
                    return true;
                }
                ItemStack heldItemForSave = player.getInventory().getItemInMainHand();
                if (heldItemForSave == null || heldItemForSave.getType() == Material.AIR) {
                    player.sendMessage(Lang.MUST_HOLD_SIGN.getWithPrefix());
                    return true;
                }
                // Check if the held sign type is allowed
                if (!SignValidationUtil.isSignTypeAllowed(heldItemForSave.getType().name())) {
                    player.sendMessage(Lang.SIGN_TYPE_NOT_ALLOWED_SAVE.getWithPrefix());
                    return true;
                }
                NBTItem nbtItemForSave = new NBTItem(heldItemForSave);
                if (!nbtItemForSave.hasTag("copiedSignFront") || !nbtItemForSave.hasTag("copiedSignBack")) {
                    player.sendMessage(Lang.SIGN_NO_DATA.getWithPrefix());
                    return true;
                }
                signLibraryManager.saveSign(player, saveName, heldItemForSave);
                player.sendMessage(Lang.SIGN_SAVED_SUCCESSFULLY.getWithPrefix());
                // Record command usage
                us.ironcladnetwork.copySign.CopySign.getCooldownManager().recordCommandUse(player, "save");
                break;
            }
            case "library": {
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.library", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Check library permission for library commands
                if (!Permissions.canViewLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfig().getBoolean("features.sign-library", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cSign library is currently disabled.");
                    return true;
                }
                // Check cooldown
                if (!us.ironcladnetwork.copySign.CopySign.getCooldownManager().canUseCommand(player, "library")) {
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().sendCooldownMessage(player, "library");
                    return true;
                }
                Map<String, SavedSignData> savedSigns = signLibraryManager.getAllSigns(player);
                if (savedSigns.isEmpty()) {
                    player.sendMessage(Lang.SIGN_LIBRARY_EMPTY.getWithPrefix());
                } else {
                    SignLibraryGUI.open(player, savedSigns);
                    // Record command usage
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().recordCommandUse(player, "library");
                }
                break;
            }
            case "delete": {
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.delete", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Check library permission for library commands
                if (!Permissions.canDeleteFromLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfig().getBoolean("features.sign-library", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cSign library is currently disabled.");
                    return true;
                }
                // Check cooldown
                if (!us.ironcladnetwork.copySign.CopySign.getCooldownManager().canUseCommand(player, "delete")) {
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().sendCooldownMessage(player, "delete");
                    return true;
                }
                // Usage: /copysign delete [name]
                if (args.length < 2) {
                    player.sendMessage(Lang.COPYSIGN_USAGE.getWithPrefix());
                    return true;
                }
                String deleteName = args[1];
                
                // Validate delete name input
                if (!SignValidationUtil.isValidSignName(deleteName)) {
                    player.sendMessage(Lang.PREFIX.get() + "§cInvalid sign name. Use only letters, numbers, hyphens, and underscores (max 32 characters).");
                    return true;
                }
                
                if (signLibraryManager.getSign(player, deleteName) == null) {
                    player.sendMessage(Lang.SAVED_SIGN_NOT_FOUND.getWithPrefix());
                } else {
                    signLibraryManager.deleteSign(player, deleteName);
                    // Record command usage
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().recordCommandUse(player, "delete");
                }
                break;
            }
            case "load": {
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.load", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Check library permission for library commands
                if (!Permissions.canLoadFromLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfig().getBoolean("features.sign-library", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cSign library is currently disabled.");
                    return true;
                }
                // Check cooldown
                if (!us.ironcladnetwork.copySign.CopySign.getCooldownManager().canUseCommand(player, "load")) {
                    us.ironcladnetwork.copySign.CopySign.getCooldownManager().sendCooldownMessage(player, "load");
                    return true;
                }
                // Usage: /copysign load [name]
                if (args.length < 2) {
                    player.sendMessage(Lang.COPYSIGN_USAGE.getWithPrefix());
                    return true;
                }
                String loadName = args[1];
                
                // Validate load name input
                if (!SignValidationUtil.isValidSignName(loadName)) {
                    player.sendMessage(Lang.PREFIX.get() + "§cInvalid sign name. Use only letters, numbers, hyphens, and underscores (max 32 characters).");
                    return true;
                }
                
                SavedSignData savedData = signLibraryManager.getSign(player, loadName);
                if (savedData == null) {
                    player.sendMessage(Lang.SAVED_SIGN_NOT_FOUND.getWithPrefix());
                    return true;
                }
                ItemStack heldItemForLoad = player.getInventory().getItemInMainHand();
                if (heldItemForLoad == null || heldItemForLoad.getType() == Material.AIR || !heldItemForLoad.getType().name().endsWith("_SIGN")) {
                    player.sendMessage(Lang.MUST_HOLD_SIGN.getWithPrefix());
                    return true;
                }
                // Check if the held sign type is allowed
                if (!SignValidationUtil.isSignTypeAllowed(heldItemForLoad.getType().name())) {
                    player.sendMessage(Lang.SIGN_TYPE_NOT_ALLOWED_LOAD.getWithPrefix());
                    return true;
                }
                boolean heldHanging = heldItemForLoad.getType().name().contains("HANGING_SIGN");
                boolean savedHanging = savedData.getSignType().equalsIgnoreCase("hanging");
                if (heldHanging != savedHanging) {
                    player.sendMessage(Lang.SIGN_TYPE_MISMATCH.formatWithPrefix(
                        "%held%", heldHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get(),
                        "%target%", savedHanging ? Lang.HANGING_SIGN.get() : Lang.REGULAR_SIGN.get()));
                    return true;
                }
                NBTItem nbtItemForLoad = new NBTItem(heldItemForLoad);
                // Validate sign data before storing in NBT
                if (!NBTValidationUtil.validateSignData(savedData.getFront(), savedData.getBack())) {
                    player.sendMessage(Lang.PREFIX.get() + "§cInvalid sign data: exceeds size limits");
                    return true;
                }
                // Combine arrays into newline-delimited strings.
                String frontText = String.join("\n", savedData.getFront());
                String backText = String.join("\n", savedData.getBack());
                // Additional validation for combined strings
                if (!NBTValidationUtil.validateNBTData(frontText) || !NBTValidationUtil.validateNBTData(backText)) {
                    player.sendMessage(Lang.PREFIX.get() + "§cInvalid sign data: text too large");
                    return true;
                }
                nbtItemForLoad.setString("copiedSignFront", frontText);
                nbtItemForLoad.setString("copiedSignBack", backText);
                nbtItemForLoad.setString("copiedSignFrontColor", savedData.getFrontColor());
                nbtItemForLoad.setString("copiedSignBackColor", savedData.getBackColor());
                nbtItemForLoad.setBoolean("signGlowing", savedData.isGlowing());
                nbtItemForLoad.setString("signType", savedData.getSignType());

                // Update lore for visual display.
                ItemStack updatedHeldItem = nbtItemForLoad.getItem();
                ItemMeta meta = updatedHeldItem.getItemMeta();
                if (meta != null) {
                    List<String> lore = SignLoreBuilder.buildSignLore(
                        savedData.getFront(), savedData.getBack(),
                        savedData.getFrontColor(), savedData.getBackColor(),
                        savedData.isGlowing(), savedData.getSignType()
                    );
                    meta.setLore(lore);
                    updatedHeldItem.setItemMeta(meta);
                }
                player.getInventory().setItemInMainHand(updatedHeldItem);
                player.sendMessage(Lang.SIGN_LOADED.getWithPrefix());
                // Record command usage
                us.ironcladnetwork.copySign.CopySign.getCooldownManager().recordCommandUse(player, "load");
                break;
            }
            case "reload": {
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.reload", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Check reload permission
                if (!Permissions.canReload(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_RELOAD.getWithPrefix());
                    return true;
                }
                // Call the reload method from the main plugin class
                us.ironcladnetwork.copySign.CopySign.getInstance().reloadPlugin();
                player.sendMessage(Lang.PLUGIN_RELOADED.getWithPrefix());
                break;
            }
            case "templates": {
                // Check if the command is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.templates", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cThis command is currently disabled.");
                    return true;
                }
                // Check if the server-templates feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfig().getBoolean("features.server-templates", true)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cServer templates are currently disabled.");
                    return true;
                }
                // Check if player has permission to view templates
                if (!Permissions.canViewTemplates(player)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cYou don't have permission to view server templates!");
                    return true;
                }
                // Get all server templates
                Map<String, SavedSignData> templates = us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().getAllTemplates();
                if (templates.isEmpty() && !Permissions.canCreateTemplates(player)) {
                    player.sendMessage(Lang.PREFIX.get() + "&cNo server templates are available.");
                    return true;
                }
                // Open the GUI
                boolean canEdit = Permissions.canEditTemplates(player);
                us.ironcladnetwork.copySign.GUI.ServerTemplateGUI.open(player, templates, canEdit);
                break;
            }
            default:
                player.sendMessage(Lang.COPYSIGN_USAGE.getWithPrefix());
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Check if sender is a player and has basic permission
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        Player player = (Player) sender;
        if (!Permissions.canUse(player)) {
            return Collections.emptyList();
        }

        // Build options list based on permissions and command toggles
        List<String> options = new ArrayList<>();
        
        // Basic commands available to all users with copysign.use (check command toggles)
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.on", true)) {
            options.add("on");
        }
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.off", true)) {
            options.add("off");
        }
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.clear", true)) {
            options.add("clear");
        }
        
        // Library commands based on specific permissions and command toggles
        if (Permissions.canSaveToLibrary(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.save", true)) {
            options.add("save");
        }
        if (Permissions.canViewLibrary(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.library", true)) {
            options.add("library");
        }
        if (Permissions.canDeleteFromLibrary(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.delete", true)) {
            options.add("delete");
        }
        if (Permissions.canLoadFromLibrary(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.load", true)) {
            options.add("load");
        }
        
        // Reload command only available with copysign.reload permission and command toggle
        if (Permissions.canReload(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.reload", true)) {
            options.add("reload");
        }
        
        // Templates command available with copysign.templates permission and command toggle
        if (Permissions.canViewTemplates(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.templates", true)) {
            options.add("templates");
        }

        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ENGLISH);
            List<String> completions = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(current))
                    completions.add(option);
            }
            return completions;
        }
        // For subcommands that require a sign name, auto-complete for "delete" and "load" only.
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ENGLISH);
            if ((subCommand.equals("delete") && Permissions.canDeleteFromLibrary(player)) || 
                (subCommand.equals("load") && Permissions.canLoadFromLibrary(player))) {
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
        return Collections.emptyList();
    }
    
}
