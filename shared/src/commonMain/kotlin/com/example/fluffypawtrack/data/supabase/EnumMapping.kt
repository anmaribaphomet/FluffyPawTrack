package com.example.fluffypawtrack.data.supabase

/*
 * Enum mapping between Postgres and Kotlin (supabase/README.md, "Enum mapping").
 *
 * Every database enum value is the lower snake case of its Kotlin constant
 * (`OnTheWay` <-> `on_the_way`, `AdoptionCenter` <-> `adoption_center`), so one rule covers
 * all twelve types. EnumMappingTest pins the README table value by value.
 */

/** The Postgres value of this Kotlin enum constant. */
internal val Enum<*>.dbValue: String
    get() = buildString {
        name.forEachIndexed { index, char ->
            if (char.isUpperCase() && index > 0) append('_')
            append(char.lowercaseChar())
        }
    }

/** The Kotlin constant of a Postgres enum value; fails for values the app does not know. */
internal inline fun <reified E : Enum<E>> fromDb(value: String): E =
    enumValues<E>().firstOrNull { it.dbValue == value }
        ?: throw IllegalArgumentException("Unknown ${E::class.simpleName} value: $value")
