# Backend 3am Kamel — Spring Boot + MongoDB Atlas

## API
| Méthode | Route | Rôle |
|---|---|---|
| GET  | `/api/health` | test de vie |
| GET  | `/api/products` | catalogue (même format que `PRODUCTS` du front) |
| POST | `/api/orders` | enregistre un devis/commande du panier |
| POST | `/api/contact` | enregistre un message du formulaire contact |
| GET  | `/api/admin/orders`, `/api/admin/messages` | lecture, header `X-Admin-Key` requis |

## 1. MongoDB Atlas
1. Créez un cluster gratuit (M0), puis un utilisateur (Database Access).
2. Network Access : autorisez `0.0.0.0/0` (les hébergeurs gratuits ont des IP variables).
3. Connect > Drivers : copiez l'URI et ajoutez le nom de base :
   `mongodb+srv://USER:MOT_DE_PASSE@cluster0.xxxx.mongodb.net/kamel?retryWrites=true&w=majority`

## 2. Lancer en local
```bash
export MONGODB_URI="mongodb+srv://..."
export ADMIN_KEY="un-secret-long"
mvn spring-boot:run
# test : curl http://localhost:8080/api/products
```
Au premier démarrage, les 16 produits sont importés automatiquement.

## 3. Déployer (obligatoire : Netlify n'héberge que le front)
Render / Railway / Fly.io avec le `Dockerfile` fourni. Variables d'environnement :
`MONGODB_URI`, `ADMIN_KEY`, et `CORS_ORIGINS=https://3am-kamel.netlify.app`.
L'URL obtenue (en https) va dans `API_URL` du front.

## 4. Voir les commandes
```bash
curl -H "X-Admin-Key: un-secret-long" https://VOTRE-BACKEND/api/admin/orders
```
