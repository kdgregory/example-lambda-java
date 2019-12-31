# Database

All file metadata is stored in a DynamoDB table that has the following attributes:

| Attribute Name    | Description
|-------------------|------------
| `username`        | User that owns the file. Also assigned during upload, based on logged-in user.
| `id`              | Unique identifier for the file, assigned during upload.
| `filename`        | The original filename.
| `description`     | User-provided description.
| `mimetype`        | The standard MIME type for the file.
| `uploadedAt`      | The millis-since-epoch timestamp when the file was uploaded.
| `sizes`           | An array of strings that identifies the various resolutions that have been saved for the file.


The current implementation uses a composite key: `username` as the partition key, and `id` as the sort key. This seems
counter-intuitive, as `id` is a unique value, but is driven by the following:

* We can use Query for all retrievals, without needing a global secondary index (which only provides eventual consistency
  and also increases the provisioned throughput and therefore the cost of the table).
* The primary retrieval operation is a list of all photos by user (and for retrieving an individual photo, it's not too
  onerous to require that the retriever know the username).
* In a production environment, with a large number of users, hashing by username should provide a sufficiently-performant
  distribution of data (unless, of course, there's one user with an orders-of-magnitude more photos than another).

