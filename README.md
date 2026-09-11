# Net Audit Panel Android

Minimalny fundament aplikacji Android do jawnych, autoryzowanych testów własnych urządzeń i sieci.

## Checkpoint 2

- Kotlin, Gradle Kotlin DSL i Jetpack Compose
- `compileSdk = 36`, `targetSdk = 36`, Java 17
- wspólny kontrakt `BluetoothTransport`
- oddzielne kontrakty BLE oraz Bluetooth Classic/RFCOMM
- uprawnienia Bluetooth dla Androida 12+ oraz zgodność wsteczna
- Nordic Android BLE Library jako jedyna zewnętrzna biblioteka BLE

Ten checkpoint nie implementuje jeszcze skanowania, łączenia, przechwytywania danych ani operacji GATT/RFCOMM. Ekran aplikacji jest wyłącznie potwierdzeniem uruchomienia fundamentu.

## Budowanie

Wymagane: JDK 17 oraz Android SDK Platform 36.

```bash
./gradlew testDebugUnitTest assembleDebug
```

Test na fizycznym telefonie jest osobnym krokiem i nie jest zastępowany testem jednostkowym.
