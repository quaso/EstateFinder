var urlBase = 'http://localhost:8080/rest/';

App.controller("EstatesController", function($scope, $http) {
	function fetchEstates(){
		$http.get(urlBase + 'estates').success(function(data) {
			$scope.estates = data.estates;
			$scope.lastUpdate = data.lastUpdate;
		});
	}
	$scope.fetchEstates = fetchEstates();

	$scope.collectNow = function() {
		$http.get(urlBase + 'collect').success(function(data) {
			fetchEstates()
		});
	}

	fetchEstates();
});
