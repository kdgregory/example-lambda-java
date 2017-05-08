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

            self.refresh = function() {
                console.log("refreshing file list");
                $http.post('api/list', {})
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
