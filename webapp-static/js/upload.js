angular.module('lPhoto').
component("upload", {
    templateUrl: "templates/upload.html",
    controller: ['$scope', '$http', '$location',
        function UploadController($scope, $http, $location) {
            console.log("UploadController called");

            var self = this;
            self.fileName = "";
            self.fileSize = null;
            self.fileType = null;
            self.fileData = null;
            self.description = "";

            // apparently Angular won't bind to the file-chooser, so we'll add
            // an explicit event handler

            document.getElementById('fileChooser').onchange = function(event) {
                console.log("file selection changed");

                var file = event.target.files[0];
                self.fileName = file.name
                self.fileSize = file.size
                self.fileType = file.type
                self.description = "";
                $scope.$apply();

                var reader = new FileReader();
                reader.onload = function(evt) {
                    self.fileData = new Uint8Array(reader.result);
                    $scope.$apply();
                }
                reader.readAsArrayBuffer(file);
            };


            self.doCancel = function() {
                $location.path("/main");
            }


            self.canDoUpload = function() {
                return self.fileData != null;
            }


            self.doUpload = function () {
                console.log("doUpload: starting");

                // this is a chain of async actions
                // happening, which would mean that the buffer would be associated with
                self.doUpload1(self.fileData)

                // disable upload button while in process of upload
                self.fileData = null;
                $scope.$apply();
            }


            self.doUpload1 = function (fileData) {
                console.log("doUpload: requesting upload URL");
                $http.post('api/requestUpload', {
                    "filename":     self.fileName,
                    "mimetype":     self.fileType,
                    "description":  self.description
                })
                .then(
                    function(response) {
                        if (response.data.responseCode === "SUCCESS") {
                            console.log("doUpload: successfully retrieved URL");
                            self.doUpload2(response.data.data, fileData)
                        }
                        else if (response.data.responseCode === "NOT_AUTHENTICATED") {
                            console.log("must authenticate");
                            $location.path("/signIn");
                        }
                        else {
                            alert("got: " + response.data.responseCode);
                        }
                    },
                    function(reason) {
                        alert("upload failed: " + reason);
                    });
            }


            self.doUpload2 = function(uploadUrl, fileData) {
                console.log("doUpload: uploading file");
                console.log("data size: " + fileData.byteLength)
                $http.put(uploadUrl, fileData, {
                    transformRequest: [],
                    headers: {
                        "Content-Type": self.fileType
                    }
                })
                .then(
                    function(response) {
                        console.log("doUpload: successfully uploaded file");
                        $location.path("/main");
                    },
                    function(reason) {
                        alert("upload failed: " + reason);
                    });
            }
        }
    ]
});
