var App = angular.module('EstatesFinderApp', []);

App.controller("EstatesController", ['$scope', function($scope){
    $scope.test = "I now understand how the scope works!";
}]);

/*
App.controller("EstatesController", function($scope) {
	$scope.lastUpdate = new Date();
	$scope.test = "aaa";
});
*/