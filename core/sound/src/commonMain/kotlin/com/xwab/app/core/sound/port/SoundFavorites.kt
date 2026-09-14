package com.xwab.app.core.sound.port

/**
 * The favorites namespace sounds are saved under.
 *
 * `FavoritesPort` namespaces are caller-owned strings that become part of the key on disk. This one
 * read `music` for as long as the catalog model was called that, and was kept after the model
 * became [Track] on the grounds that a stored name must not move — which is true of a name anything
 * already holds, and this app has not shipped: no installed copy holds it, so it moved with the
 * model rather than staying behind as the last word of a vocabulary nothing else uses. It reads
 * `sound` now, the same name `:core:sources` gives the same content kind.
 *
 * From the first release on that argument reverses: changing this then drops every favorite saved
 * under the old name, and costs a data migration rather than a rename.
 *
 * It belongs here beside [TrackId] because it names the identity rather than any one screen.
 * Stated at each call site instead, it was five literals across three feature modules, and a typo
 * in one of them is not something the compiler has anything to say about: that screen would
 * quietly read an empty store rather than fail to build.
 */
const val SOUND_FAVORITES_NAMESPACE: String = "sound"
