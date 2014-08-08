var urlBase = 'http://localhost:8080/rest/';

app.controller("EstatesController", function($scope, $http) {
	function fetchEstates() {
		$http.get(urlBase + 'estates').success(function(data) {
			$scope.estates = data.estates;
			$scope.lastUpdate = data.lastUpdate;
		}).error(function() {
			alert('Error retrieving data');
		});
	}

	$scope.collectNow = function() {
		$http.get(urlBase + 'collect').success(function(data) {
			fetchEstates()
		}).error(function() {
			alert('Error collecting new data');
		});
	}

	fetchEstates();
});
