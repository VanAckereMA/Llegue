package ru.rescuesmstracker.data.migration

import io.realm.DynamicRealm

/**
 * This migration appeared after the Kotlin major version update.
 * The data type definitions in Kotlin objects didn't change but looks like
 * some bytecode-level changes took place.
 */
class MigrationRule3 : MigrationRule {
    override fun migrate(realm: DynamicRealm) {
        val schema = realm.schema
        schema.get("Contact")
            ?.setNullable("photoUriString", false)
        schema.get("Sms")
            ?.setNullable("status", false)
            ?.setNullable("type", false)
    }

    override fun toVersion(): Long = 3
}