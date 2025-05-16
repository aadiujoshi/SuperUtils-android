package com.example.superutils.data

class CharSequenceTracker(sequenceToMatch: ByteArray) {
    private val sequence: ByteArray = sequenceToMatch
    private var matched = 0
    var relativeStart = -1

    private var complete = false

    fun nextChar(char: Byte, relativeIndex: Int = -1) {
        if (complete) return

        if (sequence[matched] == char) {
            if (matched == 0) {
                this.relativeStart = relativeIndex
            }
            matched += 1
        } else {
            matched = 0
            if (sequence[0] == char) {
                matched += 1
            }
        }
    }

    fun found(): Boolean {
        if (complete) {
            return false
        }
        if (matched == sequence.size) {
            return true;
        }
        return false;
    }

    fun reset() {
        complete = false
        matched = 0
        relativeStart = -1
    }

    fun markComplete() {
        complete = true
    }
}