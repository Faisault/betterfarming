# BetterFarming 5.1.0 (EpicFarming + fermes minières + carburant + progression)

Base : EpicFarming 4.4.1. Tout est en français (`System.Language Mode: fr_FR`, par défaut).

## Fonctions
- **Hologramme multi-lignes** sur chaque ferme : type (Cultures / Élevage / Mixte / Minage), niveau, propriétaire,
  production totale, carburant restant en heures, stockage lié. Lignes configurables : `Hologram.Lines`.
- **Carburant** : une ferme sans carburant s'arrête. Clic droit avec charbon, charbon de bois, bloc de charbon, branche de blaze
  (config `Fuel.Items`). Le carburant est conservé dans l'item quand on récupère la ferme.
- **Fermes minières** : `mining-levels.yml` (5 niveaux, minerais pondérés, intervalle, quantité, pages de stockage).
- **Liaison à un conteneur** : bouton « Lier un conteneur » dans le menu de la ferme (Maj+clic pour délier), puis clic droit sur
  une trémie / coffre / baril / shulker (même monde, `Link.Max Distance`). Les conteneurs collés à la ferme sont aussi alimentés
  (`Link.Adjacent Containers`). Compatible avec les trémies améliorées / chunk hoppers qui sont de vraies trémies.
- **Progression** : récoltes critiques (x2, `Progression.Critical Harvest`), paliers de production avec récompenses
  (`Progression.Milestones`), statistiques dans le menu, classement `/efa top`.
- Commandes : `/efa givefarmitem <joueur> <niveau> [mining]`, `/efa boost <joueur> <x> [durée]`, `/efa top`, `/efa reload`.

## Aucune dépendance à d'autres plugins
Le plugin ne dépend d'aucun autre plugin (plus de `softdepend`) : Vault / économie, WorldGuard / Towny / protections,
UltimateStacker / StackMob / WildStacker, FabledSkyBlock et EpicHoppers ne sont plus utilisés ni détectés.
- Les améliorations se paient uniquement en niveaux d'XP (le bouton « argent » et `Main.Upgrade With Economy` ont été retirés).
- Plus de vérification des plugins de protection (`Main.Use Protection Plugins` retiré) : les droits se gèrent par permissions.
- Les animaux sont comptés un par un (plus de support des entités empilées).
- Les hologrammes sont gérés en interne (ArmorStands), la tâche des trémies démarre toujours.

## Build
`mvn clean package` (nécessite l'accès aux dépôts repo.craftaro.com / repo.songoda.com).
Remplace BetterFarming et EpicFarming sur le serveur (retire les anciens jars).
