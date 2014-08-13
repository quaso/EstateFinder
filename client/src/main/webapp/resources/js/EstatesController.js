angular
		.module('Estates.Controllers', [])
		.controller(
				"EstatesController",
				function($scope, $http) {

//					 var urlBase = 'http://localhost:8080/rest/';
					var urlBase = 'http://quasoestatefinderserver.appspot.com/rest/';

					var collecting = false;

					function fetchEstates() {
						$http.get(urlBase + 'estates').success(function(data) {
							handleResults(data);
							$scope.filteredEstates = $scope.allEstates;
						}).error(function() {
							alert('Error retrieving data');
						});
					}

					function handleResults(data) {
						$scope.allEstates = data.estates;
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

					$scope.hideAllUnknownEstates = function() {
						var idList = [];
						var estateId;
						for (estateId in $scope.allEstates) {
							if (!$scope.allEstates[estateId].street) {
								idList.push($scope.allEstates[estateId].id);
								$scope.allEstates[estateId].visible = false;
							}
						}
						$http.get(urlBase + 'hide/' + idList).error(function() {
							alert('Error hiding data');
						});
					};

					$scope.searchForStreet = function() {
						if (typeof $scope.searchStreet === 'undefined'
								|| $scope.searchStreet.length === 0) {
							$scope.filteredEstates = $scope.allEstates;
						} else {
							var result = [];
							var streetId;
							for (streetId in $scope.searchStreet) {
								var estateId;
								for (estateId in $scope.allEstates) {
									if ($scope.allEstates[estateId].street == $scope.searchStreet[streetId]) {
										result
												.push($scope.allEstates[estateId]);
									}
								}
							}
							$scope.filteredEstates = result;
						}
					};

					$scope.save = function() {
						$http.get(urlBase + 'save').success(function(data) {
							alert('Data saved');
						}).error(
								function(data, status) {
									alert('Error saving data (' + status
											+ '): ' + data);
								});
					};

					$scope.deleteAll = function() {
						$http.get(urlBase + 'deleteAll').success(
								function(data) {
									alert('All data deleted');
								}).error(function(data, status) {
							alert('Error deleting data: ' + data);
						});
					};

					$scope.prepareRecollect = function() {
						$http.get(urlBase + 'prepareRecollect').success(
								function(data) {
									alert('Next collect will be a full one');
								}).error(function(data, status) {
							alert('Error deleting data: ' + data);
						});
					};

					fetchEstates();
				});
