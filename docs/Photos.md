# Photo system docs

In a nutshell, the system takes a photo, compresses it, and then encodes it into a lossy webp. This then gets stored as raw bytes in the database.

## Important Note
The system only supports the activity feed right now. Meaning, photos can only be uploaded to the database and loaded via the activity page/post. 

The system will need more development time in order to support other pages, like the observation journal but, as of writing this, we are out of time for sprint 2.

## limitations
Max photos per post - 5
longest side of a file - 800px
Stored Format - Lossy WebP
Size budget per photo - ~60KB
Hard cap per Photo - 100KB
Accepted inputs - JPEG, PNG, WebP (via file signature)
Largest accepted input file - 25 MB

Note, other formats are refused by the system. The system does not support gifs or videos either.

## Usage
val container = (application as EkhoApp).container

// Each source is a function that opens the photo; they are opened one at a time.
val photos: List<EncodedPhoto> = container.photoPreparer.prepare(
    uris.map { uri -> { contentResolver.openInputStream(uri)!! } }
)
container.feedRepository.addPost(caption = text, imageUrls = emptyList(), photos = photos)

### Notes
prepare is being ran off of the main thread. No partial returns. Stops if it refuses a file
addPost is used to write the post and its photos into the db
a post can use both imageUrls or photos now.
- Image urls are the legacy system from sprint 1

## Example
if it still exists, there is a tempPhoto page available to use to test

## Loading photos
This is already handled by the post card

## Cache
There is a interface to handle caching the photos but it is not hooked up to anything currently.

