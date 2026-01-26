#!/usr/bin/env bash

set -e

OS="$(uname -s)"
ARCH="$(uname -m)"

echo "========================================"
echo "    TRON Java Dependencies Installer"
echo "========================================"
echo ""
echo ">>> Environment Detection"
if [[ "$OS" == "Darwin" ]]; then
    echo "  OS: macOS $OS"
elif [[ "$OS" == "Linux" ]]; then
    echo "  OS: $OS"
fi
echo "  Architecture: $ARCH"
echo ""
echo ">>> Tested platforms:"
echo "    - macOS x86_64 (JDK 8)"
echo "    - macOS arm64 (JDK 17)"
echo "    - Linux x86_64 (generic, including Ubuntu) (JDK 8)"
echo "    - Linux arm64/aarch64 (generic, including Ubuntu) (JDK 17)"
echo "    Note: Other platforms may require manual installation if errors occur"
echo ""
echo ">>> This script will install the following components if not already installed:"
echo "  1. Homebrew to download and install JDK (macOS only)"
echo "  2. Git for cloning Github repository"
if [[ "$OS" == "Darwin" ]]; then
    if [[ "$ARCH" == "x86_64" ]]; then
        echo "  3. OpenJDK 8 (required for x86_64 architecture)"
    elif [[ "$ARCH" == "arm64" ]]; then
        echo "  3. OpenJDK 17 (required for arm64 architecture)"
    fi
elif [[ "$OS" == "Linux" ]]; then
    if [[ "$ARCH" == "x86_64" ]]; then
        echo "  3. OpenJDK 8 (required for x86_64 architecture)"
    elif [[ "$ARCH" == "aarch64" ]] || [[ "$ARCH" == "arm64" ]]; then
        echo "  3. OpenJDK 17 (required for arm64/aarch64 architecture)"
    fi
fi
echo ""

# Function to ask for user confirmation
ask_confirmation() {
    while true; do
        read -p "Do you want to continue? (y/N): " yn
        case $yn in
            [Yy]* ) return 0;;
            [Nn]* | "" ) echo "Installation cancelled."; exit 0;;
            * ) echo "Please answer yes (y) or no (n).";;
        esac
    done
}

# Function to check Java version
check_java_version() {
    if command -v java &> /dev/null; then
        local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        echo "    Current Java version: $java_version"
        
        # Check if it's JDK 8 (version starts with 1.8)
        if [[ "$java_version" =~ ^1\.8\. ]]; then
            echo "    JDK 8 is already installed."
            return 0
        # Check if it's JDK 17 (version starts with 17)
        elif [[ "$java_version" =~ ^17\. ]]; then
            echo "    JDK 17 is already installed."
            return 1
        else
            echo "    Different Java version detected: $java_version"
            return 2
        fi
    else
        return 3
    fi
}

# Function to ask for JDK installation confirmation
ask_jdk_confirmation() {
    local current_version="$1"
    local required_version="$2"
    local arch="$3"
    
    echo ""
    echo "JDK Version Mismatch Detected!"
    echo "    Current version: $current_version"
    echo "    Required version for $arch: $required_version"
    echo "    This script will install $required_version alongside your existing installation."
    echo "    Your current Java installation will not be removed."
    echo ""
    
    while true; do
        read -p "Do you want to install $required_version? (y/N): " yn
        case $yn in
            [Yy]* ) return 0;;
            [Nn]* | "" ) echo "JDK installation cancelled. Exiting."; exit 0;;
            * ) echo "Please answer yes (y) or no (n).";;
        esac
    done
}

# First, check and install Git (needed for cloning repository)
echo ">>> Checking Git installation..."
if ! command -v git &> /dev/null; then
    echo "    Git is not installed."
    while true; do
        read -p "Do you want to install Git (required for cloning the java-tron repository)? (y/N): " yn
        case $yn in
            [Yy]* ) 
                echo ">>> Installing Git..."
                INSTALL_GIT=true
                break;;
            [Nn]* | "" ) 
                echo "Git installation cancelled. You'll need Git to clone the java-tron repository."
                echo "You can install Git manually later and then clone the repository."
                INSTALL_GIT=false
                break;;
            * ) echo "Please answer yes (y) or no (n).";;
        esac
    done
else
    echo "Git is already installed: $(git --version)"
    INSTALL_GIT=false
fi

echo ""
echo ">>> Checking existing Java installation..."
set +e  # Temporarily disable exit on error
check_java_version
java_status=$?
set -e  # Re-enable exit on error

# Determine required JDK version based on architecture
if [[ "$OS" == "Darwin" ]]; then
    if [[ "$ARCH" == "x86_64" ]]; then
        required_jdk="JDK 8"
        required_status=0
    elif [[ "$ARCH" == "arm64" ]]; then
        required_jdk="JDK 17"
        required_status=1
    else
        echo "Error: Unsupported architecture for macOS: $ARCH"
        exit 1
    fi
elif [[ "$OS" == "Linux" ]]; then
    if [[ "$ARCH" == "x86_64" ]]; then
        required_jdk="JDK 8"
        required_status=0
    elif [[ "$ARCH" == "aarch64" ]] || [[ "$ARCH" == "arm64" ]]; then
        required_jdk="JDK 17"
        required_status=1
    else
        echo "Error: Unsupported architecture for Linux: $ARCH"
        exit 1
    fi
else
    echo "Error: Unsupported Operating System: $OS"
    exit 1
fi

# Check if correct JDK version is already installed
if [[ $java_status -eq $required_status ]]; then
    echo "    You can skip the Java installation part."
    echo ""
    if [[ "$INSTALL_GIT" == "false" ]]; then
        echo "Both Git and Java JDK are ready for TRON development!"
        echo ""
        exit 0
    else
        echo ">>> Proceeding with Git installation only..."
        SKIP_JAVA_INSTALL=true
    fi
elif [[ $java_status -eq 0 ]] || [[ $java_status -eq 1 ]] || [[ $java_status -eq 2 ]]; then
    # Different JDK version is installed, ask for confirmation
    current_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    ask_jdk_confirmation "$current_version" "$required_jdk" "$ARCH"
    SKIP_JAVA_INSTALL=false
else
    # No Java installation found, ask for general confirmation
    echo ""
    echo "No Java installation detected!"
    echo "    This script will install $required_jdk which is required for $ARCH architecture."
    echo ""
    ask_confirmation
    SKIP_JAVA_INSTALL=false
fi

# Function to show permanent Java configuration instructions
show_permanent_java_config() {
    local jdk_version="$1"
    local os_type="$2"
    local java_home="$3"
    local java_bin_path="$4"
    
    echo ""
    echo "    To make JDK $jdk_version permanent:"
    if [[ "$os_type" == "Darwin" ]]; then
        echo "        # Add to ~/.zshrc or ~/.bash_profile:"
        echo "        echo 'export JAVA_HOME=\"$java_home\"' >> ~/.zshrc"
        echo "        echo 'export PATH=\"\$JAVA_HOME/bin:\$PATH\"' >> ~/.zshrc"
        echo "        # Then run below command:"
        echo "        source ~/.zshrc"
        echo ""
        echo "        # Or use jenv for Java version management:"
        echo "        brew install jenv"
        echo "        jenv add $java_home"
    elif [[ "$os_type" == "Linux" ]]; then
        echo "        # Method 1: Add to ~/.bashrc:"
        echo "        echo 'export JAVA_HOME=\"$java_home\"' >> ~/.bashrc"
        echo "        echo 'export PATH=\"\$JAVA_HOME/bin:\$PATH\"' >> ~/.bashrc"
        echo "        source ~/.bashrc"
        echo ""
        echo "        # Method 2: Use update-alternatives (recommended):"
        if [[ "$PKG_MANAGER" == "apt-get" ]]; then
            echo "        sudo update-alternatives --install /usr/bin/java java $java_bin_path/java 1"
            echo "        sudo update-alternatives --install /usr/bin/javac javac $java_bin_path/javac 1"
            echo "        sudo update-alternatives --config java"
        else
            echo "        sudo alternatives --install /usr/bin/java java $java_bin_path/java 1"
            echo "        sudo alternatives --install /usr/bin/javac javac $java_bin_path/javac 1"
            echo "        sudo alternatives --config java"
        fi
    fi
    echo ""
}

# Function to show Java environment application instructions
show_java_env_instructions() {
    local java_home="$1"
    local java_bin_path="$2"
    
    echo ""
    echo "    To apply Java environment to your current shell session:"
    echo "        source ./tron_java_env.sh"
    echo ""
    echo "    Or run this command directly:"
    echo "        export JAVA_HOME=\"$java_home\""
    echo "        export PATH=\"$java_bin_path:\$PATH\""
}

# Function to get Java paths based on OS and architecture
get_java_paths() {
    local jdk_version="$1"
    local os_type="$2"
    local arch="$3"
    local java_home=""
    
    if [[ "$os_type" == "Darwin" ]]; then
        # macOS paths
        if [[ "$jdk_version" == "8" ]]; then
            java_home="/usr/local/opt/openjdk@8"
        elif [[ "$jdk_version" == "17" ]]; then
            if [[ "$arch" == "arm64" ]]; then
                java_home="/opt/homebrew/opt/openjdk@17"
            else
                java_home="/usr/local/opt/openjdk@17"
            fi
        fi
    elif [[ "$os_type" == "Linux" ]]; then
        # Linux paths - provide generic path for manual configuration
        java_home="/usr/lib/jvm/java-$jdk_version-openjdk"
    fi
    
    echo "$java_home"
}

# Unified Java environment configuration function
configure_java_environment() {
    local jdk_version="$1"
    local os_type="$2"
    local arch="$3"
    local java_home=""
    local java_bin_path=""
    
    echo ""
    echo ">>> Configuring Java environment for JDK $jdk_version..."
    
    # Ask user for confirmation before changing environment
    echo ""
    echo "This will modify your Java environment settings:"
    echo "    - Set JAVA_HOME to the new JDK $jdk_version installation"
    echo "    - Update PATH to include the new Java binaries"
    echo "    - Create a script (tron_java_env.sh) for easy environment setup"
    echo ""
    
    while true; do
        read -p "Do you want to configure the Java environment for JDK $jdk_version? (y/N): " yn
        case $yn in
            [Yy]* ) 
                echo ">>> Proceeding with Java environment configuration..."
                break;;
            [Nn]* | "" ) 
                echo "Java environment configuration skipped."
                echo "You may need to manually set JAVA_HOME and PATH for JDK $jdk_version"
                echo ""
                echo "Manual configuration commands:"
                
                # Get the expected Java path
                local expected_java_home=$(get_java_paths "$jdk_version" "$os_type" "$arch")
                local expected_java_bin_path="$expected_java_home/bin"
                echo "    export JAVA_HOME=\"$expected_java_home\""
                echo "    export PATH=\"\$JAVA_HOME/bin:\$PATH\""
                
                if [[ "$os_type" == "Linux" ]]; then
                    echo ""
                    echo "Note: Actual path may vary depending on your distribution."
                    echo "Common paths include:"
                    echo "    /usr/lib/jvm/java-$jdk_version-openjdk-amd64 (Ubuntu/Debian)"
                    echo "    /usr/lib/jvm/java-1.$jdk_version.0-openjdk (RHEL/CentOS)"
                fi
                
                # Show the same application instructions as automatic configuration
                show_java_env_instructions "$expected_java_home" "$expected_java_bin_path"
                
                # Show permanent configuration instructions
                show_permanent_java_config "$jdk_version" "$os_type" "$expected_java_home" "$expected_java_bin_path"
                
                echo ""
                return 1;;
            * ) echo "Please answer yes (y) or no (n).";;
        esac
    done
    
    # Determine Java paths based on OS and architecture
    if [[ "$os_type" == "Darwin" ]]; then
        # Use the helper function for macOS
        java_home=$(get_java_paths "$jdk_version" "$os_type" "$arch")
        java_bin_path="$java_home/bin"
    elif [[ "$os_type" == "Linux" ]]; then
        # Linux paths - try to find the actual installation
        if [[ "$jdk_version" == "8" ]]; then
            if [[ "$PKG_MANAGER" == "apt-get" ]]; then
                if [[ "$arch" == "aarch64" ]] || [[ "$arch" == "arm64" ]]; then
                    java_home="/usr/lib/jvm/java-8-openjdk-arm64"
                else
                    java_home="/usr/lib/jvm/java-8-openjdk-amd64"
                fi
            else
                # RHEL/CentOS/Amazon Linux - try multiple possible paths
                for path in "/usr/lib/jvm/java-1.8.0-amazon-corretto" "/usr/lib/jvm/java-1.8.0-openjdk"; do
                    if [[ -d "$path" ]]; then
                        java_home="$path"
                        break
                    fi
                done
            fi
        elif [[ "$jdk_version" == "17" ]]; then
            if [[ "$PKG_MANAGER" == "apt-get" ]]; then
                if [[ "$arch" == "aarch64" ]] || [[ "$arch" == "arm64" ]]; then
                    java_home="/usr/lib/jvm/java-17-openjdk-arm64"
                else
                    java_home="/usr/lib/jvm/java-17-openjdk-amd64"
                fi
            else
                # RHEL/CentOS/Amazon Linux - try multiple possible paths
                for path in "/usr/lib/jvm/java-17-amazon-corretto" "/usr/lib/jvm/java-17-openjdk"; do
                    if [[ -d "$path" ]]; then
                        java_home="$path"
                        break
                    fi
                done
            fi
        fi
        java_bin_path="$java_home/bin"
    fi
    
    # Set environment variables for current session
    if [[ -d "$java_home" ]]; then
        export JAVA_HOME="$java_home"
        export PATH="$java_bin_path:$PATH"
        echo "    JAVA_HOME set to: $JAVA_HOME"
        echo "    PATH updated to include: $java_bin_path"
        echo "    Environment temporarily configured for JDK $jdk_version"
        
        # Create a source script for the user's current shell
        local env_script="./tron_java_env.sh"
        cat > "$env_script" << EOF
        
#!/bin/bash
# TRON Java Environment Configuration
# Generated by install_dependencies.sh on $(date)

export JAVA_HOME="$java_home"
export PATH="$java_bin_path:\$PATH"

echo "Java environment configured:"
echo "   JAVA_HOME: \$JAVA_HOME"
echo "   Java version: \$(java -version 2>&1 | head -n 1)"
EOF
        chmod +x "$env_script"
        
        echo ""
        show_java_env_instructions "$java_home" "$java_bin_path"
        
    else
        echo "    Could not find Java installation at expected path: $java_home"
        echo "    You may need to set JAVA_HOME manually"
        return 1
    fi
    
    # Provide OS-specific permanent configuration instructions
    show_permanent_java_config "$jdk_version" "$os_type" "$java_home" "$java_bin_path"
    
    return 0
}

echo "----------------------------------------"

install_macos() {
    if ! command -v brew &> /dev/null; then
        echo ">>> Homebrew not found."
        echo "    Homebrew is required to install Java on macOS."
        echo ""
        while true; do
            read -p "Do you want to install Homebrew? (y/N): " yn
            case $yn in
                [Yy]* ) 
                    echo ">>> Installing Homebrew..."
                    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
                    break;;
                [Nn]* | "" ) 
                    echo "Homebrew installation cancelled."
                    echo "Cannot proceed with Java installation without Homebrew on macOS."
                    echo "Please install Homebrew manually or use alternative Java installation methods."
                    exit 1;;
                * ) echo "Please answer yes (y) or no (n).";;
            esac
        done
        
        # Add Homebrew to PATH for the current session (Apple Silicon vs Intel)
        if [[ "$ARCH" == "arm64" ]]; then
            eval "$(/opt/homebrew/bin/brew shellenv)"
        else
            eval "$(/usr/local/bin/brew shellenv)"
        fi
    else
        echo ">>> Homebrew is already installed."
    fi

    echo ">>> Updating Homebrew..."
    brew update

    # Install Git if needed
    if [[ "$INSTALL_GIT" == "true" ]]; then
        echo ">>> Installing Git..."
        brew install git
        echo "    Git installed successfully: $(git --version)"
    fi

    # Skip Java installation if flag is set
    if [[ "$SKIP_JAVA_INSTALL" == "true" ]]; then
        echo ">>> Skipping Java installation (correct version already detected)."
        return 0
    fi

    if [[ "$ARCH" == "x86_64" ]]; then
        echo ">>> Architecture is x86_64. Checking for JDK 8..."
        set +e  # Temporarily disable exit on error
        check_java_version
        local java_status=$?
        set -e  # Re-enable exit on error
        
        if [[ $java_status -eq 0 ]]; then
            echo ">>> JDK 8 is already installed. Skipping installation."
        else
            if [[ $java_status -eq 1 ]]; then
                echo ">>> Installing JDK 8 alongside existing JDK 17..."
            elif [[ $java_status -eq 2 ]]; then
                echo ">>> Installing JDK 8 alongside existing Java installation..."
            else
                echo ">>> Installing JDK 8..."
            fi
            brew install openjdk@8
            
            # Use unified Java environment configuration
            if configure_java_environment "8" "Darwin" "$ARCH"; then
                echo "Environment has been updated! Java 8 is now configured."
            else
                echo "Java 8 installed but environment not configured. You may need to set JAVA_HOME manually."
            fi
        fi
        
    elif [[ "$ARCH" == "arm64" ]]; then
        echo ">>> Architecture is arm64. Checking for JDK 17..."
        set +e  # Temporarily disable exit on error
        check_java_version
        local java_status=$?
        set -e  # Re-enable exit on error
        
        if [[ $java_status -eq 1 ]]; then
            echo ">>> JDK 17 is already installed. Skipping installation."
        else
            if [[ $java_status -eq 0 ]]; then
                echo ">>> Installing JDK 17 alongside existing JDK 8..."
            elif [[ $java_status -eq 2 ]]; then
                echo ">>> Installing JDK 17 alongside existing Java installation..."
            else
                echo ">>> Installing JDK 17..."
            fi
            brew install openjdk@17

            # Use unified Java environment configuration
            if configure_java_environment "17" "Darwin" "$ARCH"; then
                echo "Environment has been updated! Java 17 is now configured."
            else
                echo "Java 17 installed but environment not configured. You may need to set JAVA_HOME manually."
            fi
        fi

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

    # Install Git if needed
    if [[ "$INSTALL_GIT" == "true" ]]; then
        echo ">>> Installing Git..."
        $INSTALL_CMD git
        echo "    Git installed successfully: $(git --version)"
    fi

    # Skip Java installation if flag is set
    if [[ "$SKIP_JAVA_INSTALL" == "true" ]]; then
        echo ">>> Skipping Java installation (correct version already detected)."
        return 0
    fi

    install_first_available() {
        for pkg in "$@"; do
            if $INSTALL_CMD "$pkg"; then
                return 0
            fi
        done
        return 1
    }

    if [[ "$ARCH" == "x86_64" ]]; then
        echo ">>> Architecture is x86_64. Checking for JDK 8..."
        set +e  # Temporarily disable exit on error
        check_java_version
        local java_status=$?
        set -e  # Re-enable exit on error
        
        if [[ $java_status -eq 0 ]]; then
            echo ">>> JDK 8 is already installed. Skipping installation."
        else
            if [[ $java_status -eq 1 ]]; then
                echo ">>> Installing JDK 8 alongside existing JDK 17..."
            elif [[ $java_status -eq 2 ]]; then
                echo ">>> Installing JDK 8 alongside existing Java installation..."
            else
                echo ">>> Installing JDK 8..."
            fi
            if [[ "$PKG_MANAGER" == "apt-get" ]]; then
                install_first_available openjdk-8-jdk
            else
                install_first_available java-1.8.0-amazon-corretto-devel java-1.8.0-openjdk-devel
            fi || { echo "Error: Unable to install JDK 8 on $PKG_MANAGER"; exit 1; }
            
            # Use unified Java environment configuration
            if configure_java_environment "8" "Linux" "$ARCH"; then
                echo "Environment has been updated! Java 8 is now configured."
            else
                echo "Java 8 installed but environment not configured. You may need to set JAVA_HOME manually."
            fi
        fi
        
    elif [[ "$ARCH" == "aarch64" ]] || [[ "$ARCH" == "arm64" ]]; then
        echo ">>> Architecture is arm64/aarch64. Checking for JDK 17..."
        set +e  # Temporarily disable exit on error
        check_java_version
        local java_status=$?
        set -e  # Re-enable exit on error
        
        if [[ $java_status -eq 1 ]]; then
            echo ">>> JDK 17 is already installed. Skipping installation."
        else
            if [[ $java_status -eq 0 ]]; then
                echo ">>> Installing JDK 17 alongside existing JDK 8..."
            elif [[ $java_status -eq 2 ]]; then
                echo ">>> Installing JDK 17 alongside existing Java installation..."
            else
                echo ">>> Installing JDK 17..."
            fi
            if [[ "$PKG_MANAGER" == "apt-get" ]]; then
                install_first_available openjdk-17-jdk
            else
                install_first_available java-17-amazon-corretto-devel java-17-openjdk-devel
            fi || { echo "Error: Unable to install JDK 17 on $PKG_MANAGER"; exit 1; }
            
            # Use unified Java environment configuration
            if configure_java_environment "17" "Linux" "$ARCH"; then
                echo "Environment has been updated! Java 17 is now configured."
            else
                echo "Java 17 installed but environment not configured. You may need to set JAVA_HOME manually."
            fi
        fi
        
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
echo "Installation completed successfully!"
echo ""
echo ">>> Verification Commands:"
echo "  git --version"
echo "  java -version"
echo ""
echo ">>> Troubleshooting:"
echo "  - If 'java -version' shows incorrect version, check Configuring Java environment instructions shown above."
echo ""
echo "Your development environment is ready for TRON!"
echo ""