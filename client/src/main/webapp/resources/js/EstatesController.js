angular.module('Estates.Controllers', []).controller(
		"EstatesController",
		function($scope, $http) {

			// var urlBase = 'http://localhost:8080/rest/';
			var urlBase = 'http://quasoestatefinderserver.appspot.com/rest/';

			function fetchEstates() {
				$http.get(urlBase + 'estates').success(function(data) {
					$scope.estates = data.estates;
					$scope.streets = data.streets;
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

			$scope.deleteEstate = function(estate) {
				$http.get(urlBase + 'delete/' + estate.id).success(
						function(data) {
							estate.visible = false;
						}).error(function() {
					alert('Error deleting data');
				});
			};

			$scope.searchForStreet = function() {
				$http.get(urlBase + 'search/' + $scope.searchStreet).success(
						function(data) {
							$scope.estates = data.estates;
							$scope.lastUpdate = data.lastUpdate;
						}).error(function() {
					alert('Error searching for data');
				});
			};

			$scope.save = function() {
				$http.get(urlBase + 'save').success(function(data) {
					alert('Data saved');
				}).error(function(data, status) {
					alert('Error saving data (' + status + '): ' + data);
				});
			};

			fetchEstates();
		});
