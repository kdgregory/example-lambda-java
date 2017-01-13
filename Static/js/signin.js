angular.module('lPhoto').
component("signin", {
    templateUrl: "templates/signin.html",
    controller: ['$http',
        function SigninController() {
            console.log("SigninController called");

            var self = this;
            self.signinEmail = "";
            self.signinPassword = "";
            self.signupEmail = "";

            self.submitSignin = function() {
                console.log("submitSignin() called: " + self.signinEmail + " / " + self.signinPassword);
            }

            self.submitSignup = function() {
                console.log("submitSignup() called: " + self.signupEmail);
            }
        }
    ]
});
