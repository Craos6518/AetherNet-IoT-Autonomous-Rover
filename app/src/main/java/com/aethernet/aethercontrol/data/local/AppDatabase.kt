package com.aethernet.aethercontrol.data.local

/**
 * Esqueleto preparatorio para MOV-08 — docs/backlog.md:25.
 * Sin anotación @Database ni dependencia Room aún para no romper ksp.
 * Cuando se añada Room, descomentar y añadir entidades.
 *
 * // TODO MOV-08: entidades AccessEventEntity, SensorEventEntity
 * // @Database(entities = [AccessEventEntity::class, SensorEventEntity::class], version = 1)
 * // abstract class AppDatabase : RoomDatabase()
 */
class AppDatabase private constructor() {
    companion object {
        // Placeholder para mantener API estable; ServiceLocator no lo instancia aún.
    }
}
