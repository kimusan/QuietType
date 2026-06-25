package dk.schulz.quiettype.dictation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveTranscriptSegmenterTest {
    @Test
    fun emitsCompletedSentencesOnlyOnce() {
        val segmenter = LiveTranscriptSegmenter()

        assertNull(segmenter.nextCompletedSegment("hello wor"))
        assertEquals("hello world.", segmenter.nextCompletedSegment("hello world. this is"))
        assertNull(segmenter.nextCompletedSegment("hello world. this is"))
        assertEquals("this is live.", segmenter.nextCompletedSegment("hello world. this is live."))
    }

    @Test
    fun emitsStableWordChunksWhenStreamingModelHasNoPunctuation() {
        val segmenter = LiveTranscriptSegmenter()

        assertNull(segmenter.nextStableSegment("HELLO"))
        assertEquals("Hello", segmenter.nextStableSegment("HELLO WORLD"))
        assertEquals("world", segmenter.nextStableSegment("HELLO WORLD THIS"))
        assertEquals("this is", segmenter.nextStableSegment("HELLO WORLD THIS IS LIVE"))
    }

    @Test
    fun remainingSegmentReturnsUncommittedTailAtStop() {
        val segmenter = LiveTranscriptSegmenter()

        assertEquals("First sentence.", segmenter.nextCompletedSegment("FIRST SENTENCE. UNFINISHED TAIL"))
        assertEquals("unfinished tail", segmenter.remainingSegment("FIRST SENTENCE. UNFINISHED TAIL"))
        assertNull(segmenter.remainingSegment("FIRST SENTENCE. UNFINISHED TAIL"))
    }

    @Test
    fun preservesMixedCaseTranscripts() {
        val segmenter = LiveTranscriptSegmenter()

        assertEquals("Hello", segmenter.nextStableSegment("Hello world"))
        assertEquals("world", segmenter.remainingSegment("Hello world"))
    }
}
