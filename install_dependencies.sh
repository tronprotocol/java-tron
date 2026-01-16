#!/usr/bin/env bash

set -e

OS="$(uname -s)"
ARCH="$(uname -m)"

echo ">>> Environment Detection"
if [[ "$OS" == "Darwin" ]]; then
    echo "  OS: MacOS $OS"
elif [[ "$OS" == "Linux" ]]; then
    echo "  OS: $OS"
fi
echo "  Architecture: $ARCH"
echo "----------------------------------------"
echo ">>> Note: This script has been tested on:"
echo "    - macOS x86_64 (JDK 8)"
echo "    - macOS arm64 (JDK 17)"
echo "    - Linux x86_64 (generic, including Ubuntu) (JDK 8)"
echo "    - Linux arm64/aarch64 (generic, including Ubuntu) (JDK 17)"
echo "----------------------------------------"

install_macos() {
    if ! command -v brew &> /dev/null; then
        echo ">>> Homebrew not found. Installing Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
        
        # Add Homebrew to PATH for the current session (Apple Silicon vs Intel)
        if [[ "$ARCH" == "arm64" ]]; then
            eval "$(/opt/homebrew/bin/brew shellenv)"
        else
            eval "$(/usr/local/bin/brew shellenv)"
        fi
    fi

    echo ">>> Updating Homebrew..."
    brew update

    echo ">>> Installing Git..."
    brew install git

    if [[ "$ARCH" == "x86_64" ]]; then
        echo ">>> Architecture is x86_64. Installing JDK 8..."
        brew install openjdk@8
        
        echo ">>> Configuring PATH for openjdk@8 in this script session..."
        export PATH="/usr/local/opt/openjdk@8/bin:$PATH"
        echo "    PATH updated to include /usr/local/opt/openjdk@8/bin"
        
    elif [[ "$ARCH" == "arm64" ]]; then
        echo ">>> Architecture is arm64. Installing JDK 17..."
        brew install openjdk@17

        echo ">>> Configuring PATH for openjdk@17 in this script session..."
        export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
        echo "    PATH updated to include /opt/homebrew/opt/openjdk@17/bin"

    else
        echo "Error: Unsupported architecture for macOS script: $ARCH"
        exit 1
    fi
}

install_linux() {
    if command -v dnf &> /dev/null; then
        PKG_MANAGER="dnf"
        INSTALL_CMD="sudo dnf install -y"
        UPDATE_CMD="sudo dnf check-update"
    elif command -v yum &> /dev/null; then
        PKG_MANAGER="yum"
        INSTALL_CMD="sudo yum install -y"
        UPDATE_CMD="sudo yum check-update"
    elif command -v apt-get &> /dev/null; then
        PKG_MANAGER="apt-get"
        INSTALL_CMD="sudo apt-get install -y"
        UPDATE_CMD="sudo apt-get update"
    else
        echo "Error: Unsupported package manager. Only apt-get (Debian/Ubuntu) and yum/dnf (RHEL/CentOS/Amazon Linux) are currently supported."
        exit 1
    fi

    echo ">>> Updating package index ($PKG_MANAGER)..."
    $UPDATE_CMD || true

    echo ">>> Installing Git..."
    $INSTALL_CMD git

    install_first_available() {
        for pkg in "$@"; do
            if $INSTALL_CMD "$pkg"; then
                return 0
            fi
        done
        return 1
    }

    if [[ "$ARCH" == "x86_64" ]]; then
        echo ">>> Architecture is x86_64. Installing JDK 8..."
        if [[ "$PKG_MANAGER" == "apt-get" ]]; then
            install_first_available openjdk-8-jdk
        else
            install_first_available java-1.8.0-amazon-corretto-devel java-1.8.0-openjdk-devel
        fi || { echo "Error: Unable to install JDK 8 on $PKG_MANAGER"; exit 1; }
    elif [[ "$ARCH" == "aarch64" ]] || [[ "$ARCH" == "arm64" ]]; then
        echo ">>> Architecture is arm64/aarch64. Installing JDK 17..."
        if [[ "$PKG_MANAGER" == "apt-get" ]]; then
            install_first_available openjdk-17-jdk
        else
            install_first_available java-17-amazon-corretto-devel java-17-openjdk-devel
        fi || { echo "Error: Unable to install JDK 17 on $PKG_MANAGER"; exit 1; }
    else
        echo "Error: Unsupported architecture for Linux script: $ARCH"
        exit 1
    fi
}

if [[ "$OS" == "Darwin" ]]; then
    install_macos
elif [[ "$OS" == "Linux" ]]; then
    install_linux
else
    echo "Error: Unsupported Operating System: $OS"
    exit 1
fi

echo "----------------------------------------"
echo ">>> Installation logic completed."
echo "Please verify installations manually if any errors occurred above."
echo "Check versions with:"
echo "  git --version"
echo "  java -version"
