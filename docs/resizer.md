# Resizer Implementation

The Resizer exists to demonstrate asynchronous Lambda invocation; it uses the
[Java Image IO](http://docs.oracle.com/javase/8/docs/technotes/guides/imageio/index.html)
package to produce fixed-size versions of the original image. This won't give you the
highest-quality scaling, but as I said in the README, this isn't production-ready code.

Uploading and resizing photos is a multi-step process:

1. The client invokes the WebApp's `requestUpload` endpoint, providing photo metadata
   (filename, description, mime type). The WebApp generates a unique photo ID, saves
   the metadata under this ID, and returns a signed URL for the upload bucket.
2. The client then invokes a PUT against this URL, to upload the content.
3. The upload bucket notifies the Resizer that a new file has been uploaded.
4. The Resizer verifies that metadata exists for the file (there should be no way
   for this to fail), moves the file from the uploads bucket to the images bucket,
   then updates the metadata with the sizes available.

To support resizing, the file content is loaded into memory; destination sizes are
also staged as in-memory byte arrays. To ensure that this works, the Resizer has a
1 GB memory configuration. It will work with less (but probably not less than 256MB),
but the larger memory also means more CPU resources.
