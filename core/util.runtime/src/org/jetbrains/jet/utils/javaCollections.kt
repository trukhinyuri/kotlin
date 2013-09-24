package org.jetbrains.jet.utils

import java.util.Collections

public fun <T> emptyList(): MutableList<T> = Collections.emptyList<T>() as MutableList
public fun <T> emptySet(): MutableSet<T> = Collections.emptySet<T>() as MutableSet
public fun <K, V> emptyMap(): MutableMap<K, V> = Collections.emptyMap<K, V>() as MutableMap