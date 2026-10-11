package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models

object SamplePosts {
    private fun img(seed: String) = "https://picsum.photos/seed/$seed/800/800"
    private fun avatar(seed: String) = "https://picsum.photos/seed/$seed/120/120"

    private const val LONG_CAPTION =
        "Spent the whole weekend exploring the ridge line and honestly could not stop taking " +
            "photos. The light at golden hour made everything look unreal, and the climb was " +
            "worth every step. Already planning the next trip, so let me know if anyone wants " +
            "to join. Bring snacks and a warm layer because it gets cold fast up there."

    val all: List<Post> = listOf(
        Post("p1", "maya.hikes", avatar("a1"), listOf(img("p1a")), "Quiet morning on the trail.", 42, 1, Vote.UP),
        Post("p2", "jordan_k", avatar("a2"), listOf(img("p2a"), img("p2b"), img("p2c")), LONG_CAPTION, 128, 6),
        Post("p3", "sam.codes", avatar("a3"), listOf(img("p3a")), "", 0, 0),
        Post("p4", "priya", avatar("a4"), listOf(img("p4a"), img("p4b")), "Two views, same sunset.", 87, 3, Vote.DOWN),
        Post("p5", "leo_outdoors", avatar("a5"), listOf(img("p5a"), img("p5b"), img("p5c"), img("p5d")), "Gear check before heading out. Which one would you leave behind?", 15, 9),
        Post("p6", "no.photo.nina", avatar("a6"), emptyList(), "A post with only words, no pictures.", 3, 0),
        Post("p7", "tess", null, listOf(img("p7a")), LONG_CAPTION, 220, 14),
    )
}
