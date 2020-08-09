angular.module('lPhoto', [
  'ngRoute'
]).
config(['$locationProvider', '$routeProvider', '$sceDelegateProvider',
  function config($locationProvider, $routeProvider, $sceDelegateProvider) {
    $locationProvider.hashPrefix('!');

    $routeProvider.
      when("/signin", {
        template: "<signin></signin>"
      }).
      when("/confirmSignup", {
        template: "<confirm-signup></confirm-signup>"
      }).
      when("/main", {
        template: "<main></main>"
      }).
      when("/upload", {
        template: "<upload></upload>"
      }).
      otherwise("/signin");

      $sceDelegateProvider.resourceUrlWhitelist([
        "self",
        "https://" + window.STATIC_HOST + "/**"
      ]);
  }
]);
