var restLink = "http://localhost:8080/ForumApp/rest/";
//var restLink = "http://54.69.96.56:8080/ForumApp/rest/";

//var username = null; //robertk
//var password = null; //bbb
var encoded = ""; //btoa(username + ":" + password);

var app = angular.module('forumApp', [ 'ngRoute', 'ui.bootstrap' ]);


//navigation
app.config(function($routeProvider) {
	$routeProvider
	
	//home
	.when('/', {
		templateUrl : 'pages/home.template.html',
		controller: 'homeController'
	})
	
	
	//subcategory
	.when('/subcategory/:subcategoryId', {
		templateUrl : 'pages/subcategory.template.html',
		controller: 'subcategoryController'
	})
	.when('/subcategory/:subcategoryId/:pageNumber', {
		templateUrl : 'pages/subcategory.template.html',
		controller: 'subcategoryController'
	})
	
	
	//topic
	.when('/topic/:topicId', {
		templateUrl : 'pages/topic.template.html',
		controller: 'topicController'
	})
	.when('/topic/:topicId/:postId', {
		templateUrl : 'pages/topic.template.html',
		controller: 'topicController'
	})
	
	
	//user
	.when('/user/:userId', {
		templateUrl : 'pages/user.overview.template.html',
		controller: 'userOverviewController'
	})
	.when('/user/:userId/topics', {
		templateUrl : 'pages/user.topics.template.html',
		controller: 'userTopicsController'
	})
	.when('/user/:userId/posts', {
		templateUrl : 'pages/user.posts.template.html',
		controller: 'userPostsController'
	})
	.when('/user/:userId/likes', {
		templateUrl : 'pages/user.likes.template.html',
		controller: 'userLikesController'
	})
	
	
	//members
	.when('/members', {
		templateUrl : 'pages/members.template.html',
		controller: 'membersController'
	})
	
	
	//conversations
	.when('/conversations', {
		templateUrl : 'pages/conversations.template.html',
		controller: 'conversationsController'
	})
	
	
	//messages
	.when('/messages/:conversationId', {
		templateUrl : 'pages/messages.template.html',
		controller: 'messagesController'
	})
	
	
	//notifications
	.when('/notifications', {
		templateUrl : 'pages/notifications.template.html',
		controller: 'notificationsController'
	})
	.when('/notifications/:notificationId', {
		templateUrl : 'pages/notifications.template.html',
		controller: 'notificationsController'
	})
	
	
	
	/* .otherwise({
		redirectTo: '/'
	}) */
	;
	
	
});


//route location change event
app.run(function($rootScope, $location, $route) {
	$rootScope.$on( "$routeChangeStart", function(event, next, current) {
		$rootScope.$emit('updateNavbar');
	});
	$rootScope.$on('reload', function(event, data) {
		$route.reload();
	});	
});


//navbar
app.controller('navbarController', function($rootScope, $scope, $http) {
	$rootScope.$on('updateNavbar', function(event, data) {
		$http.get(restLink + "navbar", {
			headers : {
				"Authorization" : "Basic " + encoded
			}
		})
		.then(function(res) {
			$scope.data = res.data;
		});
	});
});


//home
app.controller('homeController', function($rootScope, $scope, $http) {
	var link = restLink + "home";
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res){
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
	});
});


//subcategory
app.controller('subcategoryController', function($scope, $http, $routeParams) {
	var categoryId = $routeParams.subcategoryId;
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'subcategory/' + categoryId + "/" + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
	});
});


//topic
app.controller('topicController', function($scope, $http, $routeParams, $sce) {
	$topicId = $routeParams.topicId;
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'topic/' + $topicId + "/" + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
		for (var i = 0; i < $scope.data.posts.length; i++) {
			var post = $scope.data.posts[i];
			post.text = convertBBCode(post.text, $sce);
		}
	});
});


//members
app.controller('membersController', function($scope, $http, $routeParams) {
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'members/' + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
	});
});


//user
app.controller('userOverviewController', function($scope, $http, $routeParams) {
	var userId = $routeParams.userId;
	var link = restLink + 'user/' + userId;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
	});
});
app.controller('userTopicsController', function($scope, $http, $sce, $routeParams) {
	var userId = $routeParams.userId;
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'user/' + userId + '/topics/' + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
		for (var i = 0; i < $scope.data.topics.length; i++) {
			var topic = $scope.data.topics[i];
			topic.text = convertBBCode(topic.text, $sce);
		}
	});
});
app.controller('userPostsController', function($scope, $http, $sce, $routeParams) {
	var userId = $routeParams.userId;
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'user/' + userId + '/posts/' + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
		for (var i = 0; i < $scope.data.posts.length; i++) {
			var post = $scope.data.posts[i];
			post.text = convertBBCode(post.text, $sce);
		}
	});
});
app.controller('userLikesController', function($scope, $http, $sce, $routeParams) {
	var userId = $routeParams.userId;
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'user/' + userId + '/likes/' + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
		for (var i = 0; i < $scope.data.posts.length; i++) {
			var post = $scope.data.posts[i];
			post.text = convertBBCode(post.text, $sce);
		}
	});
});


//conversations
app.controller('conversationsController', function($scope, $http, $sce, $routeParams) {
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'conversations/' + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
	});
});


//messages
app.controller('messagesController', function($scope, $http, $sce, $routeParams) {
	var conversationNumber = $routeParams.conversationNumber;
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'messages/' + conversationNumber + '/' + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
		for (var i = 0; i < $scope.data.messages.length; i++) {
			var message = $scope.data.messages[i];
			message.text = convertBBCode(message.text, $sce);
		}
	});
});


//notifications
app.controller('notificationsController', function($scope, $http, $sce, $routeParams) {
	var pageNumber = 1;
	if ($routeParams.pageNumber) {
		pageNumber = $routeParams.pageNumber;
	}
	var link = restLink + 'notifications/' + pageNumber;
	debug("Getting page: " + link);
	$http.get(link, {
		headers : {
			"Authorization" : "Basic " + encoded
		}
	})
	.then(function(res) {
		debug("Result has arrived for " +  link);
		$scope.data = res.data;
	});
});


function convertBBCode(text, $sce) {
	var result = XBBCODE.process({
		text : text,
		removeMisalignedTags : false,
		addInLineBreaks : false
	});
	return $sce.trustAsHtml(result.html);
}

function debug(text) {
	console.log(text);
}