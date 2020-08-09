angular.module('lPhoto').
component("confirmSignup", {
    templateUrl: "https://" + window.STATIC_HOST + "/templates/confirmSignup.html",
    controller: ['$http', '$location',
        function ConfirmSignupController($http, $location, $sceDelegateProvider) {
            console.log("ConfirmSignupController called");

            var self = this;
            self.email = "";
            self.temporaryPassword = "";
            self.finalPassword = "";
            self.confirmationPassword = "";
            self.passwordsDontMatch = false;


            self.checkPasswords = function() {
                self.passwordsDontMatch = self.finalPassword && self.confirmationPassword
                                          && (self.finalPassword != self.confirmationPassword);
            }


            self.canSubmit = function() {
                return self.email && self.temporaryPassword && self.finalPassword && self.confirmationPassword
                       && (self.finalPassword == self.confirmationPassword);
            }


            self.submit = function() {
                console.log("confirmSignup(): " +
                            self.email + " / " + self.temporaryPassword + " / " +
                            self.finalPassword + " / " + self.confirmationPassword);

                if (self.finalPassword != self.confirmationPassword) {
                    return;
                }

                $http.post('api/confirmSignup',
                           { "email": self.email,
                             "temporaryPassword": self.temporaryPassword,
                             "password": self.finalPassword})
                .then(
                    function(response) {
                        if (response.data.responseCode === "SUCCESS") {
                            console.log("got success, redirecting");
                            $location.path("/main");
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
