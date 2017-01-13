angular.module('lPhoto', [
  'ngRoute'
]).
config(['$locationProvider', '$routeProvider',
  function config($locationProvider, $routeProvider) {
    $locationProvider.hashPrefix('!');

    $routeProvider.
      when("/signin", {
        template: "<signin></signin>"
      }).
      when("/confirmSignup", {
        template: "<confirm-signup></confirm-signup>"
      }).
      otherwise("/signin");
  }
]);