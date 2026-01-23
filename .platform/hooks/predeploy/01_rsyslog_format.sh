#!/bin/bash
# Configure rsyslog to output only the message content without timestamp/hostname prefix

# Create custom rsyslog config for web application logs
cat > /etc/rsyslog.d/web-stdout.conf << 'EOF'
# Custom template for web application - message only, ltrim removes leading space
$template WebAppFormat,"%msg:1:$:ltrim%\n"

# Apply to web.stdout.log
if $programname == 'web' then /var/log/web.stdout.log;WebAppFormat
& stop
EOF

# Restart rsyslog to apply changes
systemctl restart rsyslog
