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

        // No arguments: display help
        if (args.length == 0) {
            displayMainHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ENGLISH);
        
        switch(subCommand) {
            case "on":
                // Set state to enabled and persist it.
                toggleManager.setCopySignEnabled(player, true);
                player.sendMessage(Lang.COPYSIGN_ENABLED.getWithPrefix());
                break;
            case "off":
                // Set state to disabled and persist it.
                toggleManager.setCopySignEnabled(player, false);
                player.sendMessage(Lang.COPYSIGN_DISABLED.getWithPrefix());
                break;
            case "clear":
                // Check if the clear command feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isClearCommandEnabled()) {
                    player.sendMessage(Lang.COMMAND_FEATURE_DISABLED.formatWithPrefix("%feature%", "Clear command"));
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
                    // Clear lore and display name to restore default Minecraft item name.
                    ItemMeta meta = updatedItem.getItemMeta();
                    if (meta != null) {
                        meta.setLore(null);
                        meta.setDisplayName(null);
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
                // Check library permission for library commands
                if (!Permissions.canSaveToLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                    player.sendMessage(Lang.COMMAND_FEATURE_DISABLED.formatWithPrefix("%feature%", "Sign library"));
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
                    player.sendMessage(Lang.INVALID_SIGN_NAME_FORMAT.getWithPrefix());
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
                // Record command usage
                us.ironcladnetwork.copySign.CopySign.getCooldownManager().recordCommandUse(player, "save");
                break;
            }
            case "library": {
                // Check library permission for library commands
                if (!Permissions.canViewLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                    player.sendMessage(Lang.COMMAND_FEATURE_DISABLED.formatWithPrefix("%feature%", "Sign library"));
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
                // Command enable check already done above
                // Check library permission for library commands
                if (!Permissions.canDeleteFromLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                    player.sendMessage(Lang.COMMAND_FEATURE_DISABLED.formatWithPrefix("%feature%", "Sign library"));
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
                    player.sendMessage(Lang.INVALID_SIGN_NAME_FORMAT.getWithPrefix());
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
                // Check library permission for library commands
                if (!Permissions.canLoadFromLibrary(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_LIBRARY.getWithPrefix());
                    return true;
                }
                // Check if the sign-library feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                    player.sendMessage(Lang.COMMAND_FEATURE_DISABLED.formatWithPrefix("%feature%", "Sign library"));
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
                    player.sendMessage(Lang.INVALID_SIGN_NAME_FORMAT.getWithPrefix());
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
                    player.sendMessage(Lang.SIGN_DATA_SIZE_EXCEEDED.getWithPrefix());
                    return true;
                }
                // Combine arrays into newline-delimited strings.
                String frontText = String.join("\n", savedData.getFront());
                String backText = String.join("\n", savedData.getBack());
                // Additional validation for combined strings
                if (!NBTValidationUtil.validateNBTData(frontText) || !NBTValidationUtil.validateNBTData(backText)) {
                    player.sendMessage(Lang.SIGN_DATA_TEXT_TOO_LARGE.getWithPrefix());
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
            case "validate": {
                // Check validate permission (same as reload for admin)
                if (!Permissions.canReload(player)) {
                    player.sendMessage(Lang.NO_PERMISSION_RELOAD.getWithPrefix());
                    return true;
                }
                
                // Perform validation
                player.sendMessage(Lang.PREFIX.get() + "§eValidating configuration...");
                us.ironcladnetwork.copySign.Util.ConfigValidator validator = 
                    new us.ironcladnetwork.copySign.Util.ConfigValidator(us.ironcladnetwork.copySign.CopySign.getInstance());
                
                boolean valid = validator.validate();
                
                // Send results to player
                if (valid) {
                    player.sendMessage(Lang.PREFIX.get() + "§aConfiguration validation passed!");
                } else {
                    player.sendMessage(Lang.PREFIX.get() + "§cConfiguration has errors!");
                    
                    // Show errors
                    for (String error : validator.getErrors()) {
                        player.sendMessage("§c ▸ " + error);
                    }
                }
                
                // Show warnings if any
                if (!validator.getWarnings().isEmpty()) {
                    player.sendMessage(Lang.PREFIX.get() + "§eConfiguration warnings:");
                    for (String warning : validator.getWarnings()) {
                        player.sendMessage("§e ▸ " + warning);
                    }
                }
                
                break;
            }
            case "confirm": {
                // Handle confirmation
                if (us.ironcladnetwork.copySign.CopySign.getConfirmationManager().hasPendingConfirmation(player.getUniqueId())) {
                    if (us.ironcladnetwork.copySign.CopySign.getConfirmationManager().confirm(player.getUniqueId())) {
                        // Success message is handled by the action itself
                    }
                } else {
                    player.sendMessage(Lang.NO_PENDING_CONFIRMATIONS.getWithPrefix());
                }
                break;
            }
            case "cancel": {
                // Handle cancellation
                if (us.ironcladnetwork.copySign.CopySign.getConfirmationManager().cancelConfirmation(player.getUniqueId())) {
                    player.sendMessage(Lang.ACTION_CANCELLED.getWithPrefix());
                } else {
                    player.sendMessage(Lang.NO_PENDING_CONFIRMATIONS.getWithPrefix());
                }
                break;
            }
            case "templates": {
                // Check if the server-templates feature is enabled in config
                if (!us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isServerTemplatesEnabled()) {
                    player.sendMessage(Lang.COMMAND_FEATURE_DISABLED.formatWithPrefix("%feature%", "Server templates"));
                    return true;
                }
                
                // Handle template subcommands
                if (args.length == 1) {
                    // Display help for template commands
                    displayTemplateHelp(player);
                    return true;
                }
                
                String templateSubCommand = args[1].toLowerCase(Locale.ENGLISH);
                switch (templateSubCommand) {
                    case "list":
                        handleTemplateList(player);
                        break;
                    case "create":
                        handleTemplateCreate(player, args);
                        break;
                    case "delete":
                        handleTemplateDelete(player, args);
                        break;
                    case "use":
                    case "load":
                        handleTemplateUse(player, args);
                        break;
                    default:
                        displayTemplateHelp(player);
                        break;
                }
                break;
            }
            default:
                displayMainHelp(player);
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
        
        // Add confirm/cancel if player has pending confirmation
        if (us.ironcladnetwork.copySign.CopySign.getConfirmationManager().hasPendingConfirmation(player.getUniqueId())) {
            options.add("confirm");
            options.add("cancel");
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
        
        // Validate command only available with copysign.reload permission and command toggle
        if (Permissions.canReload(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.validate", true)) {
            options.add("validate");
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
            // Handle templates subcommands
            if (subCommand.equals("templates")) {
                List<String> templateSubCommands = new ArrayList<>();
                if (Permissions.canViewTemplates(player)) {
                    templateSubCommands.add("list");
                }
                if (Permissions.canCreateTemplates(player)) {
                    templateSubCommands.add("create");
                }
                if (Permissions.canDeleteTemplates(player)) {
                    templateSubCommands.add("delete");
                }
                if (Permissions.canUseTemplates(player)) {
                    templateSubCommands.add("use");
                    templateSubCommands.add("load");
                }
                String current = args[1].toLowerCase(Locale.ENGLISH);
                List<String> completions = new ArrayList<>();
                for (String cmd : templateSubCommands) {
                    if (cmd.startsWith(current))
                        completions.add(cmd);
                }
                return completions;
            }
        }
        // For template subcommands that require a template name
        if (args.length == 3 && args[0].toLowerCase(Locale.ENGLISH).equals("templates")) {
            String templateSubCommand = args[1].toLowerCase(Locale.ENGLISH);
            if ((templateSubCommand.equals("delete") && Permissions.canDeleteTemplates(player)) || 
                (templateSubCommand.equals("use") && Permissions.canUseTemplates(player)) ||
                (templateSubCommand.equals("load") && Permissions.canUseTemplates(player))) {
                Map<String, SavedSignData> templates = us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().getAllTemplates();
                
                // Filter templates based on configuration
                boolean hideSystemTemplates = us.ironcladnetwork.copySign.CopySign.getInstance()
                    .getConfigBoolean("templates.hide-system-templates-in-tab", false);
                String systemPrefix = us.ironcladnetwork.copySign.CopySign.getInstance()
                    .getConfigString("templates.system-template-prefix", "system_");
                
                List<String> names = new ArrayList<>();
                for (String templateName : templates.keySet()) {
                    // Skip system templates if configured to hide them
                    if (hideSystemTemplates && templateName.startsWith(systemPrefix)) {
                        continue;
                    }
                    names.add(templateName);
                }
                
                String current = args[2].toLowerCase(Locale.ENGLISH);
                List<String> completions = new ArrayList<>();
                for (String name : names) {
                    if (name.toLowerCase(Locale.ENGLISH).startsWith(current))
                        completions.add(name);
                }
                return completions;
            }
        }
        
        // -force flag for delete command
        if (args.length == 4 && args[0].toLowerCase(Locale.ENGLISH).equals("templates") && 
            args[1].toLowerCase(Locale.ENGLISH).equals("delete")) {
            // Only suggest -force if the current argument starts with -
            String current = args[3].toLowerCase(Locale.ENGLISH);
            if ("-force".startsWith(current)) {
                return Collections.singletonList("-force");
            }
        }
        return Collections.emptyList();
    }
    
    /**
     * Display help message for template commands
     */
    private void displayTemplateHelp(Player player) {
        player.sendMessage(Lang.TEMPLATE_HELP_HEADER.getWithPrefix());
        player.sendMessage(Lang.TEMPLATE_HELP_LIST.get());
        player.sendMessage(Lang.TEMPLATE_HELP_CREATE.get());
        player.sendMessage(Lang.TEMPLATE_HELP_DELETE.get());
        player.sendMessage(Lang.TEMPLATE_HELP_USE.get());
        player.sendMessage("");
        player.sendMessage(Lang.TEMPLATE_HELP_EXAMPLES.get());
        player.sendMessage(Lang.TEMPLATE_HELP_EXAMPLE_CREATE.get());
        player.sendMessage(Lang.TEMPLATE_HELP_EXAMPLE_USE.get());
    }
    
    /**
     * Display main help message for /copysign command
     */
    private void displayMainHelp(Player player) {
        player.sendMessage(Lang.COMMAND_HELP_HEADER.getWithPrefix());
        
        // Show commands based on permissions and config
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.on", true)) {
            player.sendMessage(Lang.COMMAND_HELP_ON.get());
        }
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.off", true)) {
            player.sendMessage(Lang.COMMAND_HELP_OFF.get());
        }
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.clear", true) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isClearCommandEnabled()) {
            player.sendMessage(Lang.COMMAND_HELP_CLEAR.get());
        }
        
        // Library commands (require library permission)
        if (Permissions.canUseLibrary(player)) {
            if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.save", true) && 
                us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                player.sendMessage(Lang.COMMAND_HELP_SAVE.get());
            }
            if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.load", true) && 
                us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                player.sendMessage(Lang.COMMAND_HELP_LOAD.get());
            }
            if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.delete", true) && 
                us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                player.sendMessage(Lang.COMMAND_HELP_DELETE.get());
            }
            if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.library", true) && 
                us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isSignLibraryEnabled()) {
                player.sendMessage(Lang.COMMAND_HELP_LIBRARY.get());
            }
        }
        
        // Admin commands
        if (Permissions.canReload(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.reload", true)) {
            player.sendMessage(Lang.COMMAND_HELP_RELOAD.get());
        }
        
        // Validate command (also admin)
        if (Permissions.canReload(player) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.validate", true)) {
            player.sendMessage(Lang.COMMAND_HELP_VALIDATE.get());
        }
        
        // Template command (check various template permissions)
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.templates", true) && 
            us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().isServerTemplatesEnabled() &&
            (player.hasPermission("copysign.templates.view") || 
             player.hasPermission("copysign.templates.create") ||
             player.hasPermission("copysign.templates.delete") ||
             player.hasPermission("copysign.templates.use"))) {
            player.sendMessage(Lang.COMMAND_HELP_TEMPLATES.get());
        }
        
        // Confirmation commands (always show if enabled)
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.confirm", true)) {
            player.sendMessage(Lang.COMMAND_HELP_CONFIRM.get());
        }
        if (us.ironcladnetwork.copySign.CopySign.getInstance().getConfigBoolean("commands.enabled.cancel", true)) {
            player.sendMessage(Lang.COMMAND_HELP_CANCEL.get());
        }
    }
    
    /**
     * Handle template list command - opens the GUI
     */
    private void handleTemplateList(Player player) {
        // Check if player has permission to view templates
        if (!Permissions.canViewTemplates(player)) {
            player.sendMessage(Lang.TEMPLATE_NO_PERMISSION_VIEW.getWithPrefix());
            return;
        }
        
        // Get all server templates
        Map<String, SavedSignData> templates = us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().getAllTemplates();
        if (templates.isEmpty() && !Permissions.canCreateTemplates(player)) {
            player.sendMessage(Lang.TEMPLATE_LIST_EMPTY.getWithPrefix());
            return;
        }
        
        // Open the GUI
        boolean canEdit = Permissions.canEditTemplates(player);
        us.ironcladnetwork.copySign.GUI.ServerTemplateGUI.open(player, templates, canEdit);
    }
    
    /**
     * Handle template create command
     */
    private void handleTemplateCreate(Player player, String[] args) {
        // Check if player has permission to create templates
        if (!Permissions.canCreateTemplates(player)) {
            player.sendMessage(Lang.TEMPLATE_NO_PERMISSION_CREATE.getWithPrefix());
            return;
        }
        
        // Check arguments
        if (args.length < 3) {
            player.sendMessage(Lang.TEMPLATE_USAGE_CREATE.getWithPrefix());
            return;
        }
        
        String templateName = args[2];
        
        // Validate template name
        if (!SignValidationUtil.isValidSignName(templateName)) {
            player.sendMessage(Lang.INVALID_TEMPLATE_NAME.getWithPrefix());
            return;
        }
        
        // Check if template already exists
        if (us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().getTemplate(templateName) != null) {
            player.sendMessage(Lang.TEMPLATE_ALREADY_EXISTS.getWithPrefix());
            return;
        }
        
        // Check held item for sign data
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.getType().name().endsWith("_SIGN")) {
            player.sendMessage(Lang.MUST_HOLD_SIGN_WITH_DATA.getWithPrefix());
            return;
        }
        
        NBTItem nbtItem = new NBTItem(heldItem);
        if (!nbtItem.hasTag("copiedSignFront") || !nbtItem.hasTag("copiedSignBack")) {
            player.sendMessage(Lang.MUST_HOLD_SIGN_WITH_DATA.getWithPrefix());
            return;
        }
        
        // Save template using the sign item directly
        boolean success = us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().saveTemplate(player, templateName, heldItem);
        if (success) {
            player.sendMessage(Lang.TEMPLATE_CREATED.formatWithPrefix("%name%", templateName));
            // Log template creation
            us.ironcladnetwork.copySign.CopySign.getInstance().getLogger().info(
                "Player " + player.getName() + " created template '" + templateName + "'"
            );
        } else {
            player.sendMessage(Lang.TEMPLATE_CREATE_FAILED.getWithPrefix());
        }
    }
    
    /**
     * Handle template delete command
     */
    private void handleTemplateDelete(Player player, String[] args) {
        // Check if player has permission to delete templates
        if (!Permissions.canDeleteTemplates(player)) {
            player.sendMessage(Lang.TEMPLATE_NO_PERMISSION_DELETE.getWithPrefix());
            return;
        }
        
        // Check arguments
        if (args.length < 3) {
            player.sendMessage(Lang.TEMPLATE_USAGE_DELETE.getWithPrefix());
            return;
        }
        
        String templateName = args[2];
        
        // Check if template exists
        if (us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().getTemplate(templateName) == null) {
            player.sendMessage(Lang.TEMPLATE_NOT_FOUND.formatWithPrefix("%name%", templateName));
            return;
        }
        
        // Check for -force flag
        boolean force = args.length >= 4 && args[3].equalsIgnoreCase("-force");
        
        // Define the deletion action
        Runnable deleteAction = () -> {
            boolean success = us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().deleteTemplate(player, templateName);
            if (success) {
                player.sendMessage(Lang.TEMPLATE_DELETED.formatWithPrefix("%name%", templateName));
                // Log template deletion
                us.ironcladnetwork.copySign.CopySign.getInstance().getLogger().info(
                    "Player " + player.getName() + " deleted template '" + templateName + "'"
                );
            } else {
                player.sendMessage(Lang.TEMPLATE_DELETE_FAILED.getWithPrefix());
            }
        };
        
        // If force flag is used or confirmation is disabled, delete immediately
        if (force || !us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().requireConfirmationOnDelete()) {
            deleteAction.run();
        } else {
            // Request confirmation
            player.sendMessage(Lang.TEMPLATE_DELETE_CONFIRMATION.formatWithPrefix("%name%", templateName));
            player.sendMessage(Lang.TEMPLATE_DELETE_CONFIRMATION_COMMAND.getWithPrefix());
            player.sendMessage(Lang.TEMPLATE_DELETE_CONFIRMATION_EXPIRE.getWithPrefix());
            
            us.ironcladnetwork.copySign.CopySign.getConfirmationManager().requestConfirmation(
                player,
                "delete template '" + templateName + "'",
                deleteAction,
                us.ironcladnetwork.copySign.CopySign.getInstance().getConfigManager().getConfirmationTimeout()
            );
        }
    }
    
    /**
     * Handle template use/load command
     */
    private void handleTemplateUse(Player player, String[] args) {
        // Check if player has permission to use templates
        if (!Permissions.canUseTemplates(player)) {
            player.sendMessage(Lang.TEMPLATE_NO_PERMISSION_USE.getWithPrefix());
            return;
        }
        
        // Check arguments
        if (args.length < 3) {
            player.sendMessage(Lang.TEMPLATE_USAGE_USE.getWithPrefix());
            return;
        }
        
        String templateName = args[2];
        
        // Get template
        SavedSignData templateData = us.ironcladnetwork.copySign.CopySign.getServerTemplateManager().getTemplate(templateName);
        if (templateData == null) {
            player.sendMessage(Lang.TEMPLATE_NOT_FOUND.formatWithPrefix("%name%", templateName));
            return;
        }
        
        // Check held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR || !heldItem.getType().name().endsWith("_SIGN")) {
            player.sendMessage(Lang.TEMPLATE_MUST_HOLD_SIGN.getWithPrefix());
            return;
        }
        
        // Check sign type compatibility
        boolean heldHanging = heldItem.getType().name().contains("HANGING_SIGN");
        boolean templateHanging = templateData.getSignType().equalsIgnoreCase("hanging");
        if (heldHanging != templateHanging) {
            player.sendMessage(Lang.TEMPLATE_TYPE_MISMATCH.formatWithPrefix(
                "%template_type%", templateHanging ? "hanging" : "regular",
                "%held_type%", heldHanging ? "hanging" : "regular"
            ));
            return;
        }
        
        // Apply template to held sign
        NBTItem nbtItem = new NBTItem(heldItem);
        String frontText = String.join("\n", templateData.getFront());
        String backText = String.join("\n", templateData.getBack());
        
        nbtItem.setString("copiedSignFront", frontText);
        nbtItem.setString("copiedSignBack", backText);
        nbtItem.setString("copiedSignFrontColor", templateData.getFrontColor());
        nbtItem.setString("copiedSignBackColor", templateData.getBackColor());
        // Handle deprecated isGlowing method - check both front and back glow states
        boolean frontGlowing = templateData.isFrontGlowing();
        boolean backGlowing = templateData.isBackGlowing();
        nbtItem.setBoolean("signGlowing", frontGlowing || backGlowing);
        nbtItem.setBoolean("frontGlowing", frontGlowing);
        nbtItem.setBoolean("backGlowing", backGlowing);
        nbtItem.setString("signType", templateData.getSignType());
        
        // Update item with lore
        ItemStack updatedItem = nbtItem.getItem();
        ItemMeta meta = updatedItem.getItemMeta();
        if (meta != null) {
            List<String> lore = SignLoreBuilder.buildPremiumSignLore(
                null, // item name
                templateData.getFront(), templateData.getBack(),
                templateData.getFrontColor(), templateData.getBackColor(),
                frontGlowing, backGlowing, templateData.getSignType(),
                "template" // source type
            );
            meta.setLore(lore);
            updatedItem.setItemMeta(meta);
        }
        
        player.getInventory().setItemInMainHand(updatedItem);
        player.sendMessage(Lang.TEMPLATE_LOADED.formatWithPrefix("%name%", templateName));
        
        // Log template usage
        us.ironcladnetwork.copySign.CopySign.getInstance().getLogger().info(
            "Player " + player.getName() + " loaded template '" + templateName + "'"
        );
    }
    
    /**
     * Checks if a command is enabled in the configuration.
     * 
     * @param command The command name to check
     * @return true if the command is enabled, false otherwise
     */
    
}
