package com.xwab.app.core.shuffle

import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId

/**
 * The decks this build ships. A deck's id is stored on devices — in the engine's source id and the
 * now-playing state — so keep it once it has shipped.
 */
internal val shuffleDecks: List<ShuffleDeck> = listOf(
    ShuffleDeck(
        id = ShuffleDeckId(EVERYDAY_THINGS),
        title = "Everyday Things",
        description = "Ordinary objects, animals and places, one at a time and in no particular order.",
        languageTag = "en-US",
        wordIntervalSeconds = 8,
    ),
)

/**
 * The words behind each deck in [shuffleDecks].
 *
 * Chosen for the technique rather than for variety alone: concrete things that are easy to picture,
 * neutral in feeling, and unrelated to one another. Nothing that alarms (fire, spiders, knives),
 * nothing that points back at the day (phones, desks, emails), and no people, so a word gives the
 * mind something to look at rather than something to think about. Written for this app.
 */
internal val shuffleWordLists: List<ShuffleWordList> = listOf(
    ShuffleWordList(
        deckId = EVERYDAY_THINGS,
        // Eight minutes at eight seconds a word before the track loops, about 23 MB at a voice's
        // usual 24 kHz. Every play draws a new selection.
        wordsPerTrack = 60,
        words = listOf(
            "acorn", "anchor", "apple", "apron", "armchair", "avocado", "backpack", "bagel",
            "balloon", "bamboo", "banana", "barn", "basket", "beach ball", "bell", "bench",
            "bicycle", "birdhouse", "blanket", "blueberry", "boat", "bonnet", "bookshelf", "bottle",
            "bowl", "bracelet", "bread", "brick", "bridge", "broom", "bubble", "bucket",
            "butterfly", "button", "cabbage", "cactus", "cake", "camel", "candle", "canoe",
            "cap", "carousel", "carrot", "castle", "cello", "chair", "cherry", "chess piece",
            "chimney", "cloud", "clover", "coat", "coconut", "comb", "compass", "cookie",
            "cork", "corn", "cottage", "cow", "crayon", "crown", "cucumber", "cup",
            "cupcake", "curtain", "cushion", "daisy", "deer", "dolphin", "donkey", "doorknob",
            "dragonfly", "duck", "eagle", "envelope", "eraser", "fan", "feather", "fence",
            "fern", "ferry", "flag", "flamingo", "flowerpot", "flute", "fork", "fountain",
            "fox", "frog", "gate", "giraffe", "glove", "goat", "goldfish", "grape",
            "guitar", "hammock", "harbor", "harp", "hat", "haystack", "hedgehog", "honey",
            "horse", "igloo", "island", "jacket", "jar", "kangaroo", "kettle", "key",
            "kite", "koala", "ladder", "ladybug", "lake", "lamp", "lantern", "leaf",
            "lemon", "lettuce", "lighthouse", "lily", "llama", "mailbox", "map", "maple",
            "marble", "meadow", "melon", "mitten", "moon", "moss", "mountain", "mug",
            "mushroom", "napkin", "necklace", "nest", "notebook", "oak", "oar", "olive",
            "onion", "orange", "ostrich", "otter", "owl", "paddle", "paintbrush", "palm tree",
            "pancake", "panda", "parrot", "peach", "peacock", "pear", "pebble", "pelican",
            "pencil", "penguin", "pepper", "piano", "pillow", "pine cone", "pineapple", "plate",
            "plum", "pond", "popcorn", "postcard", "potato", "pumpkin", "puzzle", "quilt",
            "rabbit", "raft", "rainbow", "raspberry", "ribbon", "river", "robin", "rocking chair",
            "rope", "rose", "rowboat", "rug", "saddle", "sailboat", "sandal", "sandcastle",
            "scarf", "seashell", "sheep", "shovel", "skateboard", "sled", "slipper", "snail",
            "snowflake", "snowman", "sock", "sofa", "spoon", "squirrel", "starfish", "stool",
            "strawberry", "sunflower", "swan", "sweater", "swing", "table", "teacup", "teapot",
            "teddy bear", "tent", "thimble", "tomato", "toothbrush", "towel", "tractor", "train",
            "treehouse", "trumpet", "tulip", "turtle", "umbrella", "vase", "violin", "wagon",
            "walnut", "watering can", "watermelon", "whale", "wheel", "wheelbarrow", "whistle", "windmill",
            "window", "wool", "yarn", "zebra", "zipper",
        ),
    ),
)

private const val EVERYDAY_THINGS = "everyday-things"
