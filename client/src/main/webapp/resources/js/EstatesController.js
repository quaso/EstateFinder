angular.module('Estates.Controllers',[]).controller("EstatesController", function($scope, $http) {
	
	//var urlBase = 'http://localhost:8080/rest/';
	var urlBase = 'http://quasoestatefinderserver.appspot.com/rest/';
	
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

	$scope.deleteEstate = function(estate){
		$http.get(urlBase + 'delete/'+estate.id).success(function(data) {
			estate.visible=false;
		}).error(function() {
			alert('Error deleting data');
		});
	};
	
	fetchEstates();
});
