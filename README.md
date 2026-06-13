# e2-ma-tim17-2026

Android aplikacija (KT1 GUI) za predmet Mobilne aplikacije.

## Brzi start

1. Otvorite projekat u Android Studio (`File > Open`) i izaberite root folder `e2-ma-tim17-2026`.
2. Sacekajte da se uradi Gradle sync.
3. Kliknite `Run 'app'` i izaberite emulator ili fizicki uredjaj.

## Preduslovi

- Android Studio 
- Android SDK instaliran kroz Android Studio
- JDK 11 (projekat koristi Java 11)
- Internet konekcija (za Gradle zavisnosti)

Parametri aplikacije:

- `minSdk = 30`
- `targetSdk = 36`
- `compileSdk = 36`

## Pokretanje na fizickom telefonu

### Priprema telefona (jednom)

1. `Podesavanja > O telefonu > Informacije o softveru` i tapnite 7 puta na `Broj verzije (Build number)` dok ne pise da ste programer.
2. Vratite se u `Podesavanja > Opcije za programere` i ukljucite `USB otklanjanje gresaka (USB debugging)`.
3. Povezite telefon USB kablom i na telefonu prihvatite prozor `Dozvoliti USB otklanjanje gresaka?` (stiklirajte `Uvek dozvoli sa ovog racunara`).

### Iz Android Studija (najlakse)

1. U gornjoj traci, pored zelenog dugmeta `Run`, iz padajuceg menija izaberite ime svog telefona.
2. Kliknite `Run 'app'` (ili `Shift + F10`). Studio sam izbilduje, instalira i pokrene aplikaciju.

### Iz terminala (PowerShell)

```powershell
adb devices                 # telefon treba da se vidi sa oznakom "device"
.\gradlew.bat installDebug  # izbilduje i instalira na telefon
adb shell monkey -p com.example.myapplication -c android.intent.category.LAUNCHER 1   # pokrene app
```

Ako `adb` nije u PATH-u, koristite punu putanju:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

Ako dobijete gresku `INSTALL_FAILED_USER_RESTRICTED Installation via USB is disabled`:

- Na telefonu ukljucite opciju `Install via USB` (ako postoji kod vaseg proizvodjaca).
- U `Developer options` uradite `Revoke USB debugging authorizations`, pa ponovo odobrite.
- Probajte drugi USB kabl/port ili emulator.

## Notifikacije (Android 13+)

Za test notifikacije potrebno je odobriti runtime dozvolu `POST_NOTIFICATIONS`.

- Kada aplikacija zatrazi dozvolu, kliknite `Allow`.
- Ako ste odbili dozvolu, ukljucite je rucno:
  `Settings > Apps > My Application > Notifications > Allow`.

## Pokretanje iz terminala (PowerShell)

Iz root foldera projekta pokrenite:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Opcionalno (ako je uredjaj povezan):

```powershell
.\gradlew.bat installDebug
```

## Korisne komande

```powershell
.\gradlew.bat tasks
.\gradlew.bat --stop
.\gradlew.bat app:dependencies
```

## Struktura (bitno za pokretanje)

- Glavni modul: `app`
- Ulazna aktivnost: `MainActivity`
- Manifest: `app/src/main/AndroidManifest.xml`



