#!/usr/bin/env bash
# Topster Uninstallation Script for Linux & macOS

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${RED}🗑️  Topster Uninstallation${NC}"
echo -e "${RED}========================${NC}"
echo ""

# Detect OS
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
    echo -e "${BLUE}📦 Detected: Linux${NC}"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
    echo -e "${BLUE}📦 Detected: macOS${NC}"
else
    echo -e "${RED}❌ Unsupported OS: $OSTYPE${NC}"
    exit 1
fi

# Remove binary
BUN_BIN="$HOME/.bun/bin/topster"
if [[ -f "$BUN_BIN" ]] || [[ -L "$BUN_BIN" ]]; then
    echo -e "${YELLOW}🗑️  Removing binary: $BUN_BIN${NC}"
    rm -f "$BUN_BIN"
    echo -e "${GREEN}✅ Binary removed${NC}"
else
    echo -e "${YELLOW}⚠️  Binary not found: $BUN_BIN${NC}"
fi

# Remove config directory (ask user first)
CONFIG_DIR="$HOME/.topster"
if [[ -d "$CONFIG_DIR" ]]; then
    echo ""
    echo -e "${YELLOW}⚠️  Remove config directory and all history?${NC}"
    echo -e "${RED}   $CONFIG_DIR${NC}"
    read -p "   Type 'yes' to confirm: " -r
    echo ""
    if [[ "$REPLY" == "yes" ]]; then
        rm -rf "$CONFIG_DIR"
        echo -e "${GREEN}✅ Config directory removed${NC}"
    else
        echo -e "${YELLOW}⏭️  Config directory kept${NC}"
    fi
fi

# Summary
echo ""
echo -e "${BLUE}======================================${NC}"
echo -e "${GREEN}✅ Topster Uninstallation Complete!${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""
echo -e "${YELLOW}⚠️  Note: You may need to restart your terminal${NC}"
echo ""
