#!/usr/bin/env bash
# Topster Installation Script for Linux & macOS
# Supports: Ubuntu, Debian, Arch, Fedora, macOS

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}🎬 Topster Installation Script${NC}"
echo -e "${CYAN}=============================${NC}"
echo ""

# Detect OS
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
    echo -e "${GREEN}📦 Detected: Linux${NC}"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
    echo -e "${GREEN}📦 Detected: macOS${NC}"
else
    echo -e "${RED}❌ Unsupported OS: $OSTYPE${NC}"
    exit 1
fi

# Check for Bun
echo ""
echo -e "${BLUE}🔍 Checking for Bun installation...${NC}"
if ! command -v bun &> /dev/null; then
    echo -e "${YELLOW}📥 Installing Bun...${NC}"
    curl -fsSL https://bun.sh/install | bash
    
    # Add Bun to PATH for current session
    export BUN_INSTALL="$HOME/.bun"
    export PATH="$BUN_INSTALL/bin:$PATH"
    
    # Add to shell config
    if [[ "$OS" == "macos" ]]; then
        SHELL_CONFIG="$HOME/.zshrc"
    else
        SHELL_CONFIG="$HOME/.bashrc"
    fi
    
    if ! grep -q 'bun' "$SHELL_CONFIG"; then
        echo "" >> "$SHELL_CONFIG"
        echo "# Bun" >> "$SHELL_CONFIG"
        echo 'export BUN_INSTALL="$HOME/.bun"' >> "$SHELL_CONFIG"
        echo 'export PATH="$BUN_INSTALL/bin:$PATH"' >> "$SHELL_CONFIG"
        echo -e "${GREEN}✅ Added Bun to $SHELL_CONFIG${NC}"
        echo -e "${YELLOW}⚠️  Run: source $SHELL_CONFIG or restart your terminal${NC}"
    fi
else
    BUN_VERSION=$(bun --version)
    echo -e "${GREEN}✅ Bun already installed: $BUN_VERSION${NC}"
fi

# Check dependencies
echo ""
echo -e "${BLUE}🔍 Checking system dependencies...${NC}"

if [[ "$OS" == "linux" ]]; then
    # Check for media player
    PLAYER_FOUND=false
    if command -v mpv &> /dev/null; then
        echo -e "${GREEN}✅ mpv found${NC}"
        DEFAULT_PLAYER="mpv"
        PLAYER_FOUND=true
    elif command -v vlc &> /dev/null; then
        echo -e "${GREEN}✅ VLC found${NC}"
        DEFAULT_PLAYER="vlc"
        PLAYER_FOUND=true
    else
        echo -e "${YELLOW}⚠️  No media player found${NC}"
        echo "   Install mpv or VLC:"
        if command -v apt-get &> /dev/null; then
            echo "   sudo apt-get install mpv"
        elif command -v pacman &> /dev/null; then
            echo "   sudo pacman -S mpv"
        elif command -v dnf &> /dev/null; then
            echo "   sudo dnf install mpv"
        fi
        DEFAULT_PLAYER="mpv"
    fi
    
    # Check for ffmpeg
    if command -v ffmpeg &> /dev/null; then
        echo -e "${GREEN}✅ ffmpeg found${NC}"
    else
        echo -e "${YELLOW}⚠️  ffmpeg not found${NC}"
        echo "   Install for subtitle support:"
        if command -v apt-get &> /dev/null; then
            echo "   sudo apt-get install ffmpeg"
        elif command -v pacman &> /dev/null; then
            echo "   sudo pacman -S ffmpeg"
        elif command -v dnf &> /dev/null; then
            echo "   sudo dnf install ffmpeg"
        fi
    fi
fi

# Install dependencies
echo ""
echo -e "${BLUE}📦 Installing Topster dependencies...${NC}"
bun install

# Build project
echo ""
echo -e "${BLUE}🔨 Building Topster...${NC}"
bun run build

# Build binary with bun install
echo ""
echo -e "${BLUE}📦 Installing Topster binary...${NC}"
bun install --global

# Create config directory
CONFIG_DIR="$HOME/.topster"
echo ""
echo -e "${BLUE}📁 Creating config directory: $CONFIG_DIR${NC}"
mkdir -p "$CONFIG_DIR"

# Create default config if it doesn't exist
CONFIG_FILE="$CONFIG_DIR/config.json"
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo -e "${YELLOW}📝 Creating default config...${NC}"
    
    # Detect default language
    DEFAULT_LANG="en"
    if [[ -n "$LANG" ]]; then
        DEFAULT_LANG="${LANG%%.*}"
    fi
    
    cat > "$CONFIG_FILE" << EOF
{
  "player": "$DEFAULT_PLAYER",
  "quality": "auto",
  "language": "$DEFAULT_LANG",
  "subtitles": true,
  "historyFile": "$CONFIG_DIR/history.json",
  "cacheDir": "$CONFIG_DIR/cache",
  "logFile": "$CONFIG_DIR/debug.log",
  "mcpServer": {
    "enabled": true,
    "port": 3847
  }
}
EOF
    echo -e "${GREEN}✅ Config created: $CONFIG_FILE${NC}"
else
    echo -e "${GREEN}✅ Config already exists: $CONFIG_FILE${NC}"
fi

# Run initial typecheck
echo ""
echo -e "${BLUE}🔍 Running type check...${NC}"
bun run typecheck

# Summary
echo ""
echo -e "${CYAN}======================================${NC}"
echo -e "${GREEN}✅ Topster Installation Complete!${NC}"
echo -e "${CYAN}======================================${NC}"
echo ""
echo -e "${CYAN}📁 Installation directory: $(pwd)${NC}"
echo -e "${CYAN}⚙️  Config file: $CONFIG_FILE${NC}"
echo -e "${CYAN}📜 Log file: $CONFIG_DIR/debug.log${NC}"
echo ""
echo -e "${CYAN}🚀 Quick Start:${NC}"
echo ""
echo -e "  # Now you can just run:"
echo -e "  ${GREEN}topster${NC}"
echo ""
echo -e "  # Launch interactive TUI"
echo -e "  ${GREEN}topster${NC}"
echo ""
echo -e "  # Search for a movie"
echo -e "  ${GREEN}topster \"the matrix\"${NC}"
echo ""
echo -e "  # View trending"
echo -e "  ${GREEN}topster --trending${NC}"
echo ""
echo -e "  # Continue watching"
echo -e "  ${GREEN}topster --continue${NC}"
echo ""
echo -e "  # Download instead of stream"
echo -e "  ${GREEN}topster \"movie name\" --download ~/Downloads${NC}"
echo ""
echo -e "${CYAN}📚 Documentation: https://github.com/4cecoder/topster${NC}"
echo ""
if command -v topster &> /dev/null; then
    echo -e "${GREEN}✅ 'topster' command is now available!${NC}"
else
    echo -e "${YELLOW}⚠️  If 'topster' command is not found, restart your terminal or run:${NC}"
    if [[ "$OS" == "macos" ]]; then
        echo -e "   source ~/.zshrc"
    else
        echo -e "   source ~/.bashrc"
    fi
    echo ""
    echo -e "   Or add this to your PATH manually:"
    echo -e "   export PATH=\"\$HOME/.bun/bin:\$PATH\""
fi
echo ""
