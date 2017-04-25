(function() {
    'use strict';

    madAPP.config(function($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise('/home');

        $stateProvider
        // main view
            .state('home', {
            url: '/home',
            templateUrl: '/template/main.html',
            controller: 'mainController',
            controllerAs: 'mc'
        })

        // configuration view
        .state('logic_builder', {
            url: '/logic_builder',
            templateUrl: '/template/logicBuilder.html',
            controller: 'logicBuilderController',
            controllerAs: 'lb'
        })

        .state('app_installer', {
            url: '/app_installer',
            templateUrl: '/template/appInstaller.html',
            controller: 'appInstallerController',
            controllerAs: 'ai'
        })

        // project template view
        // .state('app', {
        //     url: '/app/:id',
        //     templateUrl: '/template/app_view.html',
        //     controller: 'appViewController',
        //     controllerAs: 'av'
        .state('demo_queue', {
            url: '/demo_queue',
            templateUrl: '/template/demo_queue.html',
            controller: 'demoQueueController',
            controllerAs: 'dq'
        })

        .state('demo_chair', {
            url: '/demo_chair',
            templateUrl: '/template/demo_chair.html',
            controller: 'demoChairController',
            controllerAs: 'dc'
        })

        .state('demo_light', {
            url: '/demo_light',
            templateUrl: '/template/demo_light.html',
            controller: 'demoLightController',
            controllerAs: 'dl'
        });

    });
})();
