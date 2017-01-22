angular.module('lPhoto').
component("main", {
    templateUrl: "templates/main.html",
    controller: ['$http', '$location',
        function MainController($http, $location) {
            console.log("MainController called");

            var self = this;
            self.authorized = false;
            self.checking = true;

            // placeholder request to verify that we're authenticated

            $http.post('api/checkAuth', {})
            .then(
                function(response) {
                    self.checking = false;
                    if (response.data.responseCode === "SUCCESS") {
                        console.log("auth check got success");
                        self.authorized = true;
                    }
                    else {
                        console.log("auth check got " + response.data.responseCode);
                        self.authorized = false;
                    }
                },
                function(reason) {
                    console.log("auth check failed: " + reason);
                    self.authorized = false;
                });
        }
    ]
});
