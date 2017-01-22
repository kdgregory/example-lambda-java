angular.module('lPhoto').
component("signin", {
    templateUrl: "templates/signin.html",
    controller: ['$http', '$location',
        function SigninController($http, $location) {
            console.log("SigninController called");

            var self = this;
            self.signinEmail = "";
            self.signinPassword = "";
            self.signupEmail = "";


            self.canSubmitSignin = function() {
                return self.signinEmail && self.signinPassword;
            }


            self.submitSignin = function() {
                console.log("submitSignin(): " + self.signinEmail + " / " + self.signinPassword);
                $http.post('api/signin',
                           { "email": self.signinEmail,
                             "password": self.signinPassword})
                .then(
                    function(response) {
                        if (response.data.responseCode === "SUCCESS") {
                            console.log("got success, redirecting");
                            $location.path("/main");
                        }
                        else if (response.data.responseCode === "TEMPORARY_PASSWORD") {
                            console.log("got TEMPORARY_PASSWORD, redirecting");
                            $location.path("/confirmSignup");
                        }
                        else {
                            alert("got: " + response.data.responseCode);
                        }
                    },
                    function(reason) {
                        alert("signin failed: " + reason);
                    });
            }


            self.canSubmitSignup = function() {
                return self.signupEmail;
            }


            self.submitSignup = function() {
                console.log("submitSignup(): " + self.signupEmail);
                $http.post('api/signup',
                           { "email": self.signupEmail })
                .then(
                    function(response) {
                        if (response.data.responseCode === "USER_CREATED") {
                            console.log("got success, redirecting");
                            $location.path("/confirmSignup");
                        }
                        else {
                            alert("got: " + response.data.responseCode);
                        }
                    },
                    function(reason) {
                        alert("signin failed: " + reason);
                    });
            }
        }
    ]
});
