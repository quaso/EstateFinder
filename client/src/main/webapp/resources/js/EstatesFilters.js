angular.module('Estates.Filters',[]).filter("nullDate", ['$filter',function($filter) {
	 return function(input, format) {
		 if (!input){
			 return 'N/A';
		 }
		
		 return  $filter('date')(input, format);
	};
}]);
