// (function() {
//     'use strict';

//     madAPP.controller('appViewController', appViewController);

//     function appViewController($stateParams, $timeout) {
//         var vm = this;
//         vm.id = $stateParams.id;
//         vm.addDeviceTemplate = "template/_add_device.html";
//         var qrUrl = 'https://chart.googleapis.com/chart?cht=qr&chs=300x300&chl=';

//         var apps = firebase.database().ref('applications');
//         var app = apps.child(vm.id);
//         var devices = firebase.database().ref('devices');

//         vm.sensors = {};

//         vm.curApp = null;
//         app.once('value').then(function(snapshot) {
//             $timeout(function() {
//                 vm.curApp = snapshot.val();
//                 console.log(vm.curApp);
//             }, 0);
//         });

//         var default_config = {
//             'light': {
//                 'on': false,
//                 'interval': undefined,
//                 'threshold': undefined
//             },
//             'sound': {
//                 'on': false,
//                 'interval': undefined,
//                 'threshold': undefined
//             },
//             'accel': {
//                 'on': false,
//                 'interval': undefined,
//                 'threshold': undefined
//             },
//             'gyro': {
//                 'on': false,
//                 'interval': undefined,
//                 'threshold': undefined
//             }
//         };

//         var newDevice = null;
//         vm.addDevice = function() {
//             newDevice = devices.push(vm.sensors, function(err) {
//                 if (err) {
//                     console.log(err);
//                 }
//             });
//             vm.qrcode = qrUrl + newDevice.key;

//             app.child('devices/' + newDevice.key).set(true);

//             vm.cur_app.devices[newDevice.key] = true;
//         };

//         vm.resetModal = function() {
//             vm.qrcode = null;
//             vm.sensors = default_config;
//             $('#add_device').modal('hide');
//         };

//         vm.showAddBtn = function() {
//             if (!vm.curApp.devices || _.size(vm.curApp.devices) < vm.curApp.devNum) {
//                 return true;
//             }
//             return false;
//         };
//     }
// })();
