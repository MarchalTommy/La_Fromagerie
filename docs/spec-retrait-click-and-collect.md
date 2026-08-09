# Spec — Retrait en boutique (click & collect) et dates de marché

**Statut :** spécification arrêtée, en attente des décisions ouvertes du §11.
**Date :** 09/08/2026 · **Branche :** `claude/click-collect-market-dates-ef4279`
**Prérequis de lecture :** `fromagerie-firestore-data-model`, `fromagerie-delivery-logistics-reference`,
`fromagerie-change-control`.

---

## 1. Objet

Permettre au client de commander sans se faire livrer, en venant chercher sa commande :

- **à la fromagerie**, un jour d'ouverture (click & collect) ;
- **sur un marché**, à une date et une adresse définies par l'admin.

La livraison à domicile existante n'est pas modifiée dans son fonctionnement.

### Dans le périmètre v1

Les trois modes de retrait ; le paiement en ligne **ou** sur place ; la tarification différenciée
par mode ; la gestion des jours de fermeture ; les rappels le jour J (retrait **et** livraison) ;
les adaptations admin indispensables à la sécurité opérationnelle.

### Hors périmètre v1

Plafond de capacité par date (fermeture manuelle uniquement) ; créneaux horaires réservables
(la plage est affichée, pas réservée) ; marchés récurrents (chaque date est saisie séparément) ;
facture pour les paiements encaissés sur place ; toute modification de la chaîne de paiement en
ligne.

---

## 2. Vocabulaire

| Terme | Sens dans ce document |
|---|---|
| **Mode** | `LIVRAISON`, `RETRAIT_BOUTIQUE` ou `RETRAIT_MARCHE`. Porté par la commande. |
| **Point de retrait** | La boutique (récurrente) ou une date de marché (ponctuelle). |
| **Tournée / parcours** | Le `delivery_path` existant. Inchangé, et sans lien avec les points de retrait. |
| **Instantané** | Copie du libellé/adresse/horaire du point, figée sur la commande à sa création. |

---

## 3. Parcours client

### 3.1 Choix du mode

Le mode est un **contexte global**, sélectionnable depuis le catalogue via un sélecteur dans la
barre supérieure, et non une étape bloquante en début de parcours. Il vaut `LIVRAISON` par défaut,
est persisté dans `SharedDatastore` (à côté de `lastSelectedPath`) et reste modifiable jusqu'à
l'écran de paiement.

Deuxième point d'entrée, au moins aussi important : lorsqu'une adresse se révèle **non éligible**
(`NOT_ELIGIBLE`, `ASK_FOR_SUPPORT`, `STREET_NOT_COVERED`), le client tombe aujourd'hui sur un
cul-de-sac avec un mail au support. Le retrait doit y être proposé comme rattrapage explicite.

### 3.2 Écran de retrait

En mode retrait, l'écran de livraison actuel est réduit :

- **pas de champ adresse**, pas de géocodage, pas de calcul d'éligibilité, pas de tournée ;
- champs demandés : **nom, email, téléphone** (le téléphone remplace fonctionnellement l'adresse :
  c'est ce qui sert en cas de retard ou d'absence) ;
- la carte affiche **un point** (boutique ou marché) au lieu du tracé de tournée ;
- la liste de dates provient du point de retrait, pas de `BuildSelectableDeliveryDatesUseCase` ;
- la **plage horaire** du point est affichée en clair sous la date choisie (ex. « samedi 8h–13h »).

### 3.3 Dates proposées

- **Boutique** : jours d'ouverture récurrents, moins les dates de fermeture (§5.2), moins les
  dates déjà closes par la règle de clôture.
- **Marché** : uniquement les dates saisies par l'admin, à venir et non closes.
- **Clôture : J-1 à 12h00 pour tous les modes**, identique à la livraison actuelle. Aucune
  divergence de règle entre modes en v1.

### 3.4 Paiement

Le client choisit entre **payer en ligne** (Google Pay → SumUp, chaîne strictement inchangée) et
**payer sur place** au moment du retrait. Le mode de paiement est indépendant du mode de retrait.

---

## 4. Tarification par mode

### 4.1 Le problème

**Deux grilles tarifaires seulement** : le tarif livraison, qui s'applique aussi au marché, et le
tarif boutique, légèrement inférieur. Le panier est néanmoins rempli **avant** que le mode ne soit
connu, et `CartItem` fige le prix unitaire à l'ajout
([CartItem.kt](../core/domain/src/main/java/com/mtdevelopment/core/model/CartItem.kt),
total recalculé en `sum(price × quantity)` dans
[CartViewModel.kt:150](../cart/presentation/src/main/java/com/mtdevelopment/cart/presentation/viewmodel/CartViewModel.kt:150)).

Propriété structurante : **le tarif boutique est toujours ≤ au tarif livraison**. Le total ne peut
donc jamais augmenter au changement de mode, quel que soit le chemin emprunté. Aucune garde
« avertir d'une hausse » n'est nécessaire, et il ne faut pas en écrire une « au cas où » : elle
serait du code mort masquant l'invariant.

### 4.2 Options écartées

**Demander le mode avant le catalogue.** Logique simple, aucun recalcul — mais un mur au premier
lancement, avant même que le client ait vu un fromage. Écarté : la friction est placée au pire
endroit possible.

**Remise globale uniforme** (pourcentage ou montant fixe appliqué au panier). Saisie admin
minimale, mais la baisse n'est pas uniforme d'un produit à l'autre. Un pourcentage imposerait de
surcroît une règle d'arrondi sur des montants en centimes, terrain sur lequel le projet a déjà
produit des bugs monétaires.

### 4.3 Solution retenue

Un **prix boutique facultatif par produit**, avec le mode comme contexte global visible :

1. Le catalogue affiche les prix du mode actif, mis à jour immédiatement au changement de mode.
2. **La livraison est la référence affichée par défaut** ; le mode marché utilise exactement les
   mêmes prix.
3. En mode boutique, l'écart est mis en avant comme un gain (prix livraison barré ou badge
   « −0,40 € »). Une baisse est un argument de conversion : elle doit se voir.
4. Un produit sans prix boutique renseigné est vendu au prix livraison, sans mention d'écart.
5. À l'écran de retrait comme au récapitulatif, une ligne explicite le tarif appliqué
   (ex. « Tarifs retrait boutique »).

**Règle invariante : le total ne doit jamais augmenter entre ce que le client a vu et ce qu'il
paie.** Ici elle est garantie par construction, pas par une vérification à l'exécution.

### 4.4 Conséquence technique

Le changement de mode déclenche une **re-tarification du panier** : chaque `CartItem` est rapproché
du catalogue **par son nom** et son `price` réécrit. Si un produit du panier n'est plus au
catalogue, sa ligne conserve son prix figé et le fait est signalé plutôt que masqué.

---

## 5. Modèle de données

> L'application est **en production** : toute évolution est **strictement additive**. Les anciens
> APK client continueront d'écrire des commandes sans les nouveaux champs — le défaut à la lecture
> n'est pas une phase transitoire, il est permanent.

### 5.1 `orders` — champs ajoutés

Convention de la collection : **snake_case**.

| Champ | Type | Défaut à la lecture | Rôle |
|---|---|---|---|
| `fulfillment_type` | String | `DELIVERY` | `DELIVERY` / `PICKUP_SHOP` / `PICKUP_MARKET` |
| `payment_mode` | String | `ONLINE` | `ONLINE` / `ON_SITE` |
| `customer_phone` | String? | `null` | Contact retrait |
| `pickup_point_id` | String? | `null` | Référence au point de retrait |
| `pickup_label` | String? | `null` | **Instantané** — ex. « Marché de Pontarlier » |
| `pickup_address` | String? | `null` | **Instantané** |
| `pickup_time_range` | String? | `null` | **Instantané** — ex. « 8h–13h » |

Les trois derniers champs sont **recopiés** sur la commande, pas seulement référencés : `orders`
est le registre historique et ne doit dépendre d'aucune donnée mutable. Sans cela, corriger ou
supprimer une date de marché réécrirait les commandes passées.

`customer_address` reste requis par le schéma : en mode retrait il est renseigné avec l'adresse de
facturation retournée par Google Pay, ou vide pour un paiement sur place.

### 5.2 `pickup_points` — nouvelle collection

| Champ | Type | Notes |
|---|---|---|
| `type` | String | `SHOP` / `MARKET` |
| `label` | String | Nom affiché au client |
| `address` | String | Adresse postale ; géocodée pour le pin carte |
| `latitude` / `longitude` | Double | Résultat du géocodage (réutilise l'API Géoplateforme) |
| `time_range` | String | Plage horaire affichée, non réservable (ex. « 8h–13h ») |
| `opening_days` | List\<String\> | **`SHOP` uniquement** — noms `DayOfWeek` |
| `closed_dates` | List\<String\> | **`SHOP` uniquement** — `dd/MM/yyyy`, vacances et fermetures |
| `date` | String? | **`MARKET` uniquement** — `dd/MM/yyyy`, date unique |

Un unique document `SHOP` est attendu ; N documents `MARKET`, un par date. Aucun marché n'est
récurrent en v1 : les marchés reviennent environ une fois par mois, sans régularité hebdomadaire
exploitable, donc chaque date est saisie individuellement.

**Fermetures** : côté boutique via `closed_dates` ; côté marché en supprimant la date.

### 5.3 `products` — champs ajoutés

Convention de la collection : **camelCase** (`priceCents`, `imgUrl`) — différente de `orders`,
respecter chacune.

| Champ | Type | Défaut | Rôle |
|---|---|---|---|
| `priceCentsPickupShop` | Long? | retombe sur `priceCents` | Tarif retrait boutique |

`priceCents` reste le **tarif livraison et la référence**, et sert tel quel au mode marché : aucun
champ de prix marché n'existe. Le prix boutique est facultatif produit par produit — l'admin ne
renseigne que ce qui diffère réellement. La branche de lecture des anciennes clés courtes
(`b`..`h`) n'a pas d'équivalent et retombe naturellement sur `priceCents`.

**Contrainte de saisie :** un prix boutique supérieur au prix livraison doit être refusé à
l'édition. C'est ce qui maintient l'invariant du §4.1 à la source, plutôt que de le rattraper côté
client.

### 5.4 `database_update` — troisième document

Ajout de `pickup_timestamp`, sur le même modèle que `products_timestamp` et `path_timestamp`.
**Toute écriture sur `pickup_points` doit le mettre à jour**, sans quoi les clients servent
indéfiniment un cache périmé.

### 5.5 Room

- `ProductEntity` : une colonne nullable ajoutée → **migration 6 → 7** additive
  (`ALTER TABLE products ADD COLUMN priceCentsPickupShop INTEGER DEFAULT NULL`).
- Points de retrait : nouvelle table `pickup_points` sur le même schéma que §5.2.

⚠️ Les colonnes Room portent les **noms de propriétés Kotlin**, jamais les valeurs `@SerialName`.
Écrire une migration contre le mauvais nom produit une colonne fantôme.

⚠️ **Une lecture Firestore hors-ligne réussit et renvoie zéro document** : `addOnFailureListener`
ne se déclenche pas. « Zéro point de retrait » doit être traité comme un **échec**, jamais comme
une réponse valide — c'est le piège qui avait rendu toutes les adresses non livrables au premier
lancement sans réseau (corrigé le 29/07 sur les parcours).

---

## 6. Paiement et cycle de vie

### 6.1 États

| Situation | `status` | `payment_mode` |
|---|---|---|
| Commande créée, paiement en ligne en cours | `PENDING` | `ONLINE` |
| Paiement en ligne réussi | `PAID` | `ONLINE` |
| Paiement en ligne échoué | `CANCELED` | `ONLINE` |
| Commande à payer sur place | `PENDING` | `ON_SITE` |
| Encaissée par l'admin au retrait | `PAID` | `ON_SITE` |

Le champ `payment_mode` est ce qui **désambiguïse `PENDING`**, aujourd'hui porteur de deux sens
incompatibles : « paiement en vol » et « à payer sur place ». On n'ajoute **pas** de valeur à
`OrderStatus` pour cela : le lecteur admin fait `runCatching { valueOf(…) }.getOrDefault(PENDING)`,
donc un ancien APK afficherait un nouveau statut comme `PENDING` — silencieusement, et on
retomberait dans la confusion qu'on cherche à lever. Un champ inconnu, lui, est simplement ignoré.

### 6.2 Encaissement sur place

Action admin « Encaissé » sur la commande → passage en `PAID`. **Aucune facture n'est émise** pour
ce chemin : le flux Invoice Ninja reste réservé aux paiements en ligne.

### 6.3 Annulation automatique des commandes non retirées

Une commande **`payment_mode = ON_SITE` et `status = PENDING`** dont la date de retrait est
dépassée de **plus de 3 jours** passe automatiquement en `CANCELED`.

⚠️ **Cette règle ne doit jamais s'appliquer à une commande `ONLINE`.** Une commande en ligne
bloquée en `PENDING` signale un paiement en vol ou une finalisation en échec : elle demande une
investigation, pas une annulation silencieuse qui effacerait la trace d'un client potentiellement
débité.

Implémentation privilégiée : **Cloud Function planifiée** (elle s'exécute sans que personne
n'ouvre l'app). Repli acceptable si l'on veut éviter le coût backend : un balayage côté app admin
au chargement de la liste des commandes — au prix d'une exécution qui dépend de l'ouverture de
l'app.

---

## 7. Rappels et notifications

L'infrastructure existe déjà et n'est pas à construire : `ClientMessagingService`,
`NotificationLocalStore` (centre de notifications in-app), canal `fromagerie_client_general`,
demande de `POST_NOTIFICATIONS` dans `MainActivity`.

Elle ne convient toutefois **pas** au besoin : le ciblage est **par topic** (`clients`, tout le
monde), sans registre de tokens — `onNewToken` n'envoie rien. Un rappel nominatif ne peut pas
passer par ce canal.

**Solution : rappel local planifié.** La date est connue au moment de la commande, sur l'appareil
qui l'a passée. Au checkout, un `OneTimeWorkRequest` unique par commande est planifié pour le matin
du jour J ; à son déclenchement il poste la notification système **et** écrit dans
`NotificationLocalStore` pour que le centre in-app reflète la même chose.

- Vaut pour **tous les modes**, retrait comme livraison.
- `WorkManager` survit au redémarrage, contrairement à un `AlarmManager` qui exigerait la
  permission d'alarme exacte depuis Android 12.
- Le travail est annulé par nom unique si la commande est annulée.
- Précision de l'ordre de l'heure, pas de la minute : suffisant pour un rappel « aujourd'hui ».
- Le refus de `POST_NOTIFICATIONS` reste respecté : la notification n'apparaît qu'in-app.

**Ce lot est autonome** : il ne dépend pas du click & collect et peut être livré avant.

---

## 8. Parcours admin

### 8.1 Gestion des points de retrait

Nouvel écran CRUD, calqué sur `PathEditScreen` (brouillon `rememberSaveable` type `PathDraft`) :
horaires et jours d'ouverture de la boutique, dates de fermeture, création/édition/suppression des
dates de marché avec adresse géocodée. Entrée à ajouter dans la navigation admin.

### 8.2 Commandes

Actions « Encaissé » (→ `PAID`) et « Retiré » (→ `DELIVERED`) sur les commandes en retrait.

### 8.3 Trois impacts existants à traiter — dont un bloquant

**Tournée du jour — correction obligatoire.**
[DeliveryHelperScreen.kt:218](../admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/screen/DeliveryHelperScreen.kt:218)
prend **toutes** les commandes du jour et envoie leurs `customerAddress` à l'optimisation Google
Routes, puis au service de tracking. Sans filtre sur `fulfillment_type == DELIVERY`, une commande à
retirer devient un arrêt de livraison à domicile. Ce n'est pas une amélioration, c'est une
condition de non-régression.

**Préparation.** `OrderPreparationScreen` agrège par date seule : une journée mêlant tournée,
retraits boutique et marché produirait un unique « 4 Comté » sans distinguer ce qui part dans le
camion. Le regroupement devient **(date × point de retrait)**, et l'identifiant composite de
`preparation_status` (`"05072026_ComtéAOP"`) doit intégrer le point sous peine de collision entre
deux lots du même jour.

**Commandes manuelles.** `DeliveryAddDialog` crée aussi des commandes : elles doivent porter
`fulfillment_type = DELIVERY` explicitement.

---

## 9. Découpage en lots

| Lot | Contenu | Livrable indépendamment |
|---|---|---|
| **0** | Rappels jour J (§7), livraison comprise | **Oui** |
| **1** | Champs `orders` + filtre tournée + regroupement préparation | Oui (invisible côté client) |
| **2** | Collection `pickup_points` + écran admin de gestion | Oui |
| **3** | Parcours client de retrait (sélecteur, formulaire, dates, carte) | Non — dépend de 1 et 2 |
| **4** | Tarification par mode (§4) | Non — dépend de 3 |
| **5** | Paiement sur place + encaissement + annulation J+3 | Non — dépend de 3 |

L'ordre 1 → 2 → 3 garantit qu'à aucun moment une commande en retrait ne peut exister sans que
l'admin sache la distinguer d'une livraison.

---

## 10. Critères d'acceptation

1. Une commande écrite par un APK antérieur, sans `fulfillment_type`, est lue et traitée comme une
   livraison — de façon permanente, pas transitoire.
2. Une commande en retrait n'apparaît **jamais** dans l'optimisation de tournée ni dans le service
   de suivi de livraison.
3. Une journée mêlant tournée + retrait boutique + marché produit **trois lots de préparation
   distincts**, sans collision d'identifiants dans `preparation_status`.
4. Le total affiché au client ne peut jamais augmenter entre le panier et le paiement, quel que
   soit l'enchaînement de changements de mode ; en mode boutique, l'économie réalisée est visible
   sur les produits concernés.
5. Une date de fermeture boutique fait disparaître le jour correspondant des dates proposées.
6. Modifier ou supprimer un point de retrait ne modifie aucune commande existante.
7. Une commande `ON_SITE` non retirée passe en `CANCELED` à J+3 ; une commande `ONLINE` en
   `PENDING` n'est **jamais** annulée automatiquement.
8. Sans réseau au premier lancement, l'absence de points de retrait est signalée comme une erreur,
   jamais présentée comme « aucun retrait possible ».
9. Le rappel jour J se déclenche pour une commande en livraison comme pour une commande en retrait,
   et survit à un redémarrage de l'appareil.
10. Les deux flavors compilent, et la suite de tests ne régresse pas au-delà des 2 échecs connus
    d'`AdminViewModelTest`.

---

## 11. Décisions encore ouvertes

1. **Annulation J+3 : Cloud Function planifiée ou balayage côté app admin ?** (§6.3)
2. **Fermer une date sur laquelle des commandes existent déjà** — refuser, ou avertir et laisser
   faire en listant les clients à prévenir ?
3. **Adresse de facturation en retrait avec paiement sur place** — aucune adresse n'est alors
   collectée. Acceptable puisqu'aucune facture n'est émise, à confirmer.

Aucune de ces trois questions ne bloque le démarrage : les lots 0 à 4 peuvent être menés sans y
répondre, seul le lot 5 attend la réponse à la n°1.

---

## 12. Contraintes projet

- **Changement de schéma Firestore** → classe (e) au sens de `fromagerie-change-control` :
  validation explicite de Tommy avant merge.
- **Additif uniquement** : aucun renommage, aucune suppression, aucun changement de type ou de
  sémantique d'un champ existant.
- **Aucune écriture sur la Firestore de production** depuis une session de dev. Les mappings se
  prouvent par tests unitaires sur les DTO.
- **La chaîne de paiement en ligne n'est pas touchée** — choix délibéré : le paiement sur place est
  un chemin parallèle, pas une modification du chemin existant.
- Les deux flavors doivent compiler ; livraison par branche → PR vers `main`.
