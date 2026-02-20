#!/bin/bash

# Network Operation Audit System - Remote Server Logging Setup Script
# This script configures a Linux server to log all executed commands to a specific file
# in a format compatible with the Audit System's log collector.

LOG_FILE="/var/log/ssh_commands.log"
RSYSLOG_CONF="/etc/rsyslog.d/60-audit.conf"

echo ">>> Setting up remote logging for Audit System..."

# 1. Configure rsyslog to redirect local6 facility to our log file
echo ">>> Configuring rsyslog..."
if [ -f "$RSYSLOG_CONF" ]; then
    echo "    $RSYSLOG_CONF already exists. Backing up..."
    sudo cp $RSYSLOG_CONF ${RSYSLOG_CONF}.bak
fi

echo "local6.* $LOG_FILE" | sudo tee $RSYSLOG_CONF > /dev/null

# 2. Set permissions for the log file so the log collector user can read it
echo ">>> Setting log file permissions..."
sudo touch $LOG_FILE
sudo chmod 644 $LOG_FILE
# Ideally, restrict this to a specific group, but 644 is needed for simple SSH user reading

# 3. Restart rsyslog to apply changes
echo ">>> Restarting rsyslog..."
sudo systemctl restart rsyslog

# 4. Configure PROMPT_COMMAND in /etc/profile to capture commands
# This ensures it applies to all users. Alternatively, add to specific user's .bashrc
echo ">>> Configuring PROMPT_COMMAND in /etc/profile..."

# The command to extract history and format it
# Format: Timestamp | SSH_IP:ip | USER:user | PWD:pwd | COMMAND:cmd
# We use 'logger' to send it to syslog (local6)
AUDIT_CMD="export PROMPT_COMMAND='RETRN_VAL=\$?;logger -p local6.info \"\$(date \"+%Y-%m-%d %H:%M:%S\") | SSH_IP:\${SSH_CLIENT%% *} | USER:\$(whoami) | PWD:\$(pwd) | COMMAND:\$(history 1 | sed \"s/^[ ]*[0-9]\+[ ]*//\")\"'"

if grep -q "SSH_IP" /etc/profile; then
    echo "    PROMPT_COMMAND already configured in /etc/profile."
else
    echo "" | sudo tee -a /etc/profile
    echo "# Audit System Command Logging" | sudo tee -a /etc/profile
    echo "$AUDIT_CMD" | sudo tee -a /etc/profile
    echo "    Added PROMPT_COMMAND to /etc/profile."
fi

echo ">>> Setup complete!"
echo ">>> Please log out and log back in (or source /etc/profile) to enable logging."
echo ">>> Log file location: $LOG_FILE"
