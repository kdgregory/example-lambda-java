# Database

Image metadata is stored in a DynamoDB table that has the following attributes:

| Attribute Name    | Description
|-------------------|------------
| `username`        | User that owns the file. Assigned during upload, based on logged-in user.
| `id`              | Unique identifier for the file, assigned during upload.
| `filename`        | The original filename.
| `description`     | User-provided description.
| `mimetype`        | The standard MIME type for the file.
| `uploadedAt`      | The millis-since-epoch timestamp when the file was uploaded.
| `sizes`           | An array of strings that identifies the various resolutions that have been saved for the file.


## Keys and Indexes

The current implementation uses a composite key: `username` as the partition key, and `id` as the sort key.

Partitioning by username is a consequence of the primary retrieval pattern, which is a
list of photos by user. This is, however, sub-optimal for a system with a small number
of users, where records may be concentrated in a single shard.

I considered using `uploadedAt` as the sort key, to give users a consistent listing.
However, there is no way to guarantee that `uploadedAt` would be unique, even with
millisecond precisions. And it's easy enough to sort the array before returning it.

There's a global secondary index on `id`, to support the Resizer's need to retrieve
a newly uploaded photo's metadata knowing only its ID. My one concern in doing this
is that GSIs are eventually consistent. However, there should be enough of a delay
between writing the initial metadata and needing to retrieve it that the index will
become consistent.


## Sizes

The Resizer produces a fixed set of renditions, controlled by the `Sizes` enum. This
field holds the names of the size values that have been written to the image bucket.
The files are written to S3 using the key `images/photoId/size`, so it's easy for
the client to generate image URLs knowing the size.

The `sizes` field -- more correctly, its absence -- also indicates a newly uploaded
photo. If the Resizer sees an empty field, it first moves the photo from the Uploads
bucket to the Images bucket (and then saves the metadata with `ORIGINAL` as the only
size).
