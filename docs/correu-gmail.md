# Enviar els informes des del Gmail de la societat

L'app no guarda cap contrasenya del Gmail. Fa servir un petit script de Google (Apps Script) que viu dins el mateix compte de Gmail de la societat i envia els correus en nom seu. És gratuït (límit de 100 correus al dia en comptes Gmail normals, més que suficient per a menys de 50 socis).

## Passos

1. Entra a <https://script.google.com> amb el Gmail de la societat (en proves, `xavigaya+gastrobacus@gmail.com` rep tots els correus, però l'script s'ha de crear amb un compte Gmail real).
2. **Projecte nou**. Esborra el que hi hagi i enganxa el contingut de `backend/enviar-correus.gs`.
3. Executa la funció `prova` un cop. Google demanarà permís per enviar correus: accepta-ho. Hauries de rebre un correu de prova.
4. **Configuració del projecte** (roda dentada) > **Propietats de l'script** > Afegeix la propietat `TOKEN` amb una clau llarga inventada (per exemple 30 lletres i números a l'atzar).
5. **Implementa** > **Nova implementació** > tipus **Aplicació web**.
   - Executa com: **Jo**
   - Qui hi té accés: **Qualsevol usuari**
6. Copia l'adreça que acaba en `/exec`.
7. A l'app: **Gestió** > **Configuració** > *Adreça de l'script d'enviament* i *Clau secreta de l'script* (el mateix `TOKEN`). Desa.

## Mode de proves

Mentre estigui activat *Mode de proves* a la configuració, tots els correus van a l'adreça de proves (`xavigaya+gastrobacus@gmail.com`) en lloc d'anar als socis. Quan es passi a real, cal desactivar-lo.

## Si no es configura

Els correus queden a **Gestió** > **Correus** com a pendents. Des d'allà es poden obrir un a un al programa de correu del mòbil o l'ordinador.
