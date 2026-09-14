package com.xwab.app.designsystem.components

/**
 * How far a control fades when it is drawn but cannot be used.
 *
 * The controls here tint their icons explicitly, which overrides what `IconButton` would otherwise
 * do with a disabled content colour — so without this a refusing control looks exactly like a
 * working one, which is the bug it exists to prevent rather than a styling preference.
 *
 * Material's own disabled alpha, so a disabled icon here reads the same as a disabled `Slider` or
 * `Switch` sitting next to it.
 */
internal const val DISABLED_ALPHA: Float = 0.38f
