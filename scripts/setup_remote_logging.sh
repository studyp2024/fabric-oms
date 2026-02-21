#!/bin/bash

# Network Operation Audit System - Remote Server Logging Setup Script
# This script configures a Linux server to log all SSH executed commands to a specific file.

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (e.g., sudo ./setup_remote_logging.sh)"
  exit 1
fi

LOG_FILE="/var/log/ssh_commands.log"
BASHRC="/etc/bash.bashrc"

echo ">>> Setting up remote logging for Audit System..."

# 1. Add configuration to /etc/bash.bashrc
echo ">>> Configuring $BASHRC..."

# Define the block to append
read -r -d '' CONFIG_BLOCK << EOM

if [ -n "\$SSH_CLIENT" ]; then
    SSH_CLIENT_IP=\$(echo "\$SSH_CLIENT" | awk '{print \$1}')
    SSH_COMMAND_LOG="$LOG_FILE"
    PROMPT_COMMAND='{
        echo "\$(date +"%Y-%m-%d %H:%M:%S") | SSH_IP:\${SSH_CLIENT_IP} | USER:\${USER} | PWD:\${PWD} | COMMAND:\$(history 1 | awk '\''{sub(/^[ ]*[0-9]+[ ]*/, ""); print \$0}'\'')" >> \${SSH_COMMAND_LOG};
    }'
fi
EOM

# Check if already configured
if grep -q "SSH_COMMAND_LOG" "$BASHRC"; then
    echo "    Configuration already exists in $BASHRC. Skipping append."
else
    echo "$CONFIG_BLOCK" >> "$BASHRC"
    echo "    Configuration appended to $BASHRC."
fi

# 2. Create log file and set permissions
echo ">>> Setting up log file: $LOG_FILE..."
touch "$LOG_FILE"
chmod 660 "$LOG_FILE"
chown root:users "$LOG_FILE"
echo "    Permissions set: chmod 660, chown root:users"

echo ">>> Setup complete!"
echo ">>> To apply changes immediately for the current session, run: source $BASHRC"
echo ">>> Otherwise, changes will take effect for new SSH sessions."
