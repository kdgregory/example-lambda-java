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
            self.fileDataUrl = null;
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
                    self.fileDataUrl = reader.result;
                    $scope.$apply();
                }
                reader.readAsDataURL(file);
            };


            self.doCancel = function() {
                $location.path("/main");
            }


            self.canDoUpload = function() {
                return self.fileDataUrl != null;
            }


            self.doUpload = function () {
                console.log("doUpload called");
                $http.post('api/upload', {
                    "filename":     self.fileName,
                    "filesize":     self.fileSize,
                    "mimetype":     self.fileType,
                    "description":  self.description,
                    "content":      self.fileDataUrl.replace(/^data.*,/, "")
                })
                .then(
                    function(response) {
                        if (response.data.responseCode === "SUCCESS") {
                            console.log("got success");
                        }
                        else {
                            alert("got: " + response.data.responseCode);
                        }
                    },
                    function(reason) {
                        alert("upload failed: " + reason);
                    });

                $location.path("/main");
            }
        }
    ]
});
