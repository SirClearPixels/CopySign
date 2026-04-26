# CopySign Plugin

A powerful Minecraft Spigot plugin that allows players to copy and paste sign text, colors, and glow states with NBT data storage and a personal sign library system.

**Version 2.2.0** - Full compatibility with Minecraft 1.21.7!

## 📋 Table of Contents

- [Features](#-features)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Configuration](#-configuration)
- [Usage Guide](#-usage-guide)
- [Sign Library System](#-sign-library-system)
- [Server Templates System](#-server-templates-system)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [Support](#-support)

## ✨ Features

### Core Functionality
- **🔄 Copy & Paste Signs**: Copy text, colors, and glow states from existing signs
- **🎨 Color Preservation**: Maintains exact text colors and formatting
- **✨ Glow State Support**: Preserves glowing text effects
- **🏷️ Sign Type Compatibility**: Works with both regular and hanging signs
- **👤 Personal Toggle System**: Individual enable/disable for each player
- **📚 Sign Library**: Save and reuse frequently used sign templates
- **🖥️ GUI Management**: Easy-to-use graphical interface for library management
- **💾 NBT Data Storage**: Reliable, efficient data handling

### Advanced Features
- **🔍 Tab Completion**: Smart command completion for better UX
- **🛡️ Permission System**: Granular control over feature access
- **📊 bStats Integration**: Anonymous usage statistics (optional)
- **🔧 Reload Support**: Hot-reload configuration without restart
- **🌐 Multi-language Ready**: Customizable message system
- **🛡️ WorldGuard Support**: Respects region protections (optional)
- **🔊 Sound Effects**: Customizable feedback sounds for all actions
- **⚡ Performance Optimized**: Thread-safe operations and efficient caching

## 📋 Requirements

- **Minecraft**: 1.21.7+ (tested with 1.21.7)
- **Server Software**: Spigot, Paper, or compatible forks
- **Java**: 21+ (compiled with Java 21)
- **Dependencies**: 
  - [NBT-API v2.15.1+](https://www.spigotmc.org/resources/nbt-api.7939/) (Required)
  - [WorldGuard](https://enginehub.org/worldguard) (Optional - for region protection)

## 🚀 Installation

### Step 1: Install Dependencies
1. Download [NBT-API plugin](https://www.spigotmc.org/resources/nbt-api.7939/) (v2.15.1 or newer)
2. Place `NBTAPI-X.X.X.jar` in your server's `plugins/` folder
3. (Optional) Download [WorldGuard](https://enginehub.org/worldguard) for region protection
4. Restart your server to load dependencies

### Step 2: Install CopySign
1. Download the latest `CopySign-2.2.0.jar`
2. Place the jar file in your server's `plugins/` folder
3. Restart your server

### Step 3: Configure Permissions
Grant basic permissions to your players:
```yaml
# For basic users
permissions:
  copysign.use: true

# For trusted builders
permissions:
  copysign.use: true
  copysign.copycolor: true
  copysign.library: true

# For staff/advanced users
permissions:
  copysign.use: true
  copysign.copycolor: true
  copysign.copyglow: true
  copysign.library: true
  copysign.reload: true
  copysign.admin: true
```

### Step 4: Verify Installation
1. Check server console for successful plugin loading
2. Run `/copysign` in-game to verify functionality
3. Check that `plugins/CopySign/` folder was created with config files

## 🎯 Quick Start

### Basic Usage (5 minutes)
1. **Enable the feature**: `/copysign on`
2. **Get a sign**: Hold any sign item in your hand
3. **Copy existing sign**: Shift + Left-click on a sign with text
4. **Place copied sign**: Place your sign item normally - text applies automatically!

### Library Usage
1. **Save a sign**: After copying, use `/copysign save MyTemplate`
2. **Load saved sign**: Hold a sign and use `/copysign load MyTemplate`
3. **Browse library**: Use `/copysign library` for GUI management

## 📝 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/copysign on` | Enable copy feature | `copysign.use` |
| `/copysign off` | Disable copy feature | `copysign.use` |
| `/copysign clear` | Clear NBT data from held sign | `copysign.use` |
| `/copysign save <name>` | Save sign to personal library | `copysign.library` |
| `/copysign load <name>` | Load sign from library | `copysign.library` |
| `/copysign delete <name>` | Delete sign from library | `copysign.library` |
| `/copysign library` | Open library GUI | `copysign.library` |
| `/copysign reload` | Reload plugin configuration | `copysign.reload` |
| `/copysign templates` | Display template commands help | `copysign.templates` |
| `/copysign templates list` | Open server templates GUI | `copysign.templates` |
| `/copysign templates create <name>` | Create template from held sign | `copysign.templates.create` |
| `/copysign templates delete <name> [-force]` | Delete server template | `copysign.templates.delete` |
| `/copysign templates use <name>` | Load template onto held sign | `copysign.templates.use` |
| `/copysign confirm` | Confirm pending action | `copysign.use` |
| `/copysign cancel` | Cancel pending action | `copysign.use` |

**Aliases**: `/cs` can be used instead of `/copysign`

## 🔐 Permissions

### Core Permissions
- `copysign.use` - Basic copy/paste functionality *(default: op)*
- `copysign.copycolor` - Copy sign text colors *(default: op)*
- `copysign.copyglow` - Copy sign glow states *(default: op)*
- `copysign.library` - Access sign library features *(default: op)*
- `copysign.library.save` - Save signs to personal library *(default: op)*
- `copysign.library.load` - Load signs from personal library *(default: op)*
- `copysign.library.delete` - Delete signs from personal library *(default: op)*
- `copysign.library.view` - View personal library *(default: op)*

### Sign Type Permissions
- `copysign.copy.regular` - Copy from regular signs *(default: op)*
- `copysign.copy.hanging` - Copy from hanging signs *(default: op)*
- `copysign.paste.regular` - Paste to regular signs *(default: op)*
- `copysign.paste.hanging` - Paste to hanging signs *(default: op)*

### Administrative Permissions
- `copysign.reload` - Reload plugin configuration *(default: op)*
- `copysign.templates` - View server templates *(default: op)*
- `copysign.templates.create` - Create server templates *(default: op)*
- `copysign.templates.delete` - Delete server templates *(default: op)*
- `copysign.templates.use` - Use server templates *(default: op)*
- `copysign.admin` - Full administrative access *(default: op)*

### Bypass Permissions
- `copysign.bypass.cooldowns` - Bypass command cooldowns *(default: false)*
- `copysign.bypass.limits` - Bypass various plugin limits *(default: false)*
- `copysign.bypass.worlds` - Use in disabled worlds *(default: false)*
- `copysign.bypass.worldguard` - Bypass WorldGuard protection *(default: false)*

### Permission Groups
```yaml
# Basic User Group
copysign_basic:
  permissions:
    - copysign.use

# Builder Group  
copysign_builder:
  permissions:
    - copysign.use
    - copysign.copycolor
    - copysign.library

# Staff Group
copysign_staff:
  permissions:
    - copysign.*
```

## ⚙️ Configuration

### config.yml
```yaml
# CopySign Configuration
settings:
  # Default state for new players
  default-enabled: true
  
  # Maximum saved signs per player (0 = unlimited)
  max-saved-signs: 50
  
  # Enable bStats metrics
  metrics: true
  
  # Debug mode
  debug: false

# Command cooldowns (in seconds)
cooldowns:
  save: 1
  load: 1
  library: 2
```

### messages.yml
Customize all plugin messages with color codes:
```yaml
messages:
  prefix: "&8[&bCopySign&8] &7"
  sign-copied: "&aSign text copied to your held sign!"
  feature-enabled: "&aSign copy feature enabled."
  feature-disabled: "&cSign copy feature disabled."
  # ... more messages
```

## 📖 Usage Guide

### Copying Signs
1. **Hold a sign item** in your main hand
2. **Find the target sign** you want to copy
3. **Shift + Left-click** the target sign
4. **Success message** confirms the copy operation
5. **Check the lore** on your sign item to see copied data

### Pasting Signs
1. **Place the sign** with copied data normally
2. **Text applies automatically** during placement
3. **Colors and glow** are preserved (with proper permissions)

### Managing Your Library
#### Saving Signs
```bash
# After copying a sign
/copysign save WelcomeSign
/copysign save ShopHeader
/copysign save Rules1
```

#### Loading Signs
```bash
# Hold a sign item first
/copysign load WelcomeSign
# Place the sign - template applies automatically
```

#### GUI Management
```bash
/copysign library
# Click signs to load them
# Right-click to delete them
```

### Advanced Tips
- **Organize with naming**: Use descriptive names like `Shop_Header`, `Welcome_Main`, `Rules_Page1`
- **Color copying**: Requires `copysign.copycolor` permission
- **Glow effects**: Requires `copysign.copyglow` permission  
- **Sign compatibility**: Regular signs ↔ Regular signs, Hanging signs ↔ Hanging signs
- **Tab completion**: Use Tab key for command and name completion

## 📚 Sign Library System

### Personal Libraries
- Each player has their own private sign library
- No limit on saved signs (configurable)
- Persistent across server restarts
- Organized by custom names

### GUI Features
- **Visual browsing**: See all saved signs at a glance
- **One-click loading**: Click to apply template to held sign
- **Easy deletion**: Right-click to remove templates
- **Search-friendly**: Organized display with clear names

### Library Commands
```bash
# Save current copied sign
/copysign save <name>

# Load saved template
/copysign load <name>

# Delete template
/copysign delete <name>

# Open GUI browser
/copysign library
```

### Best Practices
- Use consistent naming conventions
- Regularly clean up unused templates
- Save complex signs with colors/formatting
- Create templates for common use cases

## 🌐 Server Templates System

### Overview
Server templates are administrator-managed sign templates available to all players with proper permissions. Unlike personal libraries, server templates provide standardized signs for common server needs.

### Managing Server Templates
#### Creating Templates
```bash
# Copy a sign first (Shift + Left-click)
/copysign templates create welcome-spawn
/copysign templates create shop-header
/copysign templates create rules-section1
```

#### Using Templates
```bash
# Hold a sign item first
/copysign templates use welcome-spawn
# Place the sign - template applies automatically
```

#### Deleting Templates
```bash
# Delete with confirmation prompt
/copysign templates delete old-template

# Skip confirmation with -force flag
/copysign templates delete old-template -force
```

#### Viewing Templates
```bash
# Open GUI to browse all server templates
/copysign templates list
```

### Template Features
- **Server-wide availability**: Accessible to all players with permission
- **Admin-controlled**: Only administrators can create/delete
- **Sign type validation**: Ensures compatibility between template and held sign
- **Confirmation prompts**: Prevents accidental deletion (configurable)
- **Tab completion**: Smart completion for template names

### Template Permissions
- `copysign.templates` - View and list server templates
- `copysign.templates.use` - Load templates onto signs
- `copysign.templates.create` - Create new server templates
- `copysign.templates.delete` - Delete existing templates

## 🔧 Troubleshooting

### Common Issues

#### "Plugin not working"
- ✅ Verify NBT-API is installed and loaded
- ✅ Check server console for error messages
- ✅ Ensure Java 21+ is being used
- ✅ Confirm Minecraft version compatibility (1.21+)

#### "No permission" errors
- ✅ Grant `copysign.use` permission
- ✅ Check permission plugin configuration
- ✅ Verify player has required permissions for specific features

#### "Can't copy signs"
- ✅ Enable feature with `/copysign on`
- ✅ Hold a sign item in main hand
- ✅ Use Shift + Left-click on target sign
- ✅ Ensure target is a valid sign block

#### "Library not working"
- ✅ Grant `copysign.library` permission
- ✅ Check if `savedSigns.yml` exists and is writable
- ✅ Verify sign was copied before saving

#### "Template commands not working"
- ✅ Grant appropriate template permissions (`copysign.templates.*`)
- ✅ For creation: Ensure you're holding a sign with copied data
- ✅ For use: Ensure you're holding a blank sign item
- ✅ Check template name is valid (alphanumeric, hyphens, underscores only)
- ✅ Verify template exists with `/copysign templates list`
- ✅ Check console for any error messages

#### "Confirmation prompts timing out"
- ✅ Use `/copysign confirm` within 30 seconds
- ✅ Or use `-force` flag to skip confirmation: `/copysign templates delete <name> -force`
- ✅ Check `templates.require-confirmation-on-delete` in config.yml

### Debug Mode
Enable debug logging in `config.yml`:
```yaml
settings:
  debug: true
```

### Getting Help
1. Check server console for error messages
2. Enable debug mode for detailed logging
3. Verify all requirements are met
4. Check file permissions in plugin folder

### Development Setup
1. Clone the repository
2. Ensure Java 21+ and Maven are installed
3. Run `mvn clean compile` to build
4. Use `mvn clean package` to create plugin jar

### Reporting Issues
- Report to GitHub
- Include server version, plugin version, and error logs
- Describe steps to reproduce the problem

## 📞 Support

### Getting Help
- **Documentation**: Check this README and in-game `/copysign help`

### Server Compatibility
- **Tested on**: Spigot 1.21.7, Paper 1.21.7
- **Compatible with**: Most Spigot-based servers
- **Java versions**: Compiled with Java 21, requires Java 21+

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **NBT-API** by tr7zw for NBT data handling
- **bStats** for plugin metrics
- **Spigot Community** for API documentation and support

---

**Made with ❤️ for the Minecraft community**