angular.module('lPhoto').
component("main", {
    templateUrl: "templates/main.html",
    controller: ['$scope', '$http', '$location',
        function MainController($scope, $http, $location) {
            console.log("MainController called");

            var self = this;
            self.fileList = [];

            self.upload = function() {
                $location.path("/upload");
            };

            self.dateHelper = function(file) {
                if (file && file.uploadedAt) {
                    return "Uploaded " + (new Date(file.uploadedAt)).toLocaleString();
                }
                else {
                    return "";
                }
            }

            self.refresh = function() {
                console.log("refreshing file list");
                $http.get('api/list')
                .then(
                    function(response) {
                        if (response.data.responseCode === "SUCCESS") {
                            console.log("got success");
                            self.fileList = response.data.data;
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
                        alert("file listing failed: " + reason);
                    });
            };

            // load initial list
            self.refresh();
        }
    ]
});
