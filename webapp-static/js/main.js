angular.module('lPhoto').
component("main", {
    templateUrl: "https://" + window.STATIC_HOST + "/templates/main.html",
    controller: ['$scope', '$http', '$location',
        function MainController($scope, $http, $location, $sceDelegateProvider) {
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

            self.hasSizes = function(file) {
                return !! (file && file.sizes && file.sizes.length > 0);
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
