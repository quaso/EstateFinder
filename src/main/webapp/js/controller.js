(function () {
    var as = angular.module('EstateFinder.controllers', []);

    as.controller('EstatesController', function ($scope) {
    	
    	
    	
        $scope.language = function () {
            return i18n.language;
        };
        $scope.setLanguage = function (lang) {
            i18n.setLanguage(lang);
        };
        $scope.activeWhen = function (value) {
            return value ? 'active' : '';
        };

        $scope.path = function () {
            return $location.url();
        };

        $scope.login = function () {
            $scope.$emit('event:loginRequest', $scope.username, $scope.password);
            $('#login').modal('hide');
        };
        $scope.logout = function () {
            $rootScope.user = null;
            $scope.username = $scope.password = null;
            $scope.$emit('event:logoutRequest');
            $location.url('/person');
        };

    });
    
    controller('DriversController', function($scope) {
        $scope.driversList = [
          {
              Driver: {
                  givenName: 'Sebastian',
                  familyName: 'Vettel'
              },
              points: 322,
              nationality: "German",
              Constructors: [
                  {name: "Red Bull"}
              ]
          },
          {
              Driver: {
              givenName: 'Fernando',
                  familyName: 'Alonso'
              },
              points: 207,
              nationality: "Spanish",
              Constructors: [
                  {name: "Ferrari"}
              ]
          }
        ];
    });
}());