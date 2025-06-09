# CopySign Plugin

A powerful Minecraft Spigot plugin that allows players to copy and paste sign text, colors, and glow states with NBT data storage and a personal sign library system.

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

## 📋 Requirements

- **Minecraft**: 1.21+ (tested with 1.21.1)
- **Server Software**: Spigot, Paper, or compatible forks
- **Java**: 21+ (compiled with Java 21)
- **Dependencies**: 
  - [NBT-API v2.12.2+](https://www.spigotmc.org/resources/nbt-api.7939/) (Required)

## 🚀 Installation

### Step 1: Install Dependencies
1. Download [NBT-API plugin](https://www.spigotmc.org/resources/nbt-api.7939/)
2. Place `NBTAPI-X.X.X.jar` in your server's `plugins/` folder
3. Restart your server to load NBT-API

### Step 2: Install CopySign
1. Download the latest `CopySign-X.X-X.jar`
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
| `/copysign templates` | View server templates | `copysign.templates` |

**Aliases**: `/cs` can be used instead of `/copysign`

## 🔐 Permissions

### Core Permissions
- `copysign.use` - Basic copy/paste functionality *(default: op)*
- `copysign.copycolor` - Copy sign text colors *(default: op)*
- `copysign.copyglow` - Copy sign glow states *(default: op)*
- `copysign.library` - Access sign library features *(default: op)*

### Administrative Permissions
- `copysign.reload` - Reload plugin configuration *(default: op)*
- `copysign.templates` - View server templates *(default: op)*
- `copysign.admin` - Manage server templates *(default: op)*

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
- **Tested on**: Spigot 1.21.1, Paper 1.21.1
- **Compatible with**: Most Spigot-based servers
- **Java versions**: Compiled with Java 21, compatible with Java 8+

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **NBT-API** by tr7zw for NBT data handling
- **bStats** for plugin metrics
- **Spigot Community** for API documentation and support

---

**Made with ❤️ for the Minecraft community**