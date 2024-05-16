/**
 * Copyright (c) 2024 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.ui.dal

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.quartz.events.MuteListEvent
import com.vitorpamplona.quartz.events.NIP90ContentDiscoveryResponseEvent
import com.vitorpamplona.quartz.events.PeopleListEvent

open class NIP90ContentDiscoveryFilter(
    val account: Account,
    val dvmkey: String,
    val request: String,
) : AdditiveFeedFilter<Note>() {
    override fun feedKey(): String {
        return account.userProfile().pubkeyHex + "-" + request
    }

    open fun followList(): String {
        return account.defaultDiscoveryFollowList.value
    }

    override fun showHiddenKey(): Boolean {
        return followList() == PeopleListEvent.blockListFor(account.userProfile().pubkeyHex) ||
            followList() == MuteListEvent.blockListFor(account.userProfile().pubkeyHex)
    }

    override fun feed(): List<Note> {
        val params = buildFilterParams(account)

        val notes =
            LocalCache.notes.filterIntoSet { _, it ->
                val noteEvent = it.event
                noteEvent is NIP90ContentDiscoveryResponseEvent && it.event!!.isTaggedEvent(request)
                //  it.event?.pubKey() == dvmkey && it.event?.isTaggedUser(account.keyPair.pubKey.toHexKey()) == true // && params.match(noteEvent)
            }

        var sorted = sort(notes)
        if (sorted.isNotEmpty()) {
            var note = sorted.first()

            var eventContent = note.event?.content()

            var collection: MutableSet<Note> = mutableSetOf()
            val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            var etags = mapper.readValue(eventContent, List::class.java)
            for (element in etags) {
                var tag = mapper.readValue(mapper.writeValueAsString(element), Array::class.java)
                val note = LocalCache.checkGetOrCreateNote(tag[1].toString())
                if (note != null) {
                    collection.add(note)
                }
            }

            return collection.toList()
        } else {
            return listOf()
        }
    }

    override fun applyFilter(collection: Set<Note>): Set<Note> {
        return innerApplyFilter(collection)
    }

    fun buildFilterParams(account: Account): FilterByListParams {
        return FilterByListParams.create(
            account.userProfile().pubkeyHex,
            account.defaultDiscoveryFollowList.value,
            account.liveDiscoveryFollowLists.value,
            account.flowHiddenUsers.value,
        )
    }

    protected open fun innerApplyFilter(collection: Collection<Note>): Set<Note> {
        // val params = buildFilterParams(account)

        val notes =
            collection.filterTo(HashSet()) {
                val noteEvent = it.event
                noteEvent is NIP90ContentDiscoveryResponseEvent && // &&
                    it.event!!.isTaggedEvent(request) // && it.event?.isTaggedUser(account.keyPair.pubKey.toHexKey()) == true // && params.match(noteEvent)
            }

        val sorted = sort(notes)
        if (sorted.isNotEmpty()) {
            var note = sorted.first()

            var eventContent = note.event?.content()
            // println(eventContent)

            val collection: MutableSet<Note> = mutableSetOf()
            val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            var etags = mapper.readValue(eventContent, Array::class.java)
            for (element in etags) {
                var tag = mapper.readValue(mapper.writeValueAsString(element), Array::class.java)
                val note = LocalCache.checkGetOrCreateNote(tag[1].toString())
                if (note != null) {
                    collection.add(note)
                }
            }
            return collection
        } else {
            return hashSetOf()
        }
    }

    override fun sort(collection: Set<Note>): List<Note> {
        return collection.toList() // collection.sortedWith(compareBy({ it.createdAt() }, { it.idHex })).reversed()
    }
}