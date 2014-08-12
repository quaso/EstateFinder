angular.module('Estates.Controllers', []).controller(
		"EstatesController",
		function($scope, $http) {

//			var urlBase = 'http://localhost:8080/rest/';
			 var urlBase = 'http://quasoestatefinderserver.appspot.com/rest/';
			
			var collecting = false;

			function fetchEstates() {
				$http.get(urlBase + 'estates').success(function(data) {
					handleResults(data);
				}).error(function() {
					alert('Error retrieving data');
				});
			}

			function handleResults(data) {
				$scope.estates = data.estates;
				$scope.streets = data.streets;
				$scope.lastUpdate = data.lastUpdate;
				$scope.lastView = data.lastView;
				$scope.newEstatesCount = data.newEstatesCount;
			}

			$scope.collectNow = function() {
				$http.get(urlBase + 'collect').success(function(data) {
					handleResults(data);
				}).error(function() {
					alert('Error collecting new data');
				});
			};

			$scope.hideEstate = function(estate) {
				$http.get(urlBase + 'hide/' + estate.id).success(
						function(data) {
							estate.visible = false;
						}).error(function() {
					alert('Error hiding data');
				});
			};

			$scope.searchForStreet = function() {
				if (typeof $scope.searchStreet === 'undefined'
						|| $scope.searchStreet.length === 0) {
					fetchEstates();
				} else {
					$http.get(urlBase + 'search/' + $scope.searchStreet)
							.success(function(data) {
								handleResults(data)
							}).error(function() {
								alert('Error searching for data');
							});
				}
			};

			$scope.save = function() {
				$http.get(urlBase + 'save').success(function(data) {
					alert('Data saved');
				}).error(function(data, status) {
					alert('Error saving data (' + status + '): ' + data);
				});
			};

			$scope.deleteAll = function() {
				$http.get(urlBase + 'deleteAll').success(function(data) {
					alert('All data deleted');
				}).error(function(data, status) {
					alert('Error deleting data: ' + data);
				});
			};

			fetchEstates();
		});
