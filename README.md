# HAI913I — TP 1 : analyse statique de projets Java
## Projet et architecture

L’analyseur reçoit le chemin d’un répertoire contenant les sources Java et une ou plusieurs actions à exécuter. Il produit des résultats textuels dans le terminal : arbre AST, structure, appels, métriques et graphe d’appel.

Le traitement suit les étapes ci-dessous :

1. `parse/ProjectSources` repère les fichiers `.java`. Il privilégie `src/main/java`, puis `src`, et utilise le répertoire fourni comme racine si aucun de ces dossiers n’existe.
2. `parse/JdtParser` analyse ensemble les unités de compilation avec JDT, au niveau de langage Java 17, et tente de résoudre les bindings.
3. `extract/ProjectExtractor` construit le modèle de faits (`model/`) : types, attributs, méthodes et appels. Les signatures de méthode, de forme `Type#nom(types des paramètres)`, distinguent les surcharges.
4. `output/` affiche l’AST, la structure et les appels. `metrics/MetricsCalculator` calcule les 13 métriques, et `graph/CallGraph` construit le graphe depuis le modèle, sans refaire l’analyse JDT.

Les principales parties du code se trouvent dans `src/main/java/hai913i/tp1/` :

Chaque paquetage a son propre rôle:
- `parse`:  Découverte des sources et configuration du parseur JDT
- `extract`: Extraction des faits et classification des types, visibilités et appels
- `model`: Modèle de faits indépendant de JDT
- `metrics`: Calcul des métriques B2
- `graph`: Graphe d’appel construit depuis le modèle
- `output`: Formatage des résultats destinés au terminal
- `Main.java`: Point d’entrée et traitement des arguments de la ligne de commande

## Environnement

- Système: Ubuntu 24.04, Linux x86-64
- JDK: OpenJDK 21.0.12.1 ; le projet est compilé pour Java 17 et JDK 17 est le minimum requis
- Maven: Apache Maven 3.8.7
- Eclipse JDT Core: `org.eclipse.jdt:org.eclipse.jdt.core:3.46.0`
- JUnit Jupiter: 5.14.4

## Construction

Depuis la racine du dépôt, avec un JDK 17 ou ultérieur et Maven installés :

```sh
mvn clean test
mvn clean package
```

`mvn clean test` compile le projet et lance les tests (dont le test de fumée du parseur). `mvn clean package` produit le JAR exécutable autonome :

```text
target/hai913i-tp1-analyzer.jar
```

Ces commandes suffisent depuis une copie fraîche ; aucun IDE ni fichier généré préalable n’est nécessaire. Le répertoire `target/` est recréé par Maven.

## Exécution

Toutes les commandes requièrent `--source`, suivi du répertoire du projet à analyser. Les actions peuvent être combinées. Depuis la racine du dépôt :

```sh
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --validate
```

Dans les exemples ci-dessous, `../validation` et `../robustness` désignent les ressources téléchargées séparément et placées à côté du répertoire de ce dépôt.
Remplacez ces chemins par leur emplacement si nécessaire. Le chemin fourni doit être un répertoire ; les chemins contenant des espaces doivent être cités.

### Options

- `--validate`: Affiche les diagnostics de compilation JDT et le nombre d’unités de compilation et d’erreurs. |
- `--ast FICHIER.java`: Affiche l’arbre AST du fichier portant ce nom dans le projet analysé. |
- `--structure`: Affiche les types, leur paquetage, héritage, interfaces, attributs et méthodes. |
- `--calls`: Affiche les appels extraits, leur ligne, le type statique du receveur et la cible lorsqu’elle est connue. |*
- `--metrics --threshold X`: Affiche les 13 métriques B2. `X` est un entier positif ou nul et est obligatoire avec `--metrics`. |
- `--graph`: Affiche les nœuds et arcs pondérés du graphe d’appel. |
- `--method "SIGNATURE"`: Affiche les méthodes appelées et appelantes d’une méthode identifiée par sa signature complète. |

Une action doit être spécifiée. `--threshold` n’est accepté qu’avec `--metrics`. Les options inconnues, les arguments manquants et les seuils invalides sont signalés comme erreurs.

### Exemples

```sh
# Vérifier la configuration et analyser le projet
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --validate

# Afficher l’AST d’un fichier
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --ast Dvd.java

# Structure, appels et receveurs
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --structure
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --calls

# Calculer les métriques avec le seuil strictement dépassé fixé à 5
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --metrics --threshold 5

# Graphe complet, puis appelées et appelantes d’une méthode
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --graph
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" \
  --method "library.model.Loanable#checkOut(library.model.Member)"
```

Pour les métriques, le seuil `X` est appliqué strictement : la métrique 11 liste les types ayant **plus de** `X` méthodes.
Pour les classements « top 10 % », la taille retenue est arrondie à l’entier supérieur.
les ex æquo à la limite sont conservés et les valeurs nulles ne sont pas affichées.
Les métriques couvrent les types nommés, y compris les types imbriqués.
La moyenne de lignes par méthode ne porte que sur les méthodes avec corps.

### Exemples sous Windows

Dans `cmd.exe` ou PowerShell, adaptez les séparateurs et citez les chemins qui contiennent des espaces :

```powershell
java -jar target\hai913i-tp1-analyzer.jar --source "..\validation" --validate
java -jar target\hai913i-tp1-analyzer.jar --source "C:\Mes cours\HAI913I\validation" --metrics --threshold 5
```

## Validation et résultats attendus

Les vérifications ci-dessous utilisent le projet `validation` fourni avec le TP. Les résultats ont été reproduits avec les commandes de ce README.

| Étape | Commande / contrôle | Résultat de référence |
|---|---|---|
| A0 | `--validate` sur `../validation` | Racine `../validation/src`, 14 unités de compilation, 0 erreur |
| A1 | `--ast Dvd.java` | L’arbre AST est affiché, notamment les déclarations de type et de méthode |
| A2 | `--structure` | 15 types : 12 classes, 2 interfaces, 1 énumération ; 59 méthodes dont 9 constructeurs ; 21 attributs |
| A3 | `--calls` | 94 appels : 46 internes, 48 externes, 0 non résolu |
| B2 | `--metrics --threshold 5` | 15 types, 493 lignes physiques, 59 méthodes, 4 paquetages ; la sortie contient les 13 métriques |
| B3 | `--graph` | 59 nœuds, 44 arcs, 46 sites d’appel internes, 48 appels externes, 0 appel non résolu |

Pour reproduire tous ces contrôles après construction :

```sh
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --validate
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --ast Dvd.java
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --structure
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --calls
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --metrics --threshold 5
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" --graph
java -jar target/hai913i-tp1-analyzer.jar --source "../validation" \
  --method "library.model.Loanable#checkOut(library.model.Member)"
```

Les échantillons de robustesse permettent également de vérifier le comportement sur des entrées imparfaites :

```sh
java -jar target/hai913i-tp1-analyzer.jar --source "../robustness/no-java-files" --structure
java -jar target/hai913i-tp1-analyzer.jar --source "../robustness/syntax-error" --validate
java -jar target/hai913i-tp1-analyzer.jar --source "../robustness/missing-dep" --calls
```

Le dossier sans fichier Java produit une structure vide sans erreur. L’exemple `syntax-error` contient 2 unités et signale 2 erreurs de syntaxe. Dans `missing-dep`, les appels dont les bindings ne peuvent être résolus restent visibles et sont comptés comme non résolus.

## Limites connues

- La résolution des bindings utilise le JDK et les sources du projet analysé. La CLI ne propose pas d’option pour ajouter les bibliothèques externes du projet : leurs types et appels peuvent donc rester non résolus.
- Un appel non résolu est conservé dans le modèle et signalé dans la sortie des appels, mais ne crée pas d’arc dans le graphe.
- Le graphe est fondé sur les cibles de méthodes résolues statiquement. Il ne propage pas les appels virtuels vers toutes les redéfinitions possibles (pas de CHA) et ne modélise pas les appels dynamiques par réflexion ou injection de dépendances.
- Les appels de constructeurs ne sont pas ajoutés aux arcs du graphe.
- Les métriques sont des indicateurs structurels ; elles ne suffisent pas à elles seules à établir la qualité ou la maintenabilité d’un logiciel.

## Dépannage

| Symptôme | Vérification / solution |
|---|---|
| `mvn` ou `java` introuvable | Installer Maven et un JDK, puis vérifier `mvn -v` et `java -version`. Maven doit utiliser le JDK attendu. |
| Échec de compilation lié à la version Java | Utiliser un JDK 17 ou ultérieur et vérifier que `JAVA_HOME` et le `PATH` pointent vers ce JDK. La compilation du projet cible Java 17. |
| Dépendances non téléchargées | Vérifier l’accès à Maven Central, puis relancer `mvn clean test` ou `mvn clean package`. |
| « `--source` est obligatoire » ou « Pas un répertoire » | Fournir `--source` et le chemin d’un répertoire existant contenant le projet ou ses sources. Citer le chemin s’il contient des espaces. |
| « Aucune action demandée » | Ajouter au moins une action, par exemple `--validate`, `--structure` ou `--graph`. |
| Erreur avec `--metrics` | Ajouter `--threshold X`, où `X` est un entier positif ou nul ; ne pas utiliser `--threshold` seul. |
| Receveur ou cible affiché « non résolu » | Vérifier que les dépendances du projet analysé sont disponibles sur le classpath ; elles ne peuvent pas être configurées via la CLI actuelle. Une erreur de syntaxe peut aussi empêcher la résolution. |
| Méthode inconnue avec `--method` | Copier la signature complète affichée par l’analyseur, avec le nom qualifié du type et les types de tous les paramètres, puis la placer entre guillemets. |
| Avertissements de ressources dupliquées lors du `package` | Le plugin Shade peut signaler des ressources présentes dans plusieurs dépendances. Si Maven termine par `BUILD SUCCESS`, le JAR exécutable a été produit. |
