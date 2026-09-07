package com.aethernet.aethercontrol.data.local

// =============================================================================
// AppDatabase.kt — Esqueleto Room | 6º Semestre UTP | MOV-08
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 1 año PostgreSQL (Room es SQLite local como PostgreSQL pero en móvil),
//              2 años Python (SQLAlchemy), 2 años JS/React (IndexedDB/localStorage)
// Analogía React: este archivo es como `db.ts` con `new Dexie('AppDatabase')` pero vacío —
// placeholder para Room que será el `IndexedDB` de Android (SQLite local).
// Analogía Python: como `Base(DeclarativeBase)` en backend/app/database.py pero para móvil —
// aquí las entidades serán `AccessEventEntity` con `@Entity` como `Column` en SQLAlchemy.
// Analogía PostgreSQL: Room es el `init.sql` local del teléfono — cache offline cuando no hay WiFi
// (como el MEGA que sigue con Edge sin red, ver prd.md:61 Contingencia).
// FOSS: Room (AndroidX, Apache 2.0) — RNF-3.1, sin Realm propietario.
// Estado: Esqueleto preparatorio para MOV-08 — docs/backlog.md:25 (sin @Database aún para no romper ksp).
// Cuando se añada Room, descomentar y añadir entidades. ServiceLocator no lo instancia aún.
// =============================================================================

/**
 * Esqueleto preparatorio para MOV-08 — docs/backlog.md:25.
 * Sin anotación @Database ni dependencia Room aún para no romper ksp (KSP necesita plugin Room).
 * Cuando se añada Room, descomentar y añadir entidades. Mantiene API estable para ServiceLocator.
 *
 * // TODO MOV-08: entidades AccessEventEntity, SensorEventEntity
 * // @Database(entities = [AccessEventEntity::class, SensorEventEntity::class], version = 1)
 * // abstract class AppDatabase : RoomDatabase()
 */
class AppDatabase private constructor() {
    companion object {
        // Placeholder para mantener API estable; ServiceLocator no lo instancia aún.
        // En futuro: `fun getInstance(context: Context): AppDatabase` con Room.databaseBuilder (como TypeORM DataSource).
    }
}
