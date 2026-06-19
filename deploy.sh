#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOMCAT_WEBAPPS="/var/lib/tomcat10/webapps"
WAR_NAME="test-project.war"
ARTIFACT_WAR="test-project/target/test-project-1.0.0.war"

if [[ "$(id -u)" -eq 0 ]]; then
    echo "Erreur: ne lancez pas ce script avec sudo." >&2
    echo "       Seule la copie vers Tomcat utilise sudo." >&2
    echo "       Exécutez: ./deploy.sh" >&2
    exit 1
fi

check_writable_target() {
    local dir="$1"
    if [[ -d "$dir" ]] && [[ ! -w "$dir" ]]; then
        echo "Erreur: $dir n'est pas accessible en écriture (probablement créé par root)." >&2
        echo "       Corrigez avec:" >&2
        echo "         sudo chown -R $USER:$USER framework/target test-project/target" >&2
        echo "       ou:" >&2
        echo "         sudo rm -rf framework/target test-project/target" >&2
        exit 1
    fi
}

check_writable_target "$SCRIPT_DIR/framework/target"
check_writable_target "$SCRIPT_DIR/test-project/target"

echo "==> Build du projet..."
cd "$SCRIPT_DIR"
mvn -q clean package

if [[ ! -f "$ARTIFACT_WAR" ]]; then
    echo "Erreur: WAR introuvable: $ARTIFACT_WAR" >&2
    exit 1
fi

echo "==> Déploiement dans $TOMCAT_WEBAPPS..."

if [[ ! -d "$TOMCAT_WEBAPPS" ]]; then
    echo "Erreur: répertoire Tomcat introuvable: $TOMCAT_WEBAPPS" >&2
    exit 1
fi

CONTEXT_NAME="${WAR_NAME%.war}"
sudo rm -rf "$TOMCAT_WEBAPPS/$CONTEXT_NAME" "$TOMCAT_WEBAPPS/$WAR_NAME"
sudo cp "$ARTIFACT_WAR" "$TOMCAT_WEBAPPS/$WAR_NAME"

echo "==> Déploiement terminé."
echo "    Application: http://localhost:8080/$CONTEXT_NAME/annotations"
