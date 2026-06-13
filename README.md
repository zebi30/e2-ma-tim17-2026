# e2-ma-tim17-2026

Android aplikacija „Slagalica Quiz" za predmet Mobilne aplikacije (2025/26).
Sadrži registraciju i prijavu sa verifikacijom email-a, profil, sistem
notifikacija i kviz igre (Korak po korak, Moj broj, Skočko, Ko zna-zna,
Spojnice, Asocijacije).

## Brzi start

1. Otvorite projekat u Android Studio (`File > Open`) i izaberite root folder `e2-ma-tim17-2026`.
2. (Opciono, za slanje email-a) dodajte kredencijale u `local.properties` — vidi
   sekciju [Konfiguracija slanja email-a](#konfiguracija-slanja-email-a).
3. Sačekajte da se uradi Gradle sync.
4. Kliknite `Run 'app'` i izaberite emulator ili fizički uređaj.

## Preduslovi

- Android Studio (preporuka: poslednja stabilna verzija)
- Android SDK instaliran kroz Android Studio
- JDK 11 (projekat koristi Java 11 za izvorni kod)
- Internet konekcija (za Gradle zavisnosti i za slanje verifikacionog email-a)

Parametri aplikacije:

- `minSdk = 30`
- `targetSdk = 35`
- `compileSdk = 35`

> **Napomena o Gradle JDK-u:** za build je potreban pun JDK sa `jlink`-om
> (npr. ugrađeni **JetBrains Runtime**). Ako pri buildu dobijete grešku tipa
> `JdkImageTransform ... jlink executable ... does not exist`, u
> `Settings > Build, Execution, Deployment > Build Tools > Gradle` postavite
> **Gradle JVM** na JetBrains JDK (ili bilo koji pun JDK 17/21).

## Konfiguracija slanja email-a

Verifikacioni kod se šalje pravim email-om preko SMTP-a (JavaMail / Gmail).
Kredencijali se ne čuvaju u kodu nego u `local.properties` (taj fajl je u
`.gitignore` i ne ide u git):

```properties
SENDER_EMAIL=tvoj.nalog@gmail.com
SENDER_APP_PASSWORD=xxxx xxxx xxxx xxxx
```

`SENDER_APP_PASSWORD` je **App Password** (ne obična lozinka naloga):

1. Na Google nalogu uključite **2-koračnu verifikaciju**.
2. Otvorite **App passwords**, generišite lozinku za „Mail".
3. Dobijenih 16 karaktera upišite kao `SENDER_APP_PASSWORD`.

Vrednosti se ubrizgavaju u `BuildConfig` pri buildu, pa je posle izmene potreban
Gradle sync / rebuild. Ako se kredencijali ne postave, slanje email-a će prijaviti
grešku, ali ostatak aplikacije radi normalno.

## Pokretanje u Android Studio

1. Otvorite projekat: `File > Open` i izaberite root folder projekta.
2. Sačekajte da Android Studio završi sync (`Gradle Sync finished`).
3. Proverite da je izabrana konfiguracija `app`.
4. Pokrenite build (`Build > Make Project` ili `Build > Rebuild Project`).
5. Pokrenite aplikaciju: klik na `Run 'app'` (zeleni trougao), pa izaberite
   emulator ili povezani telefon.

## Pokretanje na fizičkom telefonu

1. Uključite `Developer options` i `USB debugging`.
2. Povežite telefon USB kablom i prihvatite RSA prompt na telefonu.
3. U Android Studio izaberite uređaj i pokrenite `Run 'app'`.

> Za igru **Moj broj** koristi se i **shake senzor** (protresanje telefona),
> što se najbolje testira na fizičkom uređaju.

Ako dobijete grešku `INSTALL_FAILED_USER_RESTRICTED Installation via USB is disabled`:

- Na telefonu uključite opciju `Install via USB` (ako postoji kod vašeg proizvođača).
- U `Developer options` uradite `Revoke USB debugging authorizations`, pa ponovo odobrite.
- Probajte drugi USB kabl/port ili emulator.

## Korišćenje aplikacije

1. **Registracija** — unesite email, korisničko ime, region i lozinku.
2. **Verifikacija email-a** — na uneti email stiže verifikacioni kod; upišite ga
   u ekranu za verifikaciju. **Prijava nije moguća dok nalog nije potvrđen.**
3. **Prijava** — nakon potvrde, prijavite se korisničkim imenom/email-om i lozinkom.
4. **Glavni meni** — kartice za profil, notifikacije i igre.
5. **Profil** — prikaz podataka, statistike, **izmena lozinke** i **odjava**.
6. **Notifikacije** — 4 kategorije (chat, rang, nagrade, ostalo); test dugmad
   šalju sistemsku notifikaciju i upisuju je u listu sa filterima
   (sve / po kategoriji / pročitane / nepročitane).

### Igre (pravila po specifikaciji)

Za sada se igra u single-player režimu protiv **simuliranog protivnika**
(pravo uparivanje igrača dolazi u kasnijoj kontrolnoj tački). Svaka igra ima
2 runde — prvu igra korisnik, druga (protivnikova) je simulirana, uz priliku
za „krađu" poena.

- **Korak po korak** (2×70s, max 40): pogađa se pojam kroz najviše 7 koraka po
  10s; 1. korak nosi 20 poena, svaki naredni −2.
- **Moj broj** (2×1min, max 20): klik na **STOP** (ili protresanje telefona)
  otkriva traženi broj pa 6 brojeva; pomoću njih i operacija `( ) + - * /` treba
  dobiti traženi broj (10 poena za tačan rezultat).
- **Skočko** (2×30s, max 40): pogađa se kombinacija 4 simbola u 6 pokušaja;
  crveni indikator = simbol i pozicija, žuti = samo simbol.

Tokom igre, izlazak (strelica nazad ili „X") traži potvrdu da se igra ne bi
slučajno napustila.

## Notifikacije (Android 13+)

Za test notifikacije potrebno je odobriti runtime dozvolu `POST_NOTIFICATIONS`.

- Kada aplikacija zatraži dozvolu, kliknite `Allow`.
- Ako ste odbili dozvolu, uključite je ručno:
  `Settings > Apps > Slagalica Quiz > Notifications > Allow`.

## Pokretanje iz terminala (PowerShell)

Iz root foldera projekta pokrenite:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Opciono (ako je uređaj povezan):

```powershell
.\gradlew.bat installDebug
```

## Korisne komande

```powershell
.\gradlew.bat lintDebug
.\gradlew.bat tasks
.\gradlew.bat --stop
.\gradlew.bat app:dependencies
```

## Struktura (bitno za pokretanje)

- Glavni modul: `app`
- Ulazna aktivnost: `MainActivity` (preusmerava na `LoginActivity` ako korisnik nije prijavljen)
- Manifest: `app/src/main/AndroidManifest.xml`
- Lokalna baza: `data/AppDbHelper` (SQLite — korisnici, rezultati partija, notifikacije)
- Slanje email-a: `util/EmailSender` (kredencijali iz `local.properties`)
- Simulirani protivnik: `logic/BotOpponent`
