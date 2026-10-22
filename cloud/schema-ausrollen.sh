#!/bin/bash
# Am Mac im Terminal, im Ordner YEM1N-android. Einmal vorher: xcrun cktool save-token (Management-Token aus dem CloudKit-Dashboard)
# TEAM = deine Team-ID (developer.apple.com, Mitgliedschaft)
TEAM="${1:?Team-ID angeben: ./cloud/schema-ausrollen.sh ABCDE12345}"
xcrun cktool import-schema --team-id "$TEAM" --container-id iCloud.de.cemaras.yem1n \
  --environment development --file cloud/schema.ckdb
echo "Fertig. Jetzt im CloudKit-Dashboard: Deploy Schema Changes to Production."
