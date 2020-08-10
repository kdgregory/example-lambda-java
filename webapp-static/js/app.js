angular.module('lPhoto', [
  'ngRoute'
]).
config(['$locationProvider', '$routeProvider', '$sceDelegateProvider',
  function config($locationProvider, $routeProvider, $sceDelegateProvider) {
    $locationProvider.hashPrefix('!');

    $routeProvider.
      when("/main", {
        template: "<main></main>"
      }).
      when("/upload", {
        template: "<upload></upload>"
      }).
      otherwise("/main");

      $sceDelegateProvider.resourceUrlWhitelist([
        "self",
        "https://" + window.STATIC_HOST + "/**"
      ]);
  }
]);
