# تطبيق ورشة فوري - منصة الورش المتنقلة وصيانة المركبات (`warsha-now-services-1289836`)
> منظومة الورش المتنقلة وخدمات الصيانة الميدانية المتكاملة مع بطاقات مدى Contactless EMV L2 والتوجيه اللحظي عبر GPS

- **Project ID**: `#1289836`
- **Commercial Archetype**: `services`
- **Bound Hardware Peripheral**: `Mada Contactless EMV L2 NFC + GPS Dispatch`
- **Package Name**: `sa.factory.warshanowservices1`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Clean Vector/ASCII)

---

## [Sidekick Autonomous Update]

### 1. Summary of Completed Enhancements
- **Resolved Kotlin Template Hazards**: Extracted fallback peripheral strings in Compose screens to prevent Kotlin single-quote interpolation compiler failures.
- **Modernized Compose Components**: Upgraded deprecated dividers to `HorizontalDivider` and added missing Material3 text formatting imports (`FontWeight`, `sp`).
- **Clean Invariant Enforcement**: Stripped unicode checkmarks and non-standard symbols in compliance with the Zero-Emoji enterprise standard.
- **Relocated Test Suite**: Moved regression and engine tests into `app/src/test/java/sa/factory/warshanowservices1/` for full Gradle JUnit runner integration.
- **Embedded Gradle Wrapper**: Bundled official Gradle wrapper binaries and comprehensive `.gitignore` rules for autonomous CI/CD compatibility.

### 2. Architecture & Domain Engines
- **MadaContactlessEmvL2ProtocolEngine (`sa.factory.warshanowservices1.engine.MadaContactlessEmvL2ProtocolEngine`)**:
  - Implements Saudi Central Bank (SAMA) Mada Contactless EMV Level 2 kernel protocol parsing (AID `A000000228`).
  - Calculates Application Request Cryptograms (ARQC), derives session keys, and enforces SAMA CVM contactless limits (<= 300 SAR without PIN).
  - Emits structured financial transaction audit logs and approval codes (`MADA-XXXXXX`).
- **MobileWorkshopDispatchRoutingEngine (`sa.factory.warshanowservices1.engine.MobileWorkshopDispatchRoutingEngine`)**:
  - Intelligent fleet dispatch engine for mobile workshop vans covering major service quadrants.
  - Implements Haversine geodesic GPS distance calculations and dynamic ETA estimations based on urban traffic conditions.
  - Manages SLA queues for Emergency Roadside (20 min SLA), Priority Home Service, and Periodic Maintenance.
- **HardwareSensorDriver Telemetry Integration**:
  - Emits continuous real-time state frames combining Mada NFC card reader status, transaction volume, active mobile workshop availability, and GPS coordinates.

### 3. Step-by-Step Build and Test Instructions
```bash
# Run comprehensive unit and regression test suite
./gradlew test

# Build debug APK for cloud test harness deployment
./gradlew assembleDebug
```
