angular.module('lPhoto').
component("confirmSignup", {
    templateUrl: "templates/confirmSignup.html",
    controller: ['$http',
        function ConfirmSignupController() {
            console.log("ConfirmSignupController called");

            var self = this;
            self.email = "";
            self.temporaryPassword = "";
            self.finalPassword = "";
            self.confirmationPassword = "";

            self.confirmSignup = function() {
                console.log("confirmSignup() called: " +
                            self.email + " / " + self.temporaryPassword + " / " +
                            self.finalPassword + " / " + self.confirmationPassword);
            }
        }
    ]
});
